package de.inovex.graph.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
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

import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

import de.inovex.graph.demo.MapView.Location;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider.POWER_TYPE;
import de.inovex.graph.demo.service.DownloadService;

public class DataFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private final static String DEBUG_TAG = DataFragment.class.getName();

	public interface LoaderFinishedListener {
		void onLoadFinished(int loaderId);

		void onLoadStarted(int loaderId);
	}

	public final static int PLACES_LOADER = 0; // loads all places
	public final static int PRODUCTION_LOADER = 1; // loads the
													// productionvalues for each
													// place
	public final static int PRODUCTION_LOADER_WIND = 2; // loads the production
														// total for wind
	public final static int PRODUCTION_LOADER_BIO = 3; // loads the production
														// total for biomass
	public final static int PRODUCTION_LOADER_WATER = 4; // loads the production
															// total for water

	public final static int PRODUCTION_LOADER_TOTAL = 5;

	private static final String[] sProjectionPlaces = { RWELiveDataContentProvider.Columns.Locations.NAME, RWELiveDataContentProvider.Columns.Locations.ID,
			RWELiveDataContentProvider.Columns.Locations.XPOS, RWELiveDataContentProvider.Columns.Locations.YPOS, RWELiveDataContentProvider.Columns.Locations.LAST_PRODUCTION,
			RWELiveDataContentProvider.Columns.Locations.POWER, RWELiveDataContentProvider.Columns.Locations.LOCATION_ID, RWELiveDataContentProvider.Columns.Locations.TYPE,
			RWELiveDataContentProvider.Columns.Locations.CITY, RWELiveDataContentProvider.Columns.Locations.GOLIVE, RWELiveDataContentProvider.Columns.Locations.TURBINES };

	private static final String[] sProjectionProduction = { RWELiveDataContentProvider.Columns.ProductionData.LOCATION_ID, RWELiveDataContentProvider.Columns.ProductionData.CREATED,
			RWELiveDataContentProvider.Columns.ProductionData.VALUE };

	/* contains the production series for each location */
	public static Map<String, GraphViewSeries> mProductionSeries = new HashMap<String, GraphViewSeries>();
	/* contains each location */
	public static Map<String, Location> mLocations = new HashMap<String, Location>();
	/* contains the aggregated production series for each type of location */
	public static Map<RWELiveDataContentProvider.POWER_TYPE, GraphViewSeries> productionByType = new HashMap<RWELiveDataContentProvider.POWER_TYPE, GraphViewSeries>();

	public static GraphViewSeries mTotalSeries;
	// public final static Date sDate = new Date(111, 10, 1);

	private UpdateReceiver mUpdateReceiver = new UpdateReceiver();
	private LoaderFinishedListener mListener;

	/**
	 * receives updates from DownloadService
	 * 
	 * @author renard
	 * 
	 */
	private final class UpdateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			synchronized (DataFragment.this) {
				ArrayList<ContentValues> values = intent.getParcelableArrayListExtra(DownloadService.VALUE_EXTRA);
				updateProductionLists(values);
				updateProductionByTypeLists(values);
				notifiyListener();
			}
		}
	}

	private void notifiyListener() {
		if (productionByType.containsKey(POWER_TYPE.BIOMASS) && productionByType.get(POWER_TYPE.BIOMASS).getValues().size() > 0) {
			mListener.onLoadFinished(PRODUCTION_LOADER_BIO);
		}
		if (productionByType.containsKey(POWER_TYPE.WATER) && productionByType.get(POWER_TYPE.WATER).getValues().size() > 0) {
			mListener.onLoadFinished(PRODUCTION_LOADER_WATER);
		}
		if (productionByType.containsKey(POWER_TYPE.ONSHORE_WIND) && productionByType.get(POWER_TYPE.ONSHORE_WIND).getValues().size() > 0) {
			mListener.onLoadFinished(PRODUCTION_LOADER_WIND);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (LoaderFinishedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement LoaderFinishedListener");
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().initLoader(PLACES_LOADER, null, this);
		getLoaderManager().initLoader(PRODUCTION_LOADER_TOTAL, null, this);
		getActivity().registerReceiver(mUpdateReceiver, new IntentFilter(DownloadService.NEW_PRODUCTION_DATA_ACTION));
		// acitivity was resumed, tell listener that data is available
		notifiyListener();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(mUpdateReceiver);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {	
		if (mListener!=null){
			mListener.onLoadStarted(id);
		}

		// final long start = 1321388353770L;
		// final long end = 1321441881270L;
		//
		// final String[] selectionArg = {String.valueOf(start),
		// String.valueOf(end)};
		// final String selection = "p.created>? AND p.created<?";
		// final String prod_selection = "created>? AND created<?";
		final String prod_selection = null;
		final String selection = null;
		final String[] selectionArg = null;
		switch (id) {
		case PLACES_LOADER:
			return new CursorLoader(getActivity(), RWELiveDataContentProvider.CONTENT_URI_PLACES, sProjectionPlaces, null, null, RWELiveDataContentProvider.Columns.Locations.NAME + " ASC");
		case PRODUCTION_LOADER:
			return new CursorLoader(getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION, sProjectionProduction, prod_selection, selectionArg,
					RWELiveDataContentProvider.Columns.ProductionData.CREATED + " ASC");
		case PRODUCTION_LOADER_WATER:
			return new CursorLoader(getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_WATER, null, selection, selectionArg, null);
		case PRODUCTION_LOADER_WIND:
			return new CursorLoader(getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_WIND, null, selection, selectionArg, null);
		case PRODUCTION_LOADER_BIO:
			return new CursorLoader(getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_BIO, null, selection, selectionArg, null);
		case PRODUCTION_LOADER_TOTAL:
			return new CursorLoader(getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL, null, null, null, RWELiveDataContentProvider.Columns.ProductionData.CREATED + " ASC");
		}
		return null;
	}

	public synchronized void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch (loader.getId()) {
		case PLACES_LOADER:
			if (mProductionSeries.size() == 0) {
				Log.i(DEBUG_TAG, "Starting production loaders");
				getLoaderManager().initLoader(PRODUCTION_LOADER, null, this);
				getLoaderManager().initLoader(PRODUCTION_LOADER_TOTAL, null, this);
				getLoaderManager().initLoader(PRODUCTION_LOADER_BIO, null, this);
				getLoaderManager().initLoader(PRODUCTION_LOADER_WATER, null, this);
				getLoaderManager().initLoader(PRODUCTION_LOADER_WIND, null, this);
			}

			fillLocationList(data);
			break;
		case PRODUCTION_LOADER_TOTAL:
			fillTotalList(data);
			break;
		case PRODUCTION_LOADER:
			fillProductionLists(data);
			/*
			 * do the expensive loading only once, new data will be received by
			 * broadcasts
			 */
			loader.stopLoading();
			break;
		case PRODUCTION_LOADER_BIO:
			fillProductionByTypeList(POWER_TYPE.BIOMASS, data);
			loader.stopLoading();
			break;
		case PRODUCTION_LOADER_WATER:
			fillProductionByTypeList(POWER_TYPE.WATER, data);
			loader.stopLoading();
			break;
		case PRODUCTION_LOADER_WIND:
			fillProductionByTypeList(POWER_TYPE.ONSHORE_WIND, data);
			loader.stopLoading();
			break;
		}
		if (data.getCount() > 0) {
			mListener.onLoadFinished(loader.getId());
		}

	}

	private synchronized void fillTotalList(Cursor cursor) {
		if (cursor.getCount() > 0) {
			final int totalIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.TOTAL);
			final int createdIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.CREATED);
			if (mTotalSeries == null) {
				cursor.moveToPosition(-1);
				List<GraphViewData> list = new ArrayList<GraphViewSeries.GraphViewData>();
				while (cursor.moveToNext()) {
					GraphViewData data = buildDataFromCursor(cursor, createdIndex, totalIndex);
					list.add(data);
				}
				mTotalSeries = new GraphViewSeries(list);
			} else {
				cursor.moveToPosition(cursor.getCount() - 1);
				GraphViewData data = buildDataFromCursor(cursor, createdIndex, totalIndex);
				mTotalSeries.add(data);
			}
		}
	}

	private synchronized void fillProductionByTypeList(POWER_TYPE type, Cursor cursor) {
		final int totalColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.TOTAL);
		final int createdColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.CREATED);
		List<GraphViewData> list = new ArrayList<GraphViewSeries.GraphViewData>();
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			GraphViewData data = buildDataFromCursor(cursor, createdColumnIndex, totalColumnIndex);
			list.add(data);
		}
		int color = MainActivity.getColorForType(type);
		String description = null;
		switch (type) {
		case BIOMASS:
			description = "Biomassekraftwerke";
			break;
		case WATER:
			description = "Wasserkraftwerke";
			break;
		case ONSHORE_WIND:
			description = "Windkraftwerke";
			break;
		}
		productionByType.put(type, new GraphViewSeries(description, color, list));
	}

	@Override
	public synchronized void onLoaderReset(Loader<Cursor> arg0) {

	}

	private GraphViewData buildDataFromCursor(final Cursor cursor, final int xIndexId, final int yIndexId) {
		long value = cursor.getLong(yIndexId);
		long created = cursor.getLong(xIndexId);
		return new GraphViewData(created/* - sDate.getTime() */, value);
	}

	private GraphViewData buildDataFromValues(final ContentValues values) {
		final double x = values.getAsDouble(RWELiveDataContentProvider.Columns.ProductionData.CREATED);
		final double y = values.getAsDouble(RWELiveDataContentProvider.Columns.ProductionData.VALUE);
		return new GraphViewData(x/* - sDate.getTime() */, y);
	}

	private synchronized void updateProductionByTypeLists(final List<ContentValues> values) {
		String locId;
		Map<POWER_TYPE, GraphViewData> tmpMap = new HashMap<RWELiveDataContentProvider.POWER_TYPE, GraphViewSeries.GraphViewData>();
		if (mLocations.size() > 0) {
			for (ContentValues value : values) {
				locId = value.getAsString(RWELiveDataContentProvider.Columns.ProductionData.LOCATION_ID);
				Location l = mLocations.get(locId);
				GraphViewData sumData = tmpMap.get(l.powerType);
				GraphViewData newData = buildDataFromValues(value);
				if (sumData == null) {
					tmpMap.put(l.powerType, newData);
				} else {
					sumData.valueY += newData.valueY;
				}
			}
		}
		for (POWER_TYPE type : tmpMap.keySet()) {
			GraphViewSeries series = productionByType.get(type);
			if (series == null) {
				series = new GraphViewSeries();
				productionByType.put(type, series);
			}
			series.add(tmpMap.get(type));

		}
	}

	private synchronized void updateProductionLists(final List<ContentValues> values) {
		String locId;
		for (ContentValues value : values) {
			locId = value.getAsString(RWELiveDataContentProvider.Columns.ProductionData.LOCATION_ID);
			GraphViewSeries series = mProductionSeries.get(locId);
			if (series == null) {
				series = new GraphViewSeries();
				mProductionSeries.put(locId, series);
			}
			series.add(buildDataFromValues(value));
		}
	}

	private synchronized void fillLocationList(final Cursor cursor) {
		cursor.moveToPosition(-1);
		final int xIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.XPOS);
		final int yIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.YPOS);
		final int productionIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.LAST_PRODUCTION);
		final int powerIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.POWER);
		final int idIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.LOCATION_ID);
		final int typeIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.TYPE);

		final int nameIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.NAME);
		final int cityIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.CITY);
		final int goliveIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.GOLIVE);
		final int turbinesIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.TURBINES);

		int x, y, p, pwr;
		String locId, name, city, golive, turbines;
		POWER_TYPE type;
		while (cursor.moveToNext()) {
			x = cursor.getInt(xIndex);
			y = cursor.getInt(yIndex);
			p = cursor.getInt(productionIndex);

			name = cursor.getString(nameIndex);
			city = cursor.getString(cityIndex);
			golive = cursor.getString(goliveIndex);
			turbines = cursor.getString(turbinesIndex);

			pwr = parsePower(cursor.getString(powerIndex));
			locId = cursor.getString(idIndex);
			type = POWER_TYPE.fromString(cursor.getString(typeIndex));
			/**
			 * some locations seem to have dummy location data only but the
			 * mapFragment wont draw locations which are outside the borders of
			 * germany
			 */
			ContentValues values = new ContentValues(4);
			values.put(getString(R.string.label_name), name);
			values.put(getString(R.string.label_city), city);
			values.put(getString(R.string.label_turbines), turbines);
			values.put(getString(R.string.label_golive), golive);
			values.put(getString(R.string.label_type), cursor.getString(typeIndex));
			values.put(getString(R.string.label_power), cursor.getString(powerIndex));
			mLocations.put(locId, new Location(p, pwr, x, y, type, values));
		}
	}

	public static int parsePower(final String power) {
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

	private synchronized void fillProductionLists(final Cursor cursor) {

		mProductionSeries.clear();
		long start = System.currentTimeMillis();
		int idIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.LOCATION_ID);
		int valueColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.VALUE);
		int createdColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.CREATED);

		String locId;
		Map<String, List<GraphViewData>> tempMap = new HashMap<String, List<GraphViewData>>();

		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			locId = cursor.getString(idIndex);
			GraphViewData data = buildDataFromCursor(cursor, createdColumnIndex, valueColumnIndex);
			List<GraphViewData> list = tempMap.get(locId);
			if (list == null) {
				list = new ArrayList<GraphViewSeries.GraphViewData>();
				tempMap.put(locId, list);
			}
			list.add(data);
		}
		for (String key : tempMap.keySet()) {
			List<GraphViewData> list = tempMap.get(key);
			mProductionSeries.put(key, new GraphViewSeries(list));
		}

		Log.i(DEBUG_TAG, "Time to load data for first time" + (System.currentTimeMillis() - start));

	}
}