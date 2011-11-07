package de.inovex.graph.demo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Fragment;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.Loader.OnLoadCompleteListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphs.LineGraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;

public class GraphFragment extends Fragment implements OnLoadCompleteListener<Cursor>{

	
	Date mDate = new Date(110,0,1);
	private CursorLoader mCursorLoader = null;
	
	private class ProductionContentObserver extends ContentObserver  {

		public ProductionContentObserver(Handler handler) {
			super(handler);
		}
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			Log.i("GraphFragment","OnChange");
			if (mCursor!=null){
				mCursor.requery();
				if (mCursor.moveToLast()) {
					mGraphView.addToSeries(0, buildDataFromCursor(mCursor));
				}
			}
		}
	}
	
	private GraphViewData buildDataFromCursor(Cursor cursor){
		int valueColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.TOTAL);
		int createdColumnIndex = cursor.getColumnIndex(RWELiveDataContentProvider.Columns.ProductionData.CREATED);
		long value = mCursor.getLong(valueColumnIndex);
		long created = mCursor.getLong(createdColumnIndex);		
		return new GraphViewData(created-mDate.getTime(), value);
	}
	
	private LineGraphView mGraphView;
	private static final int VIEWPORT_SIZE = 5*60*100;
	private Cursor mCursor;
	private ProductionContentObserver mContentObserver = new ProductionContentObserver(new Handler());
	private GraphViewSeries mTotalSeries;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCursorLoader = new CursorLoader(this.getActivity(), RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_MINUTE,null, null, null, RWELiveDataContentProvider.Columns.ProductionData.CREATED + " ASC");
		mCursorLoader.registerListener(0, this);
		mCursorLoader.startLoading();
	}
	

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	mGraphView = (LineGraphView)inflater.inflate(R.layout.graph_fragment, container, false);
		mGraphView.setKeepScreenOn(true);
		mGraphView.setDrawBackground(true);
		mGraphView.setScrollable(true);
		mGraphView.setScalable(true);
		mGraphView.setSmoothing(true);
		double start = System.currentTimeMillis()- mDate.getTime();
		mGraphView.setViewPort(start, VIEWPORT_SIZE );
		mGraphView.setManualYAxisBounds(600000, 550000);
//		List<GraphViewData> list = new ArrayList<GraphViewSeries.GraphViewData>();
//		Log.i("TIME","" + start);
//		list.add(new GraphViewData(start, Math.random()*1000+50000));
//		GraphViewSeries series = new GraphViewSeries(list);
//		mGraphView.addSeries(series);
        return mGraphView;
    }
    
	@Override
	public void onLoadComplete(Loader<Cursor> loader, Cursor cursor) {
		mCursor = cursor;
		List<GraphViewData> dataList = new ArrayList<GraphViewData>();
		
		cursor.moveToPosition(-1);
		while(cursor.moveToNext()){
			dataList.add(buildDataFromCursor(cursor));
		}
		if (dataList.size()>0) {		
			Log.i("GraphFragment","onLoadComplete");

			GraphViewSeries series = new GraphViewSeries(dataList);
			series.setVisible(true);
			mGraphView.addSeries(series);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
	}
	
	public void addRandomValue(){		
		double time = System.currentTimeMillis();
		time = time -mDate.getTime();
		GraphViewData data = new GraphViewData(time, Math.random()*1000+50000);
		mGraphView.addToSeries(0, data);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		getActivity().getContentResolver().registerContentObserver(RWELiveDataContentProvider.CONTENT_URI_PRODUCTION_TOTAL_MINUTE, false, mContentObserver);
	}
		
    public void toggleSeries(GraphViewSeries series) {
		mGraphView.toggleSeries(series);    	
    }    
    
    public void addSeries(GraphViewSeries series){
    	mGraphView.addSeries(series);
    }

}
