package org.jside.android;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AndroidServer extends Service{
	/** Called when the activity is first created. */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
