package org.jside.webserver;

import java.io.File;

import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Server extends Activity{
	/** Called when the activity is first created. */
	MutiThreadWebServer ws;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		new Thread(new Executor(this)).start();
		setContentView(R.layout.main);
		Button exit = (Button) super.findViewById(R.id.exit);
		exit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				System.exit(0);
			}
		});

		EditText link = (EditText) super.findViewById(R.id.link);
		link.setText("111");
	}
}
class Executor implements Runnable{
	private Server server;

	public Executor(Server server) {
		this.server = server;
	}

	public void run() {
		
		EditText link = (EditText) server.findViewById(R.id.link);
		link.setText("222");
		try {
//			ws = new MutiThreadWebServer(new File("/").toURI().toURL());
//			ws.start();
//			while (true) {
//				if (ws.getPort() > 0) {
//					break;
//				}
//				Thread.sleep(300);
//			}
//			WifiManager wm = (WifiManager) server.getSystemService(Activity.WIFI_SERVICE);
//			String ip = "127.0.0.1";
//			if (wm != null) {
//				int i = wm.getConnectionInfo().getIpAddress();
//				ip = (i >> 24) + "." + (0xFF & (i >> 16)) + '.'
//						+ (0xFF & (i >> 8)) + '.' + (0xFF & i);
//			}
//			link.setText("http://" + ip + ":" + ws.getPort());
		} catch (Throwable e) {
			String msg = e.getMessage();
			link.setText(msg.replaceAll("[\r\n]", ""));
		}
	}
}