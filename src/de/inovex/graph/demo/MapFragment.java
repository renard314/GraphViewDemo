package de.inovex.graph.demo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

import de.inovex.graph.demo.MapView.Location;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;
import de.inovex.graph.demo.service.DownloadService;

public class MapFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private final static int PLACES_LOADER = 0; // loads all places
	private final static int PRODUCTION_LOADER = 1; // loads the
													// productionvalues for each
													// place

	private static final String[] sProjectionPlaces = { RWELiveDataContentProvider.Columns.Locations.NAME, RWELiveDataContentProvider.Columns.Locations.ID,
			RWELiveDataContentProvider.Columns.Locations.XPOS, RWELiveDataContentProvider.Columns.Locations.YPOS, RWELiveDataContentProvider.Columns.Locations.LAST_PRODUCTION,
			RWELiveDataContentProvider.Columns.Locations.POWER, RWELiveDataContentProvider.Columns.Locations.LOCATION_ID};

	private static final String[] sProjectionProduction = { RWELiveDataContentProvider.Columns.ProductionData.LOCATION_ID, RWELiveDataContentProvider.Columns.ProductionData.CREATED,
			RWELiveDataContentProvider.Columns.ProductionData.VALUE };
	
	Map<String,GraphViewSeries> mProductionSeries = new HashMap<String, GraphViewSeries>();
	Map<String, Location> mLocations = new HashMap<String, Location>();

	private final static Date sDate = new Date(111, 10, 1);
	private UpdateReceiver mUpdateReceiver = new UpdateReceiver();

	/**
	 * receives updates from DownloadService
	 * @author renard
	 *
	 */
	private final class UpdateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			ArrayList<ContentValues> values = intent.getParcelableArrayListExtra(DownloadService.VALUE_EXTRA);
			fillProductionLists(values);
		}
			
		
	}
	
	private GraphViewData buildDataFromCursor(Cursor cursor) {
		int valueColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.VALUE);
		int createdColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.CREATED);
		long value = cursor.getLong(valueColumnIndex);
		long created = cursor.getLong(createdColumnIndex);
		return new GraphViewData(created - sDate.getTime(), value);
	}

	
	private MapView mMapView;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		getLoaderManager().initLoader(PLACES_LOADER, null, this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(mUpdateReceiver);
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().registerReceiver(mUpdateReceiver, new IntentFilter(DownloadService.NEW_PRODUCTION_DATA_ACTION));
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case PLACES_LOADER:
			return new CursorLoader(this.getActivity(), RWELiveDataContentProvider.CONTENT_URI_PLACES, sProjectionPlaces, null, null, RWELiveDataContentProvider.Columns.Locations.NAME + " ASC");
		case PRODUCTION_LOADER:
			return new CursorLoader(this.getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION, sProjectionProduction, null, null, RWELiveDataContentProvider.Columns.ProductionData.CREATED + " ASC");
		}
		return null;
	}

	public synchronized void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data.getCount() > 0) {
			switch (loader.getId()) {
			case PLACES_LOADER:
				fillMapView(data);
				if (mProductionSeries.size()==0){
					getLoaderManager().initLoader(PRODUCTION_LOADER, null, this);
				}
				break;
			case PRODUCTION_LOADER:
				fillProductionLists(data);
				/*do the expensive loading only once, new data will be received by broadcasts*/
				getLoaderManager().destroyLoader(PRODUCTION_LOADER);
				break;
			}
		}
	}

	@Override
	public synchronized void onLoaderReset(Loader<Cursor> arg0) {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mMapView = (MapView) inflater.inflate(R.layout.map_fragment, container, false);
		return mMapView;
	}

	public static int parsePower(String power) {
		final String expr = "MW(el\\/|th\\/|el|th|())";
		if (power.isEmpty()) {
			return 0;
		}
		if (power.equals("???")) {
			return 0;
		}
		String[] parts = power.trim().split(expr);
		float sum = 0;
		for (String part : parts) {
			try {
				Float value = Float.parseFloat(part.replace(',', '.'));
				sum += value;
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return (int) (sum * 1000);
	}

	private void fillMapView(final Cursor cursor) {
		if (getActivity() != null) {
			cursor.moveToPosition(-1);
			int xIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.XPOS);
			int yIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.YPOS);
			int productionIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.LAST_PRODUCTION);
			int powerIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.POWER);
			int idIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.LOCATION_ID);
			int x, y, p, pwr;
			String locId;
			while (cursor.moveToNext()) {
				x = cursor.getInt(xIndex);
				y = cursor.getInt(yIndex);
				p = cursor.getInt(productionIndex);
				pwr = parsePower(cursor.getString(powerIndex));
				locId = cursor.getString(idIndex);
				/** some locations seem to have dummy location data only */
				if (x != 100 && y != 100 && pwr > 0) {
					mMapView.addLocation(p, pwr, x, y);
					mLocations.put(locId, new Location(p,pwr,x,y));
				}
			}
		}
	}
	
	public void updateMap(double valueX){
		for (String locId : mProductionSeries.keySet()){
			GraphViewSeries series = mProductionSeries.get(locId);
			double p = series.getNearestValue(valueX).valueY;
			Location loc = mLocations.get(locId);
			if (loc!=null){
				mMapView.addLocation((int)p, loc.power, loc.x, loc.y);
			} else {
				//this happens if the location had no valid x and y value
			}
		}
		
	}
	
	private synchronized void fillProductionLists(List<ContentValues> values){
		String locId;
		long start = System.currentTimeMillis();
		double x,y;
		for (ContentValues value : values){
			locId= value.getAsString(RWELiveDataContentProvider.Columns.ProductionData.LOCATION_ID);
			GraphViewSeries series = mProductionSeries.get(locId);
			if(series == null){
				series = new GraphViewSeries();
				mProductionSeries.put(locId, series);
			}
			x = value.getAsDouble(RWELiveDataContentProvider.Columns.ProductionData.CREATED);
			y = value.getAsDouble(RWELiveDataContentProvider.Columns.ProductionData.VALUE);
			series.add(new GraphViewData(x, y));
		}
		Log.i("fillProductionLists", "Time to update data" + (System.currentTimeMillis() - start));
		
	}
	
	private synchronized void fillProductionLists(final Cursor cursor){
		mProductionSeries.clear();
		long start = System.currentTimeMillis();
		int idIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.LOCATION_ID);
		String locId;
		
		cursor.moveToPosition(-1);
		while(cursor.moveToNext()){
			locId = cursor.getString(idIndex);
			GraphViewData data = buildDataFromCursor(cursor);
			GraphViewSeries series = mProductionSeries.get(locId);
			if(series == null){
				series = new GraphViewSeries();
				mProductionSeries.put(locId, series);
			}
			series.add(data);
		}
		Log.i("fillProductionLists", "Time to load data" + (System.currentTimeMillis() - start));
	}
}
