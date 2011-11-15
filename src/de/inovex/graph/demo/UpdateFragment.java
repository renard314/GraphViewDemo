package de.inovex.graph.demo;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import de.inovex.graph.demo.anim.Rotate3dAnimation;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider;
import de.inovex.graph.demo.service.DownloadService;

public class UpdateFragment extends Fragment implements OnSharedPreferenceChangeListener {

	private static final String PREF_KEY = "IS_UPDATING";
	private static final String DEBUG_TAG = UpdateFragment.class.getSimpleName();
	private ViewGroup mProgressView;
	private ViewGroup mContainer;
	private ViewGroup mCountdownView;
	private ProgressBar mProgressBar;
	private TextView mTextViewProgress;
	private TextView mTextViewCountdown;
	private TextView mTextViewLabel;
	private ToggleButton mToggleButton;
	private boolean mIsShowing = false;
	private Runnable mTicker;
	private Handler mHandler;
	private Calendar mCalendar;
	private final static String mFormat = "mm:ss";
	private String mProgressMessage;
	private static final int TIME_TO_UPDATE = 20; //Time between updates in seconds
	private int mUpdateCounter = TIME_TO_UPDATE;
	private final static int REQUEST_CODE = 0;
	private final UpdateReceiver mUpdateReceiver = new UpdateReceiver();
	private SharedPreferences mPrefs;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCalendar = Calendar.getInstance();
		mProgressMessage = getResources().getString(R.string.update_places_text);
		mHandler = new Handler();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mPrefs = activity.getSharedPreferences(UpdateFragment.class.getSimpleName(), 0 );
		mPrefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onUpdateStart() {
		applyRotation(0, 0, 90);
		mIsShowing = false;
		mProgressBar.setProgress(0);
		mHandler.removeCallbacks(mTicker);
		
	}
	private int getLocationCount(){
		Cursor c = getActivity().getContentResolver().query(RWELiveDataContentProvider.CONTENT_URI_PLACES_COUNT, null, null,null, null);
		c.moveToFirst();
		int total = c.getInt(0);
		c.close();
		return total;
	}
	
	public void onUpdateFinished(int totalCount) {
		applyRotation(-1, 0, 90);
		mUpdateCounter = TIME_TO_UPDATE;
		mIsShowing = true;
		mProgressBar.setMax(totalCount);
		startTicker();
	}
	
	public void onUpdateProgress(int value) {
		mProgressBar.setProgress(value);
		String progressMsg = String.format(mProgressMessage, value);
		mTextViewProgress.setText(progressMsg);
		mTextViewProgress.invalidate();
	}

	private void cancelUpdate(){
		Intent intent = new Intent(getActivity(), AlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(this.getActivity(), REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		Log.i(DEBUG_TAG, "canceled alarm");
	}
	
	private void scheduleUpdate(){
		
		Intent intent = new Intent(getActivity(), AlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(this.getActivity(), REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000*30, sender);
		Log.i(DEBUG_TAG, "scheduled alarm: interval in seconds = " + TIME_TO_UPDATE);

	}

	/**
	 * receives updates from DownloadService
	 * @author renard
	 *
	 */
	private final class UpdateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int val = intent.getIntExtra(DownloadService.VALUE_EXTRA, 0);
			int status = intent.getIntExtra(DownloadService.STATUS_EXTRA, 0);
				switch(status){
				case DownloadService.STATUS_PROGRESS_START:
					onUpdateStart();
					break;
				case DownloadService.STATUS_FINISHED:
					onUpdateFinished(val);
					break;
				case DownloadService.STATUS_IN_PROGRESS:
					onUpdateProgress(val);
					break;				
				}
		}	
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mContainer = (ViewGroup) inflater.inflate(R.layout.update_fragment, container, false);

		mCountdownView = (ViewGroup) mContainer.findViewById(R.id.countdown);
		mProgressView = (ViewGroup) mContainer.findViewById(R.id.progress);
		mProgressBar = (ProgressBar) mContainer.findViewById(R.id.progressBarUpdate);
		mTextViewCountdown = (TextView) mContainer.findViewById(R.id.textViewCountdown);
		mTextViewProgress = (TextView) mContainer.findViewById(R.id.textViewProgress);
		mToggleButton = (ToggleButton) mContainer.findViewById(R.id.toggleUpdateButton);
		mTextViewLabel = (TextView) mContainer.findViewById(R.id.textViewLabel);
		mToggleButton.setChecked(mPrefs.getBoolean(PREF_KEY, false));

		mToggleButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				boolean val = mToggleButton.isChecked();
				Editor edit = mPrefs.edit();
				edit.putBoolean(PREF_KEY, val);
				edit.apply();				
			}
		});
		
		

		int total = getLocationCount();
		if (total==0){
			mProgressBar.setMax(42);
		} else {
			mProgressBar.setMax(total);			
		}		
		return mContainer;
	}


	
	
	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(mUpdateReceiver);
		mIsShowing = false;
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().registerReceiver(mUpdateReceiver, new IntentFilter(DownloadService.UPDATE_ACTION));
		mIsShowing = true;
		if (mPrefs.getBoolean(PREF_KEY, false)){
			startTicker();
		}
	}
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("countdown", mUpdateCounter);
		super.onSaveInstanceState(outState);
		
	}

	private void startTicker() {
		/**
		 * requests a tick on the next hard-second boundary
		 */
		mTicker = new Runnable() {
			public void run() {

				if (!mIsShowing) {
					return;
				}
				if (mUpdateCounter < 0) {

				} else {
					mCalendar.clear();
					mCalendar.set(Calendar.SECOND, mUpdateCounter--);
					mTextViewCountdown.setText(DateFormat.format(mFormat, mCalendar));
					mTextViewCountdown.invalidate();
					long now = SystemClock.uptimeMillis();
					long next = now + (1000 - now % 1000);
					mHandler.postAtTime(mTicker, next);
				}
			}
		};
		mTicker.run();
	}

	/**
	 * Setup a new 3D rotation on the container view.
	 * 
	 * @param position
	 *            the item that was clicked to show a picture, or -1 to show the
	 *            list
	 * @param start
	 *            the start angle at which the rotation must begin
	 * @param end
	 *            the end angle of the rotation
	 */
	private void applyRotation(int position, float start, float end) {
		// Find the center of the container
		final float centerX = mContainer.getWidth() / 2.0f;
		final float centerY = mContainer.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(start, end, centerX, centerY, 310.0f, true);
		rotation.setDuration(500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new DisplayNextView(position));

		mContainer.startAnimation(rotation);
	}

	/**
	 * This class listens for the end of the first half of the animation. It
	 * then posts a new action that effectively swaps the views when the
	 * container is rotated 90 degrees and thus invisible.
	 */
	private final class DisplayNextView implements Animation.AnimationListener {
		private final int mPosition;

		private DisplayNextView(int position) {
			mPosition = position;
		}

		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
			mContainer.post(new SwapViews(mPosition));
		}

		public void onAnimationRepeat(Animation animation) {
		}
	}

	/**
	 * This class is responsible for swapping the views and start the second
	 * half of the animation.
	 */
	private final class SwapViews implements Runnable {
		private final int mPosition;

		public SwapViews(int position) {
			mPosition = position;
		}

		public void run() {
			final float centerX = mContainer.getWidth() / 2.0f;
			final float centerY = mContainer.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (mPosition > -1) {
				mCountdownView.setVisibility(View.GONE);
				mProgressView.setVisibility(View.VISIBLE);
				rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
			} else {
				mProgressView.setVisibility(View.GONE);
				mCountdownView.setVisibility(View.VISIBLE);
				mTextViewLabel.setVisibility(View.VISIBLE);
				mTextViewCountdown.setVisibility(View.VISIBLE);
				rotation = new Rotate3dAnimation(270, 0, centerX, centerY, 310.0f, false);
			}

			rotation.setDuration(500);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());

			mContainer.startAnimation(rotation);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PREF_KEY)){
			boolean val = sharedPreferences.getBoolean(PREF_KEY, false);
			if (val){
				scheduleUpdate();
			} else {
				cancelUpdate();
				mTextViewLabel.setVisibility(View.INVISIBLE);
				mTextViewCountdown.setVisibility(View.INVISIBLE);
				mIsShowing = false;

			}
		}
	}


}
