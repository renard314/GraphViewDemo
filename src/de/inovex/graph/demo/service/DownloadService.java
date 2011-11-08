package de.inovex.graph.demo.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;

public class DownloadService extends IntentService {
	
	public static final String STATUS_EXTRA = "status";
	public static final String VALUE_EXTRA = "value";
	public static final int STATUS_IN_PROGRESS = 0;
	public static final int STATUS_FINISHED = 1;
	public static final int STATUS_PROGRESS_START = 2;
	public static final String UPDATE_ACTION = "PRODUCTION_DATA_UPDATE";
	
	private class LiveDataContentHandler implements ContentHandler {
		
		ContentValues mLocation = null;
		ContentValues mProduction = null;
		List<ContentValues> mProductionList = new ArrayList<ContentValues>();
		List<ContentValues> mLocationList = new ArrayList<ContentValues>();
		String mCurrentTagName = null;
		String mCurrentLocationId = null;
		private long mCurrentMillis;
		

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			String stringValue = new String(ch, start, length).replace("\n", "");
			if (TextUtils.isEmpty(stringValue)  || stringValue.length()<3){
				return;
			}
			if (mCurrentTagName.equals("live")) {
				int production = parseProductionValue(stringValue);
				
				if (production > -1) {
					mProduction.put(RWELiveDataContentProvider.Columns.ProductionData.VALUE, production);
					mProduction.put(RWELiveDataContentProvider.Columns.ProductionData.CREATED, mCurrentMillis);
					mProduction.put(RWELiveDataContentProvider.Columns.ProductionData.LOCATION_ID, mCurrentLocationId);
				}
			}
			if (mCurrentTagName.equals("name")) {
				mLocation.put(RWELiveDataContentProvider.Columns.Locations.NAME, stringValue);
			}
			if (mCurrentTagName.equals("golive")) {
				mLocation.put(RWELiveDataContentProvider.Columns.Locations.GOLIVE,stringValue);
			}
			if (mCurrentTagName.equals("turbines")) {
				mLocation.put(RWELiveDataContentProvider.Columns.Locations.TURBINES, stringValue);
			}
			if (mCurrentTagName.equals("power")) {
				mLocation.put(RWELiveDataContentProvider.Columns.Locations.POWER,stringValue);
			}
			if (mCurrentTagName.equals("flashxpos")) {
				mLocation.put(RWELiveDataContentProvider.Columns.Locations.XPOS,stringValue);				
			}
			if (mCurrentTagName.equals("flashypos")) {
				mLocation.put(RWELiveDataContentProvider.Columns.Locations.YPOS,stringValue);				
			}
		}

		@Override
		public void endDocument() throws SAXException {
			if (mProductionList.size() > 0) {
				sendUpdateBroadCast(STATUS_FINISHED,mProduction.size());
				int numInserted = getContentResolver().bulkInsert(RWELiveDataContentProvider.CONTENT_URI_PRODUCTION, mProductionList.toArray(new ContentValues[mProductionList.size()]));
				getContentResolver().notifyChange(RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_MINUTE, null);
				Log.i("DownloadService", "Inserted " + numInserted + " production values!");
			}
			if (mLocationList.size()>0) {
				int numInserted = getContentResolver().bulkInsert(RWELiveDataContentProvider.CONTENT_URI_PLACES, mLocationList.toArray(new ContentValues[mLocationList.size()]));
				getContentResolver().notifyChange(RWELiveDataContentProvider.CONTENT_URI_PLACES, null);
				Log.i("DownloadService", "Inserted " + numInserted + " places!");				
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (localName.equals("location")){
				if (mProduction.size() > 0) {
					String currentCountry = mLocation.getAsString(RWELiveDataContentProvider.Columns.Locations.COUNTRY).toLowerCase();
					if (currentCountry.equals("deutschland")){
						mProductionList.add(mProduction);
						sendUpdateBroadCast(STATUS_IN_PROGRESS,mProductionList.size());
						getContentResolver().insert(RWELiveDataContentProvider.CONTENT_URI_PLACES,mLocation);
						//Log.i("DownloadService",mLocation.getAsString(RWELiveDataContentProvider.Columns.Locations.XPOS) + " | " + mLocation.getAsString(RWELiveDataContentProvider.Columns.Locations.YPOS));						
					}
					//mLocationList.add(mLocation);
				}
			}
		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {}

		@Override
		public void setDocumentLocator(Locator locator) {}

		@Override
		public void skippedEntity(String name) throws SAXException {}

		@Override
		public void startDocument() throws SAXException {
			mCurrentMillis = System.currentTimeMillis();
			sendUpdateBroadCast(STATUS_PROGRESS_START, 0);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
			mCurrentTagName = localName.toLowerCase();
			if (localName.equals("location")){				
				mLocation = new ContentValues();
				mProduction = new ContentValues();
				mCurrentLocationId = atts.getValue(0);
				mLocation.put(RWELiveDataContentProvider.Columns.Locations.LOCATION_ID, mCurrentLocationId);
			}
			if (localName.equals("type")) {
				mLocation.put(RWELiveDataContentProvider.Columns.Locations.TYPE, atts.getValue(0));
			}
			if (localName.equals("city")) {
				mLocation.put(RWELiveDataContentProvider.Columns.Locations.CITY, atts.getValue(0));
			}
			if (localName.equals("land")) {
				mLocation.put(RWELiveDataContentProvider.Columns.Locations.COUNTRY, atts.getValue(0));
			}
		}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {}

	}

	private final LiveDataContentHandler mContentHandler = new LiveDataContentHandler();
	private XMLReader mXmlReader = null;

	public DownloadService() {
		super("DownloadService");
		/* Get a SAXParser from the SAXPArserFactory. */
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = null;
		try {
			sp = spf.newSAXParser();
			/* Get the XMLReader of the SAXParser we created. */
			mXmlReader = sp.getXMLReader();
			/* Create a new ContentHandler and apply it to the XML-Reader */
			mXmlReader.setContentHandler(mContentHandler);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

	}


	int parseProductionValue(String value) {	
	if (value.equals("noch nicht verfügbar")) {
			return -1;
		}
		String number = value.substring(0, value.length() - 2);
		String unit = value.substring(value.length() - 2, value.length()).toLowerCase();
		float fval = -1;
		try {
			number = number.replace(',', '.').trim();
			if (number.equals("--") || TextUtils.isEmpty(number)) {
				fval = 0f;
			} else {
				fval = Float.valueOf(number);
			}
			if (unit.contains("mw")) {
				fval *= 1000;
			}
			return Math.round(fval);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private void sendUpdateBroadCast(int status,int value){
		Intent intent = new Intent(UPDATE_ACTION);
		intent.putExtra(STATUS_EXTRA, status);
		intent.putExtra(VALUE_EXTRA, value);
		sendBroadcast(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (mXmlReader != null) {
			String urlString = intent.getDataString();
			try {
				URL url = new URL(urlString);				
				mXmlReader.parse(new InputSource(url.openStream()));	
//				final int BUFFER_SIZE = 1024*4; // 4k buffer
//				byte[] temp = new byte[BUFFER_SIZE];
//				int bytesRead;
//				Log.i("DOWNLOADING", "START");
//				InputStream buf = url.openStream();
//				ByteArrayOutputStream bos = new ByteArrayOutputStream();
//				while ((bytesRead = buf.read(temp, 0, BUFFER_SIZE)) != -1) {
//				    bos.write(temp, 0, bytesRead);
//				}
//				buf.close();
//				Log.i("DOWNLOADING", "FINISHED");
//				ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
//				mXmlReader.parse(new InputSource(bis));
				
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
				sendUpdateBroadCast(STATUS_FINISHED, -1);
			} catch (IOException e) {
				e.printStackTrace();
				sendUpdateBroadCast(STATUS_FINISHED, -1);
			} catch (SAXException e) {
				e.printStackTrace();
				sendUpdateBroadCast(STATUS_FINISHED, -1);
			}
		}
	}
}