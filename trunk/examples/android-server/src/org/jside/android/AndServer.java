package org.jside.android;


import java.net.MalformedURLException;
import java.net.URL;

import org.jside.webserver.action.ActionWebServer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class AndServer extends Service{
	/** Called when the activity is first created. */
	private ActionWebServer ws;
	@Override
	public IBinder onBind(Intent intent) {
		try {
			URL webBase = new java.io.File("/sdcad/htdocs/").toURI().toURL();
			ws = new ActionWebServer(webBase);
		} catch (MalformedURLException e) {
		}
		return new AndBinder();
	}
	class AndBinder extends Binder{
		public ActionWebServer getWebServer(){
			return ws;
		}

	}

}
