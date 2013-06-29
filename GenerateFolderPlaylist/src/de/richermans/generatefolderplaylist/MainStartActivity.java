package de.richermans.generatefolderplaylist;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Audio.Playlists.Members;
import android.util.Log;
import android.widget.Toast;

//This acitivity will not be displayed, instead it is onley used to start the service
public class MainStartActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Toast.makeText(this, getResources().getString(R.string.activated), Toast.LENGTH_SHORT).show();
		// Just for debugging purpose ... isnt called when debuggale is set to
		// false
		boolean isDebuggable = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
		if (isDebuggable) {
//			 wipeAllPlaylists();
//			debugTests();
//			 Intent i = new Intent(
//			 "com.example.generatefolderplaylist.GeneratePlaylistService");
//			 i.setClass(this, GeneratePlaylistService.class);
//			 startService(i);
		} else {
			finish();
		}
	}

	private void wipeAllPlaylists() {
		getContentResolver().delete(Playlists.EXTERNAL_CONTENT_URI, null, null);
	}

	private void debugTests() {
		Cursor cur = getContentResolver()
				.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null, null);
		HashMap<String, String> currentPlaylists = new HashMap<String, String>(30);
		cur.moveToFirst();
		while (cur.moveToNext()) {
			currentPlaylists.put(cur.getString(cur.getColumnIndex(Playlists.NAME)),
					cur.getString(cur.getColumnIndex(Playlists._ID)));
		}

		for (Map.Entry<String, String> plN : currentPlaylists.entrySet()) {
			Log.d("Playlist", plN.getKey() + "  " + plN.getValue());
			Uri playlist = Playlists.Members.getContentUri("external", Long.parseLong(plN.getValue()));
			Uri insTrackToPl;
			insTrackToPl = Playlists.Members.getContentUri("external", Long.parseLong(plN.getValue()));
			Cursor curss = getContentResolver().query(insTrackToPl, null, null, null, null);
			while(curss.moveToNext()){
				
				String data = curss.getString(curss.getColumnIndex(Playlists.Members.DATA));
				String x = curss.getString(curss.getColumnIndex(Playlists.Members.ALBUM));
				Cursor isTrackAlreadyInserted = getContentResolver().query(insTrackToPl, null,
						Members.DATA + " = '" + data+ "'", null, null);
				while(isTrackAlreadyInserted.moveToNext()){
					String m = isTrackAlreadyInserted.getString(isTrackAlreadyInserted.getColumnIndex(Playlists.Members.DATA));
				}
				
			}
			
			
		}

	}

}
