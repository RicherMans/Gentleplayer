package de.richermans.generatefolderplaylist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayDeque;
import java.util.HashMap;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Audio.Playlists.Members;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class GeneratePlaylistService extends Service {

	private final static long MINUMUM_LENGTH = 1024 * 1024;
	private static Object sync = new Object();
	private static final String TAG = "GeneratePlaylistService";

	// Root path, which will be searched
	final static String PLAYLISTPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Playlists/";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	private void writeFile(String playlistName, ArrayDeque<String> list, HashMap<String, Long> currentPlaylists) {
		File playlistPa = new File(PLAYLISTPATH);
		if (!playlistPa.exists()) {
			playlistPa.mkdir();
		}
		File f = new File(PLAYLISTPATH + playlistName + ".m3u");
		BufferedWriter out = null;
		try {
			if (f.exists()) {
				f.delete();
			}
			f.createNewFile();
			out = new BufferedWriter(new FileWriter(f));
			StringBuffer m3UDat = new StringBuffer();
			Long playlistId = currentPlaylists.get(playlistName);
			Uri insTrackToPl = null;
			if (playlistId != null){
				//playlist is already existing in database
				insTrackToPl = Playlists.Members.getContentUri("external", playlistId);
			}else{
				// Playlist does not exist in the current Database, so add it
				ContentValues cplayListName = new ContentValues();
				cplayListName.put(Playlists.NAME, playlistName);
				getContentResolver().insert(Playlists.EXTERNAL_CONTENT_URI,cplayListName);
			}
			for (String song : list) {
				m3UDat.append(song);
				// "update playlist"
				if (insTrackToPl != null) {
					//some songs do have special characters which lead to sql errors
					song = song.replaceAll("[-+.^:,']", "");
					Cursor isDataIns = getContentResolver().query(insTrackToPl, null,
							Members.DATA + " = '" + song + "'", null, null);
					//Possible that Cursor has 0 elements and will throw exception if moveToFirst is called
					if (isDataIns.getCount() > 0) {
						isDataIns.moveToFirst();
						int audioId = isDataIns.getInt(isDataIns.getColumnIndex(Members.AUDIO_ID));
						if (audioId <= 0) {
							addToPlaylist(audioId, playlistId);
						}
					}
					isDataIns.close();
				}
				m3UDat.append("\n");
			}
			out.write(m3UDat.toString());

		} catch (FileNotFoundException e) {
			stop();
			e.printStackTrace();
		} catch (IOException e) {
			stop();
			e.printStackTrace();
		} finally {
			// java 7 pro
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	private void addToPlaylist(int audioId, long playlistId) {
		String[] cols = new String[] { "count(*)" };
		Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
		Cursor cur = getContentResolver().query(uri, cols, null, null, null);
		if (cur.getCount() > 0) {
			cur.moveToFirst();
			final int base = cur.getInt(0);
			cur.close();
			ContentValues values = new ContentValues();
			values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(base + audioId));
			values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
			getContentResolver().insert(uri, values);
		}
	}

	private void stop() {
		this.stopSelf();
	}

	private class SearchTreeJob extends AsyncTask<String, ArrayDeque<String>, HashMap<String, ArrayDeque<String>>> {
		@Override
		protected void onPostExecute(HashMap<String, ArrayDeque<String>> result) {
			super.onPostExecute(result);
			Cursor cur = getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null,
					null);
			HashMap<String, Long> currentPlaylists = new HashMap<String, Long>(30);
			if (cur != null) {
				while (cur.moveToNext()) {
					currentPlaylists.put(cur.getString(cur.getColumnIndex(Playlists.NAME)),
							cur.getLong(cur.getColumnIndex(Playlists._ID)));

				}
			}
			cur.close();
			if (result != null) {
				for (String key : result.keySet()) {
					Log.d("Key V", key + result.get(key));
					String tmpA[] = key.split("/");
					String playlistName = tmpA[tmpA.length - 1];
					Log.d("Media V", playlistName);
					writeFile(playlistName, result.get(key), currentPlaylists);
				}
				// TODO: Use contentresolver to manually add the Playlist files
				// into Androids DB
				stop();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			stop();
		}

		@Override
		protected HashMap<String, ArrayDeque<String>> doInBackground(String... params) {
			synchronized (sync) {
				if (params.length > 1) {
					return null;
				}
				File root = new File(params[0]);
				if (root != null) {
					HashMap<String, ArrayDeque<String>> pathstomp = new HashMap<String, ArrayDeque<String>>();
					try {
						pathstomp = searchNodesRec(root, pathstomp);
						return pathstomp;
					} catch (MalformedURLException e) {
						stop();
					}
				}
			}
			return null;
		}

		private HashMap<String, ArrayDeque<String>> searchNodesRec(File dir, HashMap<String, ArrayDeque<String>> paths)
				throws MalformedURLException {
			if (dir == null) {
				return paths;
			}
			for (File f : dir.listFiles()) {
				if (f.isFile()) {
					String type = getMimeType(f.toURL().toString().trim());
					if (type != null && type.startsWith("audio")) {
						Log.d("Media ", f.toURL().toString());
						if (f.length() > MINUMUM_LENGTH) {
							ArrayDeque<String> pth = null;
							if (paths.containsKey(f.getParentFile().getPath())) {
								pth = paths.get(f.getParentFile().getPath());
								pth.add(f.getPath());
							} else {
								pth = new ArrayDeque<String>();
								pth.add(f.getPath());
							}
							paths.put(f.getParentFile().getPath(), pth);
						}
					}
				}
				if (f.isDirectory() && !f.isHidden()) {
					searchNodesRec(f, paths);
				}
			}
			return paths;
		}

		private String getMimeType(String url) {
			if (url != null) {
				String type = null;
				String extension = url.substring(url.lastIndexOf(".") + 1, url.length());
				if (extension != null) {
					MimeTypeMap mime = MimeTypeMap.getSingleton();
					type = mime.getMimeTypeFromExtension(extension);
				}
				return type;
			}
			return null;
		}

	}

	@Override
	public synchronized void onStart(Intent intent, int startId) {
		Toast.makeText(this, getResources().getString(R.string.app_started), Toast.LENGTH_SHORT).show();
		new SearchTreeJob().execute(Environment.getExternalStorageDirectory().getAbsolutePath());
		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
