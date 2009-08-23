package org.jside.android.fileserver;

import java.net.MalformedURLException;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AndroidServer extends Activity{
	static AndroidServer instance;
	/** Called when the activity is first created. */
	private org.jside.fileserver.FileServer ws;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		instance = this;
		try {
			ws = new org.jside.fileserver.FileServer();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Button exit = (Button) super.findViewById(R.id.exit);
		exit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				System.exit(0);
			}
		});
		this.start();
	}
	public static void alert(String msg){
		AlertDialog.Builder builder = new AlertDialog.Builder(instance);
		builder.setMessage(msg)
		       .setCancelable(true)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}

	public boolean prompt(String msg){
		final boolean[] rtv = new boolean[]{false};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg)
		       .setCancelable(true)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   rtv[0] = true;
		        	   dialog.cancel();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   rtv[0] = false; 
		        	   dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();

		return rtv[0];
	}

	public void start() {
		EditText link = (EditText) this.findViewById(R.id.link);
		try {
			ws.start();
			String ip = "127.0.0.1";
			WifiManager wf = (WifiManager)this.getSystemService(WIFI_SERVICE);
			if(wf!=null){
				DhcpInfo dh = wf.getDhcpInfo();
				if(dh!=null){
					ip = getIp(dh.ipAddress);
				}
			}
			link.setText("http://" + ip + ":" + ws.getPort());
		} catch (Throwable e) {
			alert(Log.getStackTraceString(e));
		}
	}
	private String getIp(int i) {
		return (0xFF & i) + "." + (0xFF & (i >> 8)) + '.'
		+ (0xFF & (i >> 16)) + '.' + (i >> 24);
	}
}
