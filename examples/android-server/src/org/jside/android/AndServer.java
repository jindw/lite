package org.jside.android;


import java.net.MalformedURLException;
import java.net.URL;

import org.jside.filemanager.FileManager;
import org.jside.webserver.action.ActionWebServer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AndServer extends Service{
	/** Called when the activity is first created. */
	private ActionWebServer ws;
	@Override
	public IBinder onBind(Intent intent) {
		getServer();
		return new AndBinder();
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		if(ws!=null){
			ws.stop();
		}
		return super.onUnbind(intent);
	}

	private ActionWebServer getServer(){
		if(ws== null){
			java.io.File webBase = new java.io.File("/sdcard/htdocs/");
			try {
				ws = new ActionWebServer(webBase.toURI().toURL());
			} catch (MalformedURLException e) {
				Log.e("ERROR:", Log.getStackTraceString(e));
			}
			FileManager fs = new FileManager(webBase.getParentFile(),"/fs/");
			ws.addAction("/fs/**", fs);
			ws.start();
		}
		return ws;
	}
	class AndBinder extends Binder{
		public ActionWebServer getWebServer(){
			return getServer();
		}
	}

}
