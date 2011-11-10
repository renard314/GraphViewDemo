package de.inovex.graph.demo;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.jjoe64.graphview.GraphView.ViewportChangeListener;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;
import de.inovex.mindtherobot.Thermometer;

public class MainActivity extends Activity implements ViewportChangeListener{

	private GraphFragment mGraphFragment;
	private MapFragment mMapFragment;
	private Thermometer mGauge;
	private int mMaxPower;
	private final static String[] sProjection = new String[] { RWELiveDataContentProvider.Columns.Locations.POWER};


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.main);
        mGraphFragment = (GraphFragment) getFragmentManager().findFragmentById(R.id.graph_fragment);
        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mGauge = (Thermometer) findViewById(R.id.gauge_solar);
        mMaxPower = getMaxPower();
        
        mGauge.setTitle(getString(R.string.workload_title));
        
//        mGauge.setMaxValue(60);
//        mGauge.setMinValue(10);
//        mGauge.setTotalNicks(30);
	}
	
	private int getMaxPower(){
		int maxPower = 0;
		Cursor c = getContentResolver().query(RWELiveDataContentProvider.CONTENT_URI_PLACES, sProjection, null, null, null);
		int index = c.getColumnIndex(RWELiveDataContentProvider.Columns.Locations.POWER);
		c.moveToPosition(-1);
		while(c.moveToNext()){
			maxPower+=MapFragment.parsePower(c.getString(index));
		}
		c.close();
		return maxPower;
	}
	


	@Override
	protected void onPause() {
        mGraphFragment.setViewPortListener(null);
		super.onPause();
	}

	@Override
	protected void onResume() {
        mGraphFragment.setViewPortListener(this);
		super.onResume();
	}
	
	private double mLastStart = -1;

	@Override
	public void onViewportChanged(double start, double size) {
		final GraphViewData nearest = mGraphFragment.getGraphSeries().getNearestValue(start);
		if (nearest.valueX!=mLastStart){			
			final float y = (float)nearest.valueY;
			final float percent = 100f *  y / mMaxPower;
			mGauge.setHandTarget(percent);
			mLastStart = nearest.valueX;
			mMapFragment.updateMap(nearest.valueX);
		}		
	}


	
}