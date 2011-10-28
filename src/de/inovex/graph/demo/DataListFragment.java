package de.inovex.graph.demo;

import java.util.ArrayList;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;

import android.app.Activity;
import android.app.ListFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class DataListFragment extends ListFragment {

	public interface ListItemSelectedListener {
		public void onListItemSelected(GraphViewSeries series);
	}

	private ListItemSelectedListener mListener;
	private int index = 0;

	private final String[] sDataSourceNames = {
				"Datenquelle 1", 
				"Datenquelle 2", 
				"Datenquelle 3", 
				"Datenquelle 4", 
				"Datenquelle 5", 
				"Datenquelle 6" };
	
	private final GraphViewSeries[] sDataSources = { 
			generateRandomData(100),
			generateRandomData(200),
			generateRandomData(80),
			generateRandomData(20),
			generateRandomData(120),
			generateRandomData(100)};

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item, sDataSourceNames);
		setListAdapter(adapter);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		if (savedInstanceState != null) {
			index = savedInstanceState.getInt("index", 0);
			mListener.onListItemSelected(sDataSources[index]);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("index", index);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (ListItemSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ListItemSelectedListener in Activity");
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		mListener.onListItemSelected(sDataSources[position]);
	}
	
	private int createRandomColor() {
		return Color.argb(255,(int)(Math.random()*255),(int)( Math.random()*255), (int)(Math.random()*255));
	}
	private GraphViewSeries generateRandomData(int size) {
		ArrayList<GraphViewData> result = new ArrayList<GraphView.GraphViewData>(size);
		double lastValue = 2.5;
		result.add(new GraphViewData(0, 2.5));
		for (int i =  1; i < size; i++){
			lastValue = result.get(i-1).valueY;
			double offset =  (Math.random()-0.5d)*0.15;
			result.add(new GraphViewData(i, lastValue+offset));
		}
		return new GraphViewSeries(createRandomColor(),result);
	}

}
