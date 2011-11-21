package de.inovex.graph.demo;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphViewSeries;

import de.inovex.graph.demo.MapView.Location;

public class MapFragment extends Fragment{

	private MapView mMapView;
	private static final String DEBUG_TAG = MapFragment.class.getSimpleName();

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mMapView = (MapView) inflater.inflate(R.layout.map_fragment, container, false);
		return mMapView;
	}
	
	public void loadMap(){
		Log.i(DEBUG_TAG, "loading data into map");
		for (Location l : DataFragment.mLocations.values()){
			mMapView.addLocation(l);
		}
		mMapView.invalidate();
	}

	public void updateMap(final double valueX) {
		for (String locId : DataFragment.mProductionSeries.keySet()) {
			GraphViewSeries series = DataFragment.mProductionSeries.get(locId);
			if (series.getValues().size() > 0) {
				double p = series.getNearestValue(valueX).valueY;
				Location loc = DataFragment.mLocations.get(locId);
				if (loc != null) {
					mMapView.addLocation((int) p, loc.power, loc.x, loc.y, loc.powerType, loc.values);
				} else {
					// this happens if the location had no valid x and y value
				}
			}
		}

	}
}
