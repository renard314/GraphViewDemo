package de.inovex.graph.demo;

import java.util.Date;

import android.app.Fragment;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.Loader.OnLoadCompleteListener;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphs.LineGraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;

public class GraphFragment extends Fragment implements OnLoadCompleteListener<Cursor> {

	Date mDate = new Date(110, 0, 1);
	private CursorLoader mCursorLoader = null;
	private GraphViewSeries mTotalProductionSeries = null;

	private GraphViewData buildDataFromCursor(Cursor cursor) {
		int valueColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.TOTAL);
		int createdColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.CREATED);
		long value = cursor.getLong(valueColumnIndex);
		long created = cursor.getLong(createdColumnIndex);
		return new GraphViewData(created - mDate.getTime(), value);
	}

	private LineGraphView mGraphView;
	private static final int VIEWPORT_SIZE = 180 * 60 * 1000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCursorLoader = new CursorLoader(this.getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_MINUTE, new String[] { RWELiveDataContentProvider.Columns.ProductionData.CREATED,
				RWELiveDataContentProvider.Columns.ProductionData.TOTAL }, null, null, RWELiveDataContentProvider.Columns.ProductionData.CREATED + " ASC");
		mCursorLoader.registerListener(0, this);
		mCursorLoader.startLoading();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mGraphView = (LineGraphView) inflater.inflate(R.layout.graph_fragment, container, false);
		mGraphView.setKeepScreenOn(true);
		mGraphView.setDrawBackground(true);
		mGraphView.setScrollable(true);
		mGraphView.setScalable(true);
		mGraphView.setSmoothing(false);
		mGraphView.setManualYAxisBounds(600000, 300000);
		return mGraphView;
	}

	@Override
	public void onLoadComplete(Loader<Cursor> loader, final Cursor cursor) {
		if (cursor.getCount() == 0) {
			return;
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				final boolean seriesCreated;
				if (mTotalProductionSeries == null) {
					mTotalProductionSeries = new GraphViewSeries();
					mTotalProductionSeries.setVisible(true);
					mGraphView.setViewPortSize(VIEWPORT_SIZE);
					cursor.moveToPosition(-1);
					seriesCreated = true;
				} else {
					cursor.moveToPosition(cursor.getCount()-2);
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
		}).run();

	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public void addRandomValue() {
		double time = System.currentTimeMillis();
		time = time - mDate.getTime();
		GraphViewData data = new GraphViewData(time, Math.random() * 1000 + 50000);
		mGraphView.addToSeries(0, data);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	public void toggleSeries(GraphViewSeries series) {
		mGraphView.toggleSeries(series);
	}

	public void addSeries(GraphViewSeries series) {
		mGraphView.addSeries(series);
	}

}
