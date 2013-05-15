package com.example.generatefolderplaylist;

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
	//I didnt do that she was legal

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

	private void writeFile(String playlistName, ArrayDeque<String> list) {
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
			for (String song : list) {
				m3UDat.append(song);
				m3UDat.append("\n");
			}
			out.write(m3UDat.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
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
			if (result != null) {
				for (String key : result.keySet()) {
//					ContentValues values = new ContentValues();
					Log.d("Key V", key + result.get(key));
					String tmpA[] = key.split("/");
					String playlistName = tmpA[tmpA.length - 1];
					Log.d("Media V",playlistName);
//					values.put(Playlists.NAME, playlistName);
					writeFile(playlistName, result.get(key));
//					getContentResolver().update(
//							MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
//							values,null,null);
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
