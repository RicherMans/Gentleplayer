package de.richermans.generatefolderplaylist;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
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
		
		Toast.makeText(this, getResources().getString(R.string.activated),Toast.LENGTH_SHORT).show();
		//Just for debugging purpose ... isnt called when debuggale is set to false
		boolean isDebuggable =  ( 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
		if(isDebuggable){
			wipeAllPlaylists();
			debugTests();
		}
		finish();
	}
	
	private void wipeAllPlaylists(){
		getContentResolver().delete(Playlists.EXTERNAL_CONTENT_URI, null, null);
	}
	
	private void debugTests(){
		Cursor cur = getContentResolver().query(
				MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null,
				null, null, null);
		HashMap<String, String> currentPlaylists = new HashMap<String, String>(
					30);
			while (cur.moveToNext()) {
				currentPlaylists.put(
						cur.getString(cur.getColumnIndex(Playlists.NAME)),
						cur.getString(cur.getColumnIndex(Playlists._ID)));
			}
		
		for(Map.Entry<String,String > plN : currentPlaylists.entrySet()){
			Log.d("Playlist",plN.getKey() + "  " +plN.getValue() );
			Uri playlist = Playlists.Members.getContentUri("external",
					Long.parseLong(plN.getValue()));
			Cursor curs = getContentResolver().query(playlist, null, Playlists.DATA +" = " + "'/mnt/sdcard/media/audio/music/Tom Hangs ft Shermanology - Blessed (Avicii Edit)-www.manomuzika.nkk.lt.mp3'", null, null);
			while(curs.moveToNext()){
				ContentValues val = new ContentValues();
				String track = curs.getString(curs.getColumnIndex(Playlists.Members.TRACK));
				String album = curs.getString(curs.getColumnIndex(Playlists.Members.ALBUM));
				String audioID = curs.getString(curs.getColumnIndex(Playlists.Members.AUDIO_ID));
				String artists  = curs.getString(curs.getColumnIndex(Playlists.Members.ARTIST));
				String disp = curs.getString(curs.getColumnIndex(Playlists.Members.DISPLAY_NAME));
				String title = curs.getString(curs.getColumnIndex(Playlists.Members.TITLE));
				String data = curs.getString(curs.getColumnIndex(Playlists.Members.DATA));
				val.put(Playlists.Members.DATA, "PENIS");
//				String dateAdd = curs.getString(curs.getColumnIndex(Playlists.Members.DATE_ADDED));
//				addToPlaylist(Integer.parseInt(audioID),Long.parseLong(plN.getValue()));
//					
//				Log.d("Track Artist ... " , track + album  +artists + disp + dateAdd + title);
			}
			curs.close();
		}
		
	}
	public void addToPlaylist(int audioId,long playlistId) {
        String[] cols = new String[] {
                "count(*)"
        };
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        Cursor cur = getContentResolver().query(uri, cols, null, null, null);
        cur.moveToFirst();
        final int base = cur.getInt(0);
        cur.close();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(base + audioId));
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
        getContentResolver().insert(uri, values);
    }


}
