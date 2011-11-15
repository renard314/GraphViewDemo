package de.inovex.graph.demo;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ViewFlipper;

import com.jjoe64.graphview.GraphView.MarkerPositionListener;
import com.jjoe64.graphview.GraphView.ViewportChangeListener;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

import de.inovex.graph.demo.DataCache.LoaderFinishedListener;
import de.inovex.graph.demo.MapView.Location;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider.POWER_TYPE;
import de.inovex.mindtherobot.Thermometer;

public class MainActivity extends Activity implements ViewportChangeListener, MarkerPositionListener, LoaderFinishedListener {

	public final static String DEBUG_TAG = MainActivity.class.getPackage().toString();
	private GraphFragment mGraphFragment;
	private MapFragment mMapFragment;
	private Thermometer mGaugeWind;
	private Thermometer mGaugeWater;
	private Thermometer mGaugeBio;
	private ViewFlipper mGaugeContainer;
	private final static String[] sProjection = new String[] { RWELiveDataContentProvider.Columns.Locations.POWER };

	private static final int sWindColor = Color.rgb(0xaa, 0x30, 0xbb);
	private static final int sWaterColor = Color.rgb(0x30, 0x60, 0xdd);
	private static final int sCoalColor = Color.rgb(0xdd, 0xdd, 0x34);
	private static final int sDefaultColor = Color.WHITE;
	private static final int sBioMassColor = Color.rgb(0x30, 0xdd, 0x30);

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
				int childCount = mGaugeContainer.getChildCount();
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
		DataCache cache = new DataCache();
		transaction.add(cache, DataCache.class.getSimpleName());
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
		mGraphFragment.setViewPortListener(null);
		mGraphFragment.setMarkerPositionListener(null);
		super.onPause();
	}

	@Override
	protected void onResume() {
		mGraphFragment.setViewPortListener(this);
		mGraphFragment.setMarkerPositionListener(this);
		super.onResume();
	}

	@Override
	public void onMarkerPositionChanged(double oldPos, double newPos) {
		if (DataCache.productionByType.size() > 0) {
			int bioPower = 0;
			int windPower = 0;
			int waterPower = 0;
			for (Location l : DataCache.mLocations.values()) {
				switch (l.powerType) {
				case WATER:
					waterPower += l.power;
					break;
				case BIOMASS:
					bioPower += l.power;
					break;
				case ONSHORE_WIND:
					windPower += l.power;
					break;
				}
			}
			GraphViewSeries series = DataCache.productionByType.get(POWER_TYPE.BIOMASS);
			if (series != null) {
				final GraphViewData nearest = series.getNearestValue(newPos);
				if (nearest != null && nearest.valueX != mLastStart) {
					upDateGauge(POWER_TYPE.BIOMASS, bioPower, newPos);
					upDateGauge(POWER_TYPE.WATER, waterPower, newPos);
					upDateGauge(POWER_TYPE.ONSHORE_WIND, windPower, newPos);
					mLastStart = nearest.valueX;
					mMapFragment.updateMap(nearest.valueX);
				}
			}
		}		
	}

	
	private double mLastStart = -1;
	private double mLastMarkerPos = -1;
	@Override
	public void onViewportChanged(double start, double size) {
		
//		if (DataCache.productionByType.size() > 0) {
//			int bioPower = 0;
//			int windPower = 0;
//			int waterPower = 0;
//			for (Location l : DataCache.mLocations.values()) {
//				switch (l.powerType) {
//				case WATER:
//					waterPower += l.power;
//					break;
//				case BIOMASS:
//					bioPower += l.power;
//					break;
//				case ONSHORE_WIND:
//					windPower += l.power;
//					break;
//				}
//			}
//			GraphViewSeries series = DataCache.productionByType.get(POWER_TYPE.BIOMASS);
//			if (series != null) {
//				final GraphViewData nearest = series.getNearestValue(start + size);
//				if (nearest != null && nearest.valueX != mLastStart) {
//					upDateGauge(POWER_TYPE.BIOMASS, bioPower, start + size);
//					upDateGauge(POWER_TYPE.WATER, waterPower, start + size);
//					upDateGauge(POWER_TYPE.ONSHORE_WIND, windPower, start + size);
//					mLastStart = nearest.valueX;
//					mMapFragment.updateMap(nearest.valueX);
//				}
//			}
//		}
	}

	private void upDateGauge(POWER_TYPE type, int maxPower, double viewportEnd) {
		GraphViewSeries series = DataCache.productionByType.get(type);
		if (series != null) {

			final GraphViewData nearest = series.getNearestValue(viewportEnd);
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

	@Override
	public void onLoadFinished(int loaderId) {
		Log.i(DEBUG_TAG, "onLoadFinished =" + loaderId);
		switch (loaderId) {
		case DataCache.PRODUCTION_LOADER_BIO:
			mGraphFragment.addSeriesToGraph(DataCache.productionByType.get(POWER_TYPE.BIOMASS));
			break;
		case DataCache.PRODUCTION_LOADER_WATER:
			mGraphFragment.addSeriesToGraph(DataCache.productionByType.get(POWER_TYPE.WATER));
			break;
		case DataCache.PRODUCTION_LOADER_WIND:
			mGraphFragment.addSeriesToGraph(DataCache.productionByType.get(POWER_TYPE.ONSHORE_WIND));
			break;
		case DataCache.PLACES_LOADER:
			mMapFragment.loadMap();
			break;
		case DataCache.PRODUCTION_LOADER_TOTAL:
			// mGraphFragment.addSeriesToGraph(DataCache.mTotalSeries);
			break;

		}
	}
}