package de.inovex.graph.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.jjoe64.graphview.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;

import de.inovex.graph.demo.MapView.Location;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider.POWER_TYPE;

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
			RWELiveDataContentProvider.Columns.Locations.XPOS, RWELiveDataContentProvider.Columns.Locations.YPOS, RWELiveDataContentProvider.Columns.Locations.POWER_AS_KW,
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

	private LoaderFinishedListener mListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (LoaderFinishedListener) activity;
			getLoaderManager().initLoader(PLACES_LOADER, null, this);
			getLoaderManager().initLoader(PRODUCTION_LOADER, null, this);
			getLoaderManager().initLoader(PRODUCTION_LOADER_BIO, null, this);
			getLoaderManager().initLoader(PRODUCTION_LOADER_WATER, null, this);
			getLoaderManager().initLoader(PRODUCTION_LOADER_WIND, null, this);

		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement LoaderFinishedListener");
		}
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
			return new CursorLoader(getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION, sProjectionProduction, prod_selection, selectionArg, RWELiveDataContentProvider.Columns.ProductionData.CREATED + " ASC");
		case PRODUCTION_LOADER_WATER:
			return new CursorLoader(getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_WATER, null, selection, selectionArg, null);
		case PRODUCTION_LOADER_WIND:
			return new CursorLoader(getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_WIND, null, selection, selectionArg, null);
		case PRODUCTION_LOADER_BIO:
			return new CursorLoader(getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_BIO, null, selection, selectionArg, null);
		}
		return null;
	}

	public synchronized void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch (loader.getId()) {
		case PLACES_LOADER:
			fillLocationList(data);
			break;
		case PRODUCTION_LOADER:
			fillProductionLists(data);
			break;
		case PRODUCTION_LOADER_BIO:
			fillProductionByTypeList(POWER_TYPE.BIOMASS, data);
			break;
		case PRODUCTION_LOADER_WATER:
			fillProductionByTypeList(POWER_TYPE.WATER, data);
			break;
		case PRODUCTION_LOADER_WIND:
			fillProductionByTypeList(POWER_TYPE.ONSHORE_WIND, data);
			break;
		}
		mListener.onLoadFinished(loader.getId());
	}

	private synchronized void fillProductionByTypeList(POWER_TYPE type, Cursor cursor) {
		final int totalColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.TOTAL);
		final int createdColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.CREATED);
		List<GraphViewData> list = new ArrayList<GraphViewData>();
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


	private synchronized void fillLocationList(final Cursor cursor) {
		cursor.moveToPosition(-1);
		final int xIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.XPOS);
		final int yIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.YPOS);
		final int powerKWIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.POWER_AS_KW);
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

			name = cursor.getString(nameIndex);
			city = cursor.getString(cityIndex);
			golive = cursor.getString(goliveIndex);
			turbines = cursor.getString(turbinesIndex);

			pwr = cursor.getInt(powerKWIndex);
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
			mLocations.put(locId, new Location(0, pwr, x, y, type, values));
		}
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
				list = new ArrayList<GraphViewData>();
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
