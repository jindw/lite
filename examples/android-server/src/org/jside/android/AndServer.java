package org.jside.android;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.jside.filemanager.FileManager;
import org.jside.webserver.HttpUtil;
import org.jside.webserver.RequestContext;
import org.jside.webserver.action.ActionWebServer;
import org.jside.webserver.action.TemplateAction;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Contacts;
import android.telephony.gsm.SmsManager;
import android.util.Log;

public class AndServer extends Service {
	/** Called when the activity is first created. */
	private ActionWebServer ws;
	private static AndServer instance;
	private static URI root = URI.create("classpath:///org/jside/android/web/");
	private static TemplateAction engine = TemplateAction.create(root);

	@Override
	public IBinder onBind(Intent intent) {
		getServer();
		return new AndBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		if (ws != null) {
			ws.stop();
		}
		return super.onUnbind(intent);
	}

	private ActionWebServer getServer() {
		instance = this;
		if (ws == null) {
			java.io.File webBase = new java.io.File("/sdcard/htdocs/");
			try {
				ws = new ActionWebServer(webBase.toURL());
			} catch (MalformedURLException e) {
				Log.e("ERROR:", Log.getStackTraceString(e));
			}
			ws.addAction("/sms", SMSAction.class);
			ws.addAction("/**.xhtml", engine);
			FileManager fs = new FileManager(webBase.getParentFile(), "/fs/");
			ws.addAction("/fs/**", fs);
			ws.addAction("/**", new Object(){
				public boolean execute(){
					String path = RequestContext.get().getRequestURI();
					if(path.equals("/")){
						path = "index.html";
					}
					URI uri = engine.createURI(path, null);
					try {
						HttpUtil.printResource(engine.openInputStream(uri));
						return true;
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
				}
			});
			ws.start();
		}
		return ws;
	}

	public static class SMSAction {
		private String[] addresses;
		private String message;
		private Collection<? extends Object> addressEntries;
		private String body;

		public String getMessage() {
			return message;
		}

		public String[] getAddresses() {
			return addresses;
		}

		public Collection<? extends Object> getAddressEntries() {
			return addressEntries;
		}

		public void setAddresses(String[] address) {
			this.addresses = address;
		}

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}

		public boolean execute() throws IOException {
			Cursor users = instance.getContentResolver().query(
					Contacts.People.CONTENT_URI, null, null, null, null);
			LinkedHashMap<String, String> um = new LinkedHashMap<String, String>();
			while (users.moveToNext()) {
				String phone = users.getString(Contacts.People.TYPE_MOBILE);
				if (phone != null) {
					String name = users.getString(users
							.getColumnIndex(Contacts.People.DISPLAY_NAME));

					um.put(phone, name + "(" + phone + ")");
				}
			}
			if (addresses != null && addresses.length > 0) {
				for (String add : addresses) {
					ContentValues values = new ContentValues();
					values.put("address", add);
					values.put("body", body);
					instance.getContentResolver().insert(
							Uri.parse("content://sms/sent"), values);
					
					SmsManager smsManager = SmsManager.getDefault();

                    Intent i = new Intent("cc.androidos.smsdemo.IGNORE_ME");
                    PendingIntent dummyEvent = PendingIntent.getBroadcast(
                    		instance, 0, i, 0);
					smsManager.sendTextMessage(add, null, body, dummyEvent, dummyEvent);

				}
				message = addresses.length + "条消息已进入发送队列。";
			}
			addressEntries = um.entrySet();
			RequestContext context = RequestContext.get();

			OutputStreamWriter out = new OutputStreamWriter(context
					.getOutputStream(), context.getEncoding());
			engine.render("/sms.xhtml", this, out);
			out.close();
			return true;// "/sms.xhtml";
		}
	}

	class AndBinder extends Binder {
		public ActionWebServer getWebServer() {
			return getServer();
		}
	}

}
