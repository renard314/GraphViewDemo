package de.inovex.graph.demo;

import android.app.Activity;
import android.os.Bundle;
import de.inovex.graph.demo.DataListFragment.ListItemSelectedListener;

public class MainActivity extends Activity implements ListItemSelectedListener {

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
	}

	@Override
	protected void onResume() {
		super.onResume();
	}


	public void onListItemSelected(long locationId) {

        if (mGraphFragment == null || !mGraphFragment.isInLayout()) {
        } else {
        	//mGraphFragment.toggleSeries(series);
        }		
	}
	
}