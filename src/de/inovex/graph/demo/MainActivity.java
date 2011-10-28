package de.inovex.graph.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.os.Bundle;

import com.jjoe64.graphview.GraphView.GraphViewSeries;

import de.inovex.graph.demo.DataListFragment.ListItemSelectedListener;

public class MainActivity extends Activity implements Runnable, ListItemSelectedListener {

	private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	private GraphFragment mGraphFragment;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        mGraphFragment = (GraphFragment) getFragmentManager().findFragmentById(R.id.graph_fragment);
	}
	


	@Override
	protected void onPause() {
		super.onPause();
		mExecutor.shutdownNow();
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			mExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally{
			mExecutor = Executors.newSingleThreadExecutor();
			//mExecutor.execute(this);
		}
	}

	public void run() {
//		GraphViewData lastData=null; 
//		while (true) {
//			
//			double offset =  (Math.random()-0.5d)*0.15;
//			mGraphView.addToSeries(0, new GraphViewData(data.valueX+1, data.valueY + offset));
//			mGraphView.setViewPort(mGraphView.getSeriesSize(0)-VIEWPORT_SIZE-1, VIEWPORT_SIZE);
//			mGraphView.postInvalidate();
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				return;
//			}
//		}

	}



	public void onListItemSelected(GraphViewSeries series) {

        if (mGraphFragment == null || !mGraphFragment.isInLayout()) {
        } else {
        	mGraphFragment.toggleSeries(series);
        }		
	}
}