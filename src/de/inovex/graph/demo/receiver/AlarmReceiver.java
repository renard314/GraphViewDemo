package de.inovex.graph.demo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import de.inovex.graph.demo.service.DownloadService;


public class AlarmReceiver extends BroadcastReceiver {
	 
    private static final String DEBUG_TAG = "AlarmReceiver";
 
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(DEBUG_TAG, "Recurring alarm; requesting download service.");
        // start the download
        
        Intent downloader = new Intent(context, DownloadService.class);
        downloader.setData(Uri.parse("http://www.rwe.com/app/tso/xmltransfer.aspx?f=innogysites.xml"));
        context.startService(downloader);
    }
 
}