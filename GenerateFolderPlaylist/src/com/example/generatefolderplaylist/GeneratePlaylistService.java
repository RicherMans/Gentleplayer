package com.example.generatefolderplaylist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class GeneratePlaylistService extends Service {

	final static long MINUMUM_LENGTH = 1024 * 1024;
	private static Object sync = new Object();
	// Root path, which will be searched
	private String pathToMedia = null;
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

	private void writeFile(String folderPath, List<String> list) {
		String tmp[] = folderPath.split("/");
		if (tmp != null) {
			String playlistName = tmp[tmp.length - 1];
			File playlistPa = new File(PLAYLISTPATH);
			if (!playlistPa.exists()) {
				playlistPa.mkdir();
			}
			File f = new File(PLAYLISTPATH + playlistName + ".m3u");
			BufferedWriter out = null;
			try {
				if(f.exists()){
					f.delete();
				}
				f.createNewFile();
				out = new BufferedWriter(new FileWriter(f));
				for (String song : list) {
					out.write(song + "\n");
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (out != null)
					try {
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	private void stop(){
		this.stopSelf();
	}

	private class SearchTreeJob extends
			AsyncTask<String, List<String>, HashMap<String, List<String>>> {

		@Override
		protected void onPostExecute(HashMap<String, List<String>> result) {
			super.onPostExecute(result);
			if (result != null) {
				for (String key : result.keySet()) {
					Log.d("Key V", key + result.get(key));
					writeFile(key, result.get(key));
				}
				stop();
			}
		}

		@Override
		protected HashMap<String, List<String>> doInBackground(String... params) {
			if (params.length > 1) {
				return null;
			}
			File root = new File(params[0]);
			if (root != null) {
				HashMap<String, List<String>> pathstomp = new HashMap<String, List<String>>();
				try {
					pathstomp = searchNodesRec(root, pathstomp);
					return pathstomp;
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		private HashMap<String, List<String>> searchNodesRec(File dir,
				HashMap<String, List<String>> paths)
				throws MalformedURLException {
			if (dir == null) {
				return paths;
			}
			for (File f : dir.listFiles()) {
				if (f.isFile()) {
					String type = getMimeType(f.toURI().toString().trim());
					if (type != null && type.startsWith("audio")) {
						Log.d("Media ", f.toURL().toString());
						if (f.length() > MINUMUM_LENGTH) {
							List<String> pth = null;
							if (paths.containsKey(f.getParentFile().getPath())) {
								pth = paths.get(f.getParentFile().getPath());
								pth.add(f.getPath());
							} else {
								pth = new ArrayList<String>();
								pth.add(f.getPath());
								if (pathToMedia == null) {
									pathToMedia = new String(f.getParentFile()
											.getAbsolutePath() + "/");
								}
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
				String extension = MimeTypeMap.getFileExtensionFromUrl(url);
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
