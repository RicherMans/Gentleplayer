package de.richermans.generatefolderplaylist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class SDCardPluggedInReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("Media", intent.getAction());
		Intent i = new Intent(
				"com.example.generatefolderplaylist.GeneratePlaylistService");
		i.setClass(context, GeneratePlaylistService.class);
		context.startService(i);
		// if(pathToMedia == null)
		// new SearchTreeJob().execute(filep.getEncodedPath());
		// else
		// new SearchTreeJob().execute(pathToMedia);
	}
}
