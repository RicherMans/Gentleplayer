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
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class GeneratePlaylistService extends Service {

	final static long MINUMUM_LENGTH = 1024 * 1024;
	private static Object sync = new Object();
	private static final String TAG = "GeneratePlaylistService";

	// Root path, which will be searched
	final static String PLAYLISTPATH = Environment
			.getExternalStorageDirectory().getAbsolutePath() + "/Playlists/";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	private void writeFile(String playlistName, ArrayDeque<String> list,
			HashMap<String, String> currentPlaylists) {
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
			ContentValues values = new ContentValues();
			f.createNewFile();
			out = new BufferedWriter(new FileWriter(f));
			StringBuffer m3UDat = new StringBuffer();
//			Uri insTrackToPl = Playlists.Members.getContentUri("external",
//					Long.parseLong(currentPlaylists.get(playlistName)));
			for (String song : list) {
				m3UDat.append(song);
//				values.put(Playlists.Members.DATA, song);
//				getContentResolver().update(insTrackToPl, values,Playlists.Members.DATA + "= ?", new String[]{"Peter Long Penis s"});
				m3UDat.append("\n");
			}
			out.write(m3UDat.toString());
			if (currentPlaylists.containsKey(playlistName)) {
				//Update
			} else {
				getContentResolver()
				.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
						values);
			}
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

	private void stop() {
		this.stopSelf();
	}

	private class SearchTreeJob
			extends
			AsyncTask<String, ArrayDeque<String>, HashMap<String, ArrayDeque<String>>> {
		@Override
		protected void onPostExecute(HashMap<String, ArrayDeque<String>> result) {
			super.onPostExecute(result);
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
		protected HashMap<String, ArrayDeque<String>> doInBackground(
				String... params) {
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
			return null;
		}

		private HashMap<String, ArrayDeque<String>> searchNodesRec(File dir,
				HashMap<String, ArrayDeque<String>> paths)
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
				String extension = url.substring(url.lastIndexOf(".") + 1,
						url.length());
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
		synchronized (sync) {
			new SearchTreeJob().execute(Environment
					.getExternalStorageDirectory().getAbsolutePath());
		}
		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
