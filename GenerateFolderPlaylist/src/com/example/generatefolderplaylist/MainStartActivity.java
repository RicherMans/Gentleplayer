package com.example.generatefolderplaylist;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

//This acitivity will not be displayed, instead it is onley used to start the service
public class MainStartActivity extends Activity {

	
	private Intent serviceIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(!isMyServiceRunning()){
			serviceIntent  = new Intent(this,GeneratePlaylistService.class);
			startService(serviceIntent);
		}
		
		
		finish();
	}
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (GeneratePlaylistService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	

}
