package org.jside.android;



import org.jside.android.AndServer.AndBinder;
import org.jside.webserver.action.ActionWebServer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AndActivity extends Activity implements ServiceConnection{
	private ActionWebServer ws;
	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent andservice = new Intent(this,AndServer.class);
		this.bindService(andservice, this , Context.BIND_AUTO_CREATE);

		setContentView(R.layout.main);
		Button open = (Button) super.findViewById(R.id.open);
		open.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Intent browseIntent = new Intent(
						Intent.ACTION_VIEW,Uri.parse("http://127.0.0.1:"+ws.getPort()+"/"));
				startActivity(browseIntent);
			}
		});
		Button exit = (Button) super.findViewById(R.id.exit);
		exit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//finish();
				System.exit(1);
			}
		});
//		Vibrator vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//		vb.vibrate(1000*60*10);
	}
	public void onServiceConnected(ComponentName name, IBinder service) {
		ws = ((AndBinder)service).getWebServer();
		this.initailize();
	}

	public void onServiceDisconnected(ComponentName name) {
	}

	public void initailize() {
		EditText link = (EditText) this.findViewById(R.id.link);
		try {
			//ws.start();
			String ip = "127.0.0.1";
			WifiManager wf = (WifiManager)this.getSystemService(WIFI_SERVICE);
			if(wf!=null && wf.getWifiState() == WifiManager.WIFI_STATE_ENABLED){
				DhcpInfo dh = wf.getDhcpInfo();
				if(dh!=null){
					ip = getIp(dh.ipAddress);
				}
			}
			link.setText("http://" + ip+":"+ws.getPort()+"/");
		} catch (Throwable e) {
			Log.e("And Error:",Log.getStackTraceString(e));
		}
	}
	private String getIp(int i) {
		return (0xFF & i) + "." + (0xFF & (i >> 8)) + '.'
		+ (0xFF & (i >> 16)) + '.' + (0xFF & (i >> 24));
	}
}
