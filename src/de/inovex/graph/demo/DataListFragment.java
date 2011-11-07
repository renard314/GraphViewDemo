package de.inovex.graph.demo;

import android.app.Activity;
import android.app.ListFragment;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.Loader.OnLoadCompleteListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;

/**
 * shows the list of locations
 * 
 * @author renard
 * 
 */
public class DataListFragment extends ListFragment implements OnLoadCompleteListener<Cursor> {

	private static final String[] sProjection = { RWELiveDataContentProvider.Columns.Locations.NAME, RWELiveDataContentProvider.Columns.Locations.ID };
	private CursorLoader mCursorLoader = null;
	public interface ListItemSelectedListener {
		public void onListItemSelected(long locationId);
	}

	private class LocationContentObserver extends ContentObserver {

		public LocationContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			if (mCursor != null && mCursorLoader!=null) {
				mCursorLoader.startLoading();
			}
		}

	}

	private ListItemSelectedListener mListener;
	private long index = 0;
	private LocationContentObserver mContentObserver = new LocationContentObserver(new Handler());
	private Cursor mCursor = null;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mCursorLoader = new CursorLoader(this.getActivity(), RWELiveDataContentProvider.CONTENT_URI_PLACES, sProjection, null, null, RWELiveDataContentProvider.Columns.Locations.NAME + " ASC");
		mCursorLoader.registerListener(1, this);
		mCursorLoader.startLoading();

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		if (savedInstanceState != null) {
			index = savedInstanceState.getInt("index", 0);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("index", index);
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
		index = id;
		mListener.onListItemSelected(id);
	}

	@Override
	public void onPause() {
		super.onPause();
		getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().getContentResolver().registerContentObserver(RWELiveDataContentProvider.CONTENT_URI_PLACES, true, mContentObserver);
	}

	@Override
	public void onLoadComplete(Loader<Cursor> loader, Cursor cursor) {

		if (cursor != null) {
			String[] from = { RWELiveDataContentProvider.Columns.Locations.NAME };
			int[] to = { R.id.textviewName };

			CursorAdapter adapter = new SimpleCursorAdapter(getActivity(), R.layout.list_item, cursor, from, to);
			setListAdapter(adapter);

			mCursor = cursor;

			mListener.onListItemSelected(index);
		}

	}

}
