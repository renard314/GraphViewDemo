package de.inovex.graph.demo;

import java.util.Date;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphs.LineGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;

public class GraphFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private final static String[] sProjection = new String[] { RWELiveDataContentProvider.Columns.ProductionData.CREATED, RWELiveDataContentProvider.Columns.ProductionData.TOTAL };
	private final static Date sDate = new Date(111, 10, 1);
	private GraphViewSeries mTotalProductionSeries = null;


	private GraphViewData buildDataFromCursor(Cursor cursor) {
		int valueColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.TOTAL);
		int createdColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.CREATED);
		long value = cursor.getLong(valueColumnIndex);
		long created = cursor.getLong(createdColumnIndex);
		return new GraphViewData(created - sDate.getTime(), value);
	}

	private LineGraphView mGraphView;
	private static final int VIEWPORT_SIZE = 5 * 60 * 1000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mGraphView = (LineGraphView) inflater.inflate(R.layout.graph_fragment, container, false);
		mGraphView.setKeepScreenOn(true);
		mGraphView.setDrawBackground(true);
		mGraphView.setScrollable(true);
		mGraphView.setScalable(true);
		mGraphView.setSmoothing(false);
		mGraphView.setManualYAxisBounds(220000, 160000);		
		mGraphView.setViewPort(System.currentTimeMillis() - sDate.getTime(), VIEWPORT_SIZE);
		return mGraphView;
	}

	
	public GraphViewSeries getGraphSeries(){
		return mTotalProductionSeries;
	}
	
	public void setViewPortListener(GraphView.ViewportChangeListener listener){
		mGraphView.setViewportListener(listener);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this.getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_MINUTE, sProjection, null, null, RWELiveDataContentProvider.Columns.ProductionData.CREATED + " ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		if (cursor.getCount() == 0) {
			return;
		}

		final boolean seriesCreated;
		if (mTotalProductionSeries == null) {
			mTotalProductionSeries = new GraphViewSeries();
			mTotalProductionSeries.setVisible(true);
			cursor.moveToPosition(-1);
			seriesCreated = true;
		} else {
			cursor.moveToPosition(cursor.getCount() - 2);
			seriesCreated = false;
		}

		while (cursor.moveToNext()) {
			mTotalProductionSeries.add(buildDataFromCursor(cursor));
		}
		GraphFragment.this.getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (seriesCreated) {
					mGraphView.addSeries(mTotalProductionSeries);
				}
				mGraphView.moveViewPortStartToTheEnd();
			}
		});
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {}

}
