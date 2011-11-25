package de.inovex.graph.demo.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;

public class DownloadService extends IntentService {
	@SuppressWarnings(value = { "unused" })
	private static final String DEBUG_TAG = DownloadService.class.getName();
	
	private static String testString = "[{\"xpos\":\"275\",\"name\":\"Ruhr/Sieg/Diemel/Lippe\",\"power\":\"25,507 MW\",\"ypos\":\"191\",\"production_series\":[{\"created\":1322214331121,\"value\":1713}],\"power_as_kw\":25507,\"type\":\"17 Wasserkraftwerke\",\"location_id\":\"D10\",\"city\":\"Betriebsstandort Herdecke\",\"country\":\"Deutschland\",\"go_live\":\"1923\"},{\"xpos\":\"261\",\"name\":\"Mosel/Eifel/Saar/Nahe/Rur\",\"power\":\"257,264 MW\",\"ypos\":\"208\",\"production_series\":[{\"created\":1322214331121,\"value\":40007}],\"power_as_kw\":257264,\"type\":\"27 Wasserkraftwerke\",\"location_id\":\"D14\",\"city\":\"Betriebsstandort Bernkastel\",\"country\":\"Deutschland\",\"go_live\":\"1905\"},{\"xpos\":\"100\",\"name\":\"Mosel/Eifel/Saar\",\"ypos\":\"100\",\"power\":\"252,97 MW\",\"production_series\":[{\"created\":1322214331121,\"value\":1713}],\"power_as_kw\":252970,\"type\":\"16 Wasserkraftwerke\",\"location_id\":\"D1400\",\"city\":\"Betriebsstandort Bernkastel\",\"country\":\"Deutschland\"},{\"xpos\":\"280\",\"name\":\"RADAG\",\"power\":\"80 MW\",\"ypos\":\"268\",\"production_series\":[{\"created\":1322214331121,\"value\":36060}],\"power_as_kw\":80000,\"type\":\"Wasserkraftwerk\",\"location_id\":\"D21\",\"city\":\"Albbruck-Dogern\",\"country\":\"Deutschland\",\"go_live\":\"1934\"},{\"xpos\":\"351\",\"name\":\"BMHKW Neukölln\",\"power\":\"20 MWel/65 MWth\",\"ypos\":\"168\",\"production_series\":[{\"created\":1322214331121,\"value\":20000}],\"power_as_kw\":85000,\"type\":\"Biomassekraftwerk\",\"location_id\":\"D3\",\"city\":\"Berlin\",\"country\":\"Deutschland\",\"go_live\":\"2006\"},{\"xpos\":\"300\",\"name\":\"Bartelsdorf\",\"power\":\"32 MW\",\"ypos\":\"144\",\"production_series\":[{\"created\":1322214331121,\"value\":3237}],\"power_as_kw\":32000,\"type\":\"Onshore Windpark\",\"location_id\":\"D31\",\"city\":\"Bartelsdorf\",\"country\":\"Deutschland\",\"go_live\":\"2009\"},{\"xpos\":\"294\",\"name\":\"Calle\",\"power\":\"9 MW\",\"ypos\":\"154\",\"production_series\":[{\"created\":1322214331121,\"value\":72}],\"power_as_kw\":9000,\"type\":\"Onshore Windpark\",\"location_id\":\"D33\",\"city\":\"Calle\",\"country\":\"Deutschland\",\"go_live\":\"2004\"},{\"xpos\":\"308\",\"name\":\"Dransfeld\",\"power\":\"1,2 MW\",\"ypos\":\"184\",\"production_series\":[{\"created\":1322214331121,\"value\":0}],\"power_as_kw\":1200,\"type\":\"Onshore Windpark\",\"location_id\":\"D35\",\"city\":\"Dransfeld\",\"country\":\"Deutschland\",\"go_live\":\"2002\"},{\"xpos\":\"314\",\"name\":\"Eicklingen\",\"power\":\"3,6 MW\",\"ypos\":\"163\",\"production_series\":[{\"created\":1322214331121,\"value\":229}],\"power_as_kw\":3600,\"type\":\"Onshore Windpark\",\"location_id\":\"D36\",\"city\":\"Eicklingen\",\"country\":\"Deutschland\",\"go_live\":\"2003\"},{\"xpos\":\"276\",\"name\":\"Elisabethfehn\",\"power\":\"8 MW\",\"ypos\":\"151\",\"production_series\":[{\"created\":1322214331121,\"value\":613}],\"power_as_kw\":8000,\"type\":\"Onshore Windpark\",\"location_id\":\"D37\",\"city\":\"Elisabethfehn\",\"country\":\"Deutschland\",\"go_live\":\"2007\"},{\"xpos\":\"299\",\"name\":\"Eystrup\",\"power\":\"9 MW\",\"ypos\":\"154\",\"production_series\":[{\"created\":1322214331121,\"value\":54}],\"power_as_kw\":9000,\"type\":\"Onshore Windpark\",\"location_id\":\"D38\",\"city\":\"Eystrup\",\"country\":\"Deutschland\",\"go_live\":\"2002 - 2003\"},{\"xpos\":\"292\",\"name\":\"Friedrichsgabekoog\",\"power\":\"5 MW\",\"ypos\":\"127\",\"production_series\":[{\"created\":1322214331121,\"value\":432}],\"power_as_kw\":5000,\"type\":\"Onshore Windpark\",\"location_id\":\"D39\",\"city\":\"Friedrichsgabekoog\",\"country\":\"Deutschland\",\"go_live\":\"1995 - 2004\"},{\"xpos\":\"315\",\"name\":\"Gethsemane\",\"power\":\"7,2 MW\",\"ypos\":\"202\",\"production_series\":[{\"created\":1322214331121,\"value\":67}],\"power_as_kw\":7200,\"type\":\"Onshore Windpark\",\"location_id\":\"D40\",\"city\":\"Gethsemane\",\"country\":\"Deutschland\",\"go_live\":\"2001\"},{\"xpos\":\"309\",\"name\":\"Grauen\",\"power\":\"4 MW\",\"ypos\":\"138\",\"production_series\":[{\"created\":1322214331121,\"value\":487}],\"power_as_kw\":4000,\"type\":\"Onshore Windpark\",\"location_id\":\"D41\",\"city\":\"Grauen\",\"country\":\"Deutschland\",\"go_live\":\"2006\"},{\"xpos\":\"335\",\"name\":\"Grebbin\",\"power\":\"8 MW\",\"ypos\":\"140\",\"production_series\":[{\"created\":1322214331121,\"value\":8}],\"power_as_kw\":8000,\"type\":\"Onshore Windpark\",\"location_id\":\"D42\",\"city\":\"Grebbin\",\"country\":\"Deutschland\",\"go_live\":\"2008\"},{\"xpos\":\"293\",\"name\":\"Hörup\",\"power\":\"2 MW\",\"ypos\":\"110\",\"production_series\":[{\"created\":1322214331121,\"value\":0}],\"power_as_kw\":2000,\"type\":\"Onshore Windpark\",\"location_id\":\"D46\",\"city\":\"Hörup\",\"country\":\"Deutschland\",\"go_live\":\"1998\"},{\"xpos\":\"339\",\"name\":\"Krusemark\",\"power\":\"22,1 MW\",\"ypos\":\"157\",\"production_series\":[{\"created\":1322214331121,\"value\":703}],\"power_as_kw\":22100,\"type\":\"Onshore Windpark\",\"location_id\":\"D48\",\"city\":\"Krusemark\",\"country\":\"Deutschland\",\"go_live\":\"1998 - 2007\"},{\"xpos\":\"315\",\"name\":\"Lasbek\",\"power\":\"10,8 MW\",\"ypos\":\"135\",\"production_series\":[{\"created\":1322214331121,\"value\":0}],\"power_as_kw\":10800,\"type\":\"Onshore Windpark\",\"location_id\":\"D49\",\"city\":\"Lasbek\",\"country\":\"Deutschland\",\"go_live\":\"2004\"},{\"xpos\":\"274\",\"name\":\"Lengerich\",\"power\":\"1,8 MW \",\"ypos\":\"163\",\"production_series\":[{\"created\":1322214331121,\"value\":3}],\"power_as_kw\":1800,\"type\":\"Onshore Windpark\",\"location_id\":\"D50\",\"city\":\"Lengerich\",\"country\":\"Deutschland\",\"go_live\":\"2003\"},{\"xpos\":\"316\",\"name\":\"Lesse\",\"power\":\"27,2 MW\",\"ypos\":\"177\",\"production_series\":[{\"created\":1322214331121,\"value\":918}],\"power_as_kw\":27200,\"type\":\"Onshore Windpark\",\"location_id\":\"D51\",\"city\":\"Lesse\",\"country\":\"Deutschland\",\"go_live\":\"2002 - 2007\"},{\"xpos\":\"359\",\"name\":\"Malterhausen\",\"power\":\"28,8 MW\",\"ypos\":\"175\",\"production_series\":[{\"created\":1322214331121,\"value\":464}],\"power_as_kw\":28800,\"type\":\"Onshore Windpark\",\"location_id\":\"D53\",\"city\":\"Malterhausen\",\"country\":\"Deutschland\",\"go_live\":\"2001 - 2005\"},{\"xpos\":\"274\",\"name\":\"Messingen\",\"power\":\"5,4 MW\",\"ypos\":\"169\",\"production_series\":[{\"created\":1322214331121,\"value\":11}],\"power_as_kw\":5400,\"type\":\"Onshore Windpark\",\"location_id\":\"D54\",\"city\":\"Messingen\",\"country\":\"Deutschland\",\"go_live\":\"2003 - 2004\"},{\"xpos\":\"312\",\"name\":\"Oedelum\",\"power\":\"3,6 MW\",\"ypos\":\"168\",\"production_series\":[{\"created\":1322214331121,\"value\":314}],\"power_as_kw\":3600,\"type\":\"Onshore Windpark\",\"location_id\":\"D55\",\"city\":\"Oedelum\",\"country\":\"Deutschland\",\"go_live\":\"2003\"},{\"xpos\":\"294\",\"name\":\"Ottersberg\",\"power\":\"1,2 MW\",\"ypos\":\"146\",\"production_series\":[{\"created\":1322214331121,\"value\":232}],\"power_as_kw\":1200,\"type\":\"Onshore Windpark\",\"location_id\":\"D56\",\"city\":\"Ottersberg\",\"country\":\"Deutschland\",\"go_live\":\"2003\"},{\"xpos\":\"299\",\"name\":\"Reeßum\",\"power\":\"4 MW\",\"ypos\":\"149\",\"production_series\":[{\"created\":1322214331121,\"value\":52}],\"power_as_kw\":4000,\"type\":\"Onshore Windpark\",\"location_id\":\"D58\",\"city\":\"Reeßum\",\"country\":\"Deutschland\",\"go_live\":\"2007\"},{\"xpos\":\"273\",\"name\":\"Bergkamen\",\"power\":\"20 MWel/61,3 MWth\",\"ypos\":\"180\",\"production_series\":[{\"created\":1322214331121,\"value\":18992}],\"power_as_kw\":81300,\"type\":\"Biomassekraftwerk\",\"location_id\":\"D6\",\"city\":\"Bergkamen\",\"country\":\"Deutschland\",\"go_live\":\"2005\"},{\"xpos\":\"320\",\"name\":\"Rethen\",\"power\":\"5,4 MW\",\"ypos\":\"165\",\"production_series\":[{\"created\":1322214331121,\"value\":0}],\"power_as_kw\":5400,\"type\":\"Onshore Windpark\",\"location_id\":\"D60\",\"city\":\"Rethen\",\"country\":\"Deutschland\",\"go_live\":\"2003\"},{\"xpos\":\"283\",\"name\":\"Sassenberg\",\"power\":\"1,8 MW \",\"ypos\":\"177\",\"production_series\":[{\"created\":1322214331121,\"value\":36}],\"power_as_kw\":1800,\"type\":\"Onshore Windpark\",\"location_id\":\"D63\",\"city\":\"Sassenberg\",\"country\":\"Deutschland\",\"go_live\":\"2003\"},{\"xpos\":\"318\",\"name\":\"Schmarloh\",\"power\":\"16 MW\",\"ypos\":\"158\",\"production_series\":[{\"created\":1322214331121,\"value\":145}],\"power_as_kw\":16000,\"type\":\"Onshore Windpark\",\"location_id\":\"D64\",\"city\":\"Schmarloh\",\"country\":\"Deutschland\",\"go_live\":\"2007 - 2008\"},{\"xpos\":\"294\",\"name\":\"Seedorf\",\"power\":\"9 MW\",\"ypos\":\"144\",\"production_series\":[{\"created\":1322214331121,\"value\":152}],\"power_as_kw\":9000,\"type\":\"Onshore Windpark\",\"location_id\":\"D66\",\"city\":\"Seedorf\",\"country\":\"Deutschland\",\"go_live\":\"2002\"},{\"xpos\":\"301\",\"name\":\"Sommerland\",\"power\":\"6 MW\",\"ypos\":\"133\",\"production_series\":[{\"created\":1322214331121,\"value\":4}],\"power_as_kw\":6000,\"type\":\"Onshore Windpark\",\"location_id\":\"D67\",\"city\":\"Sommerland\",\"country\":\"Deutschland\",\"go_live\":\"1999\"},{\"xpos\":\"274\",\"name\":\"Wönkhausen\",\"power\":\"8 MW \",\"ypos\":\"198\",\"production_series\":[{\"created\":1322214331121,\"value\":14}],\"power_as_kw\":8000,\"type\":\"Onshore Windpark\",\"location_id\":\"D71\",\"city\":\"Wönkhausen\",\"country\":\"Deutschland\",\"go_live\":\"2002 \"},{\"xpos\":\"100\",\"name\":\"BMHKW Heidelberg\",\"power\":\"???\",\"ypos\":\"100\",\"production_series\":[{\"created\":1322214331121,\"value\":13217}],\"power_as_kw\":0,\"type\":\"CHP Kohlekraftwerk\",\"location_id\":\"D998\",\"city\":\"Heidelberg\",\"country\":\"Deutschland\",\"go_live\":\"???\"},{\"xpos\":\"100\",\"ypos\":\"100\",\"power\":\"5 MWel\",\"production_series\":[{\"created\":1322214331121,\"value\":4970}],\"power_as_kw\":5000,\"type\":\"Biomassekraftwerk\",\"location_id\":\"D999\",\"country\":\"Deutschland\",\"city\":\"Wittgenstein\",\"go_live\":\"???\"},{\"xpos\":\"282\",\"name\":\"BMHKW Wittgenstein\",\"power\":\"30MWth/ 5 MWel\",\"ypos\":\"198\",\"production_series\":[{\"created\":1322214331121,\"value\":4970}],\"power_as_kw\":35000,\"type\":\"Biomassekraftwerk\",\"location_id\":\"DP1\",\"city\":\"Siegen Wittgenstein\",\"country\":\"Deutschland\",\"go_live\":\"2010\"}]";

	public static final String STATUS_EXTRA = "status";
	public static final String VALUE_EXTRA = "value";
	public static final int STATUS_IN_PROGRESS = 0;
	public static final int STATUS_FINISHED = 1;
	public static final int STATUS_PROGRESS_START = 2;
	public static final String UPDATE_ACTION = "PRODUCTION_DATA_UPDATE";
	public static final String NEW_PRODUCTION_DATA_ACTION = "NEW_PRODUCTION_DATA";

	JsonFactory mJsonFactory;

	public DownloadService() {
		super("DownloadService");
		mJsonFactory = new JsonFactory();
	}

	private List<ContentValues> parseProductionArray(JsonParser jp) throws JsonParseException, IOException {
		List<ContentValues> productionList=new ArrayList<ContentValues>();
		JsonToken token;
		ContentValues production = null;
		while ((token = jp.nextToken()) != null) {
			switch (token) {
			case START_OBJECT:
				production = new ContentValues();
				break;
			case END_OBJECT:
				productionList.add(production);
				break;
			case END_ARRAY:
				return productionList;
			case VALUE_STRING:
				production.put(jp.getCurrentName(), jp.getText());
				break;
			case VALUE_NUMBER_INT:
				production.put(jp.getCurrentName(), jp.getLongValue());
				break;
			}
		}
		return productionList;
	}

	private ContentValues parseLocation(JsonParser jp) throws JsonParseException, IOException {
		JsonToken token;
		List<ContentValues> productionList=null;
		ContentValues location = new ContentValues();
		String locationId = null;
		while ((token = jp.nextToken()) != null) {
			switch (token) {
			case START_ARRAY:
				productionList = parseProductionArray(jp);
				break;
			case END_OBJECT:
				if (locationId!=null && productionList!=null){
					mProductionMap.put(locationId, productionList);		
					for (ContentValues v: productionList){
						v.put("location_id", locationId);
					}
				}
				return location;
				
			case VALUE_STRING:
				if (jp.getCurrentName()=="location_id"){
					locationId = jp.getText();
				}
				location.put(jp.getCurrentName(), jp.getText());
				break;
			case VALUE_NUMBER_INT:				
				location.put(jp.getCurrentName(), jp.getLongValue());
				break;
			}
		}
		return null;
	}

	private Map<String, List<ContentValues>> mProductionMap = new HashMap<String, List<ContentValues>>();
	private List<ContentValues> mLocationList = new ArrayList<ContentValues>();

	
	private long getLastData(){
		Cursor c = getContentResolver().query(RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_LAST,null, null, null, null);
		long result=0;
		int createdIndex = c.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.CREATED);
		if (c.moveToFirst()){
			result = c.getLong(createdIndex);
		}
		c.close();
		return result;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null && cm.getActiveNetworkInfo().isConnected()) {
			try {
				long last = getLastData();
				long to = System.currentTimeMillis();
				long from = Math.max(last, to-1000*60*60*48);
				
				URL url = new URL("http://rwe-dashboard.appspot.com/data?from="+from+"&to="+to);
				//JsonParser jp = mJsonFactory.createJsonParser(url);
				JsonParser jp = mJsonFactory.createJsonParser(testString);
				JsonToken token;
				mLocationList.clear();
				mProductionMap.clear();
				while ((token = jp.nextToken()) != null) {
					switch (token) {
					case START_OBJECT:
						mLocationList.add(parseLocation(jp));
						break;
					}
				}
				
				if (mLocationList.size()>0){
					getContentResolver().bulkInsert(RWELiveDataContentProvider.CONTENT_URI_PLACES, mLocationList.toArray(new ContentValues[]{}));
				}
				if (mProductionMap.size()>0){
					List<ContentValues> l = new ArrayList<ContentValues>();
					for (List<ContentValues> list: mProductionMap.values()){
						l.addAll(list);
					}
					getContentResolver().bulkInsert(RWELiveDataContentProvider.CONTENT_URI_PRODUCTION, l.toArray(new ContentValues[]{}));
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}