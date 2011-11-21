package de.inovex.graph.demo;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ViewFlipper;

import com.jjoe64.graphview.GraphView.MarkerPositionListener;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

import de.inovex.graph.demo.DataFragment.LoaderFinishedListener;
import de.inovex.graph.demo.MapView.Location;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider.POWER_TYPE;
import de.inovex.mindtherobot.Thermometer;

public class MainActivity extends Activity implements MarkerPositionListener, LoaderFinishedListener {

	public final static String DEBUG_TAG = MainActivity.class.getPackage().toString();

	private static final int sWindColor = Color.rgb(0xaa, 0x30, 0xbb);
	private static final int sWaterColor = Color.rgb(0x30, 0x60, 0xdd);
	private static final int sCoalColor = Color.rgb(0xdd, 0xdd, 0x34);
	private static final int sDefaultColor = Color.WHITE;
	private static final int sBioMassColor = Color.rgb(0x30, 0xdd, 0x30);

	private GraphFragment mGraphFragment;
	private MapFragment mMapFragment;
	private Thermometer mGaugeWind;
	private Thermometer mGaugeWater;
	private Thermometer mGaugeBio;
	private ViewFlipper mGaugeContainer;
	private int mMaxBioPower = 0;
	private int mMaxWindPower = 0;
	private int mMaxWaterPower = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.main);
		mGraphFragment = (GraphFragment) getFragmentManager().findFragmentById(R.id.graph_fragment);
		mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
		mGaugeContainer = (ViewFlipper) findViewById(R.id.gauge_container);
		mGaugeContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int currentChild = mGaugeContainer.getDisplayedChild();
				final int childCount = mGaugeContainer.getChildCount();
				currentChild++;
				if (currentChild == childCount) {
					currentChild = 0;
				}
				mGaugeContainer.setDisplayedChild(currentChild);
			}
		});
		
		mGaugeWind = (Thermometer) findViewById(R.id.gauge_wind);
		mGaugeWater = (Thermometer) findViewById(R.id.gauge_water);
		mGaugeBio = (Thermometer) findViewById(R.id.gauge_bio);

		mGaugeWind.setTitle(getString(R.string.workload_wind_title));
		mGaugeWater.setTitle(getString(R.string.workload_water_title));
		mGaugeBio.setTitle(getString(R.string.workload_bio_title));

		mGaugeBio.setMaxValue(50);
		mGaugeBio.setMinValue(0);
		mGaugeBio.setTotalNicks(50);
		mGaugeBio.setScaleInterval(3);
		mGaugeBio.setRimColor(getColorForType(POWER_TYPE.BIOMASS));

		mGaugeWater.setMaxValue(50);
		mGaugeWater.setMinValue(0);
		mGaugeWater.setTotalNicks(50);
		mGaugeWater.setScaleInterval(3);
		mGaugeWater.setRimColor(getColorForType(POWER_TYPE.WATER));

		mGaugeWind.setMaxValue(50);
		mGaugeWind.setMinValue(0);
		mGaugeWind.setTotalNicks(50);
		mGaugeWind.setScaleInterval(3);
		mGaugeWind.setRimColor(getColorForType(POWER_TYPE.ONSHORE_WIND));
		
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		DataFragment cache = new DataFragment();
		transaction.add(cache, DataFragment.class.getSimpleName());
		transaction.commit();

	}

	public static int getColorForType(POWER_TYPE type) {
		switch (type) {
		case ONSHORE_WIND:
			return sWindColor;
		case BIOMASS:
			return sBioMassColor;
		case CHP_COAL:
			return sCoalColor;
		case WATER:
			return sWaterColor;
		default:
			return sDefaultColor;
		}
	}

	@Override
	protected void onPause() {
		mGraphFragment.setMarkerPositionListener(null);
		super.onPause();
	}

	@Override
	protected void onResume() {
		mGraphFragment.setMarkerPositionListener(this);
		super.onResume();
	}

	@Override
	public void onMarkerPositionChanged(double oldPos, double newPos) {
		if (DataFragment.productionByType.size() > 0) {

			GraphViewSeries series = DataFragment.productionByType.get(POWER_TYPE.BIOMASS);
			if (series != null) {
				final GraphViewData nearest = series.getNearestValue(newPos);
				if (nearest != null && nearest.valueX != mLastStart) {
					upDateGauge(POWER_TYPE.BIOMASS, mMaxBioPower, newPos);
					upDateGauge(POWER_TYPE.WATER, mMaxWaterPower, newPos);
					upDateGauge(POWER_TYPE.ONSHORE_WIND, mMaxWindPower, newPos);
					mLastStart = nearest.valueX;
					mMapFragment.updateMap(nearest.valueX);
				}
			}
		}		
	}

	
	private double mLastStart = -1;

	private void upDateGauge(POWER_TYPE type, final int maxPower, final double posX) {
		GraphViewSeries series = DataFragment.productionByType.get(type);
		if (series != null) {

			final GraphViewData nearest = series.getNearestValue(posX);
			final float y = (float) nearest.valueY;
			final float percent = 100f * y / maxPower;
			switch (type) {
			case BIOMASS:
				mGaugeBio.setHandTarget(percent);
				break;
			case WATER:
				mGaugeWater.setHandTarget(percent);
				break;
			case ONSHORE_WIND:
				mGaugeWind.setHandTarget(percent);
				break;

			}
		}
	}
	
	private void calculateMaxProductions(){
		mMaxBioPower = 0;
		mMaxWaterPower = 0;
		mMaxWindPower = 0;
		for (Location l : DataFragment.mLocations.values()) {
			switch (l.powerType) {
			case WATER:
				mMaxWaterPower += l.power;
				break;
			case BIOMASS:
				mMaxBioPower += l.power;
				break;
			case ONSHORE_WIND:
				mMaxWindPower += l.power;
				break;
			}
		}
	}

	@Override
	public void onLoadFinished(int loaderId) {
		Log.i(DEBUG_TAG, "onLoadFinished =" + loaderId);
		switch (loaderId) {
		case DataFragment.PRODUCTION_LOADER_BIO:
			mGraphFragment.addSeriesToGraph(DataFragment.productionByType.get(POWER_TYPE.BIOMASS));
			break;
		case DataFragment.PRODUCTION_LOADER_WATER:
			mGraphFragment.addSeriesToGraph(DataFragment.productionByType.get(POWER_TYPE.WATER));
			break;
		case DataFragment.PRODUCTION_LOADER_WIND:
			mGraphFragment.addSeriesToGraph(DataFragment.productionByType.get(POWER_TYPE.ONSHORE_WIND));
			break;
		case DataFragment.PLACES_LOADER:
			mMapFragment.loadMap();
			calculateMaxProductions();
			break;
		case DataFragment.PRODUCTION_LOADER_TOTAL:
			// mGraphFragment.addSeriesToGraph(DataCache.mTotalSeries);
			break;

		}
	}
}