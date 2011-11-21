package de.inovex.graph.demo;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphs.LineGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.MarkerPositionListener;
import com.jjoe64.graphview.GraphViewSeries;

public class GraphFragment extends Fragment {
	
	@SuppressWarnings(value = { "unused" })
	private static final String DEBUG_TAG = GraphFragment.class.getName();
	
	private LineGraphView mGraphView;
	private static final int VIEWPORT_SIZE = 15 * 60 * 1000;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mGraphView = (LineGraphView) inflater.inflate(R.layout.graph_fragment, container, false);
		mGraphView.setKeepScreenOn(true);
		mGraphView.setDrawBackground(false);
		mGraphView.setScrollable(true);
		mGraphView.setScalable(true);
		mGraphView.setSmoothing(false);
		mGraphView.setManualYAxisBounds(110000, 0);
		mGraphView.setViewPortSize(VIEWPORT_SIZE);
		mGraphView.setShowLegend(true);
		mGraphView.setLegendAlign(GraphView.LegendAlign.BOTTOM);
		mGraphView.setYTitle("Produktion");
		mGraphView.setYUnit("kW");
		return mGraphView;
	}

	public void setViewPortListener(GraphView.ViewportChangeListener listener) {
		mGraphView.setViewportListener(listener);
	}
	public void setMarkerPositionListener(MarkerPositionListener listener){
		mGraphView.setMarkerPositionListener(listener);		
	}
	public void addSeriesToGraph(GraphViewSeries series){
		mGraphView.addSeries(series);		
	}
}
