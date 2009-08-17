package org.jside.server;

import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Server extends Activity{
	/** Called when the activity is first created. */
	private FileServer ws = new FileServer();

	@Override
	public void onCreate(Bundle savedInstanceState) {
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

	public void start() {
		EditText link = (EditText) this.findViewById(R.id.link);
		try {
			ws.start();
			WifiManager wm = (WifiManager) this.getSystemService(Activity.WIFI_SERVICE);
			String ip = "127.0.0.1";
			if (wm != null && wm.getConnectionInfo() !=null) {
				int i = wm.getConnectionInfo().getIpAddress();
				ip = (i >> 24) + "." + (0xFF & (i >> 16)) + '.'
						+ (0xFF & (i >> 8)) + '.' + (0xFF & i);
			}
			link.setText("http://" + ip + ":" + ws.getPort());
		} catch (Throwable e) {
			String msg = e.getMessage();
			link.setText(msg.replaceAll("[\r\n]", ""));
		}
	}
}
