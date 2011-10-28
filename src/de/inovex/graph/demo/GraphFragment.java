package de.inovex.graph.demo;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

public class GraphFragment extends Fragment {

	private LineGraphView mGraphView;
	private int mCurrentValueX = 0;
	private static final int VIEWPORT_SIZE = 20;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	mGraphView = (LineGraphView)inflater.inflate(R.layout.graph_fragment, container, false);
		mGraphView.setViewPort(mCurrentValueX, VIEWPORT_SIZE);
		mGraphView.setKeepScreenOn(true);
		mGraphView.setDrawBackground(true);
		mGraphView.setScrollable(true);
		mGraphView.setSmoothing(true);
        return mGraphView;
    }
		
    public void toggleSeries(GraphViewSeries series) {
		mGraphView.toggleSeries(series);    	
    }    

}
