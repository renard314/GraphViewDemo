package de.inovex.graph.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import de.inovex.graph.demo.service.DownloadService;

public class AlarmReceiver extends BroadcastReceiver {

	private static final String DEBUG_TAG = "AlarmReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(DEBUG_TAG, "Recurring alarm; requesting download service.");
		Intent downloader = new Intent(context, DownloadService.class);
		context.startService(downloader);
	}

}
