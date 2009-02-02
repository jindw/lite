package org.xidea.lite.test.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWebServer implements Runnable {
	private static Map<String, String> contentTypeMap = new HashMap<String, String>();
	static {
		// contentTypeMap.put("uu", "application/octet-stream");
		// contentTypeMap.put("exe", "application/octet-stream");
		contentTypeMap.put("css", "text/css");
		contentTypeMap.put("gif", "image/gif");
		contentTypeMap.put("jpg", "image/jpeg");
		contentTypeMap.put("jpeg", "image/jpeg");
		contentTypeMap.put("htm", "text/html");
		contentTypeMap.put("html", "text/html");
		contentTypeMap.put("xhtml", "text/html");
		contentTypeMap.put("zip", "application/zip");
		contentTypeMap.put("sh", "application/x-shar");
		contentTypeMap.put("tar", "application/x-tar");
		contentTypeMap.put("snd", "audio/basic");
		contentTypeMap.put("au", "audio/basic");
		contentTypeMap.put("wav", "audio/x-wav");
		contentTypeMap.put("text", "text/plain");
		contentTypeMap.put("c", "text/plain");
		contentTypeMap.put("cc", "text/plain");
		contentTypeMap.put("c++", "text/plain");
		contentTypeMap.put("h", "text/plain");
		contentTypeMap.put("pl", "text/plain");
		contentTypeMap.put("txt", "text/plain");
		contentTypeMap.put("java", "text/plain");
		
	}

	private int defaultPort = 88;
	private int port;
	private ServerSocket serverSocket;
	private boolean runningFlag;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jside.webserver.RequestHandle#getContentType(java.lang.String,
	 *      java.lang.String)
	 */
	public String getContentType(String name) {
		String contentType = (String) contentTypeMap.get(name);
		return contentType == null ? "unknown/unknown" : contentType;
	}

	public int getPort() {
		return port;
	}

	public void restart() {
		try {
			if (this.serverSocket != null) {
				System.out.println("Webserver stoping up");
				this.serverSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Webserver starting up");
		int port = this.defaultPort+1000;
		for (int i = 0; i < 5; i++) {
			try {
				// create the main server socket
				serverSocket = new ServerSocket(port);
				this.port = port;
				System.out.println("Waiting for connection:" + port);
				break;
			} catch (java.net.BindException e) {
				port++;
			} catch (Exception e) {
				System.out.println("Error: " + e);
				return;
			}
		}
	}

	public void start() {
		Thread thread = new Thread(this);
		thread.start();
	}

	public void stop() {
		this.runningFlag = false;
	}

	public void run() {
		this.runningFlag = true;
		restart();
		while (runningFlag) {
			try {
				if (serverSocket.isBound() && !serverSocket.isClosed()) {
					Socket remote = serverSocket.accept();
					// remote is now the connected socket
					System.out.println("Connection, sending data.");
					scheduleRequest(remote);
				} else {
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	protected void scheduleRequest(Socket remote) throws IOException {
		InputStream in = remote.getInputStream();
		OutputStream out = remote.getOutputStream();
		processRequest(new RequestHandle(this, in, out));
		in.close();
		out.close();
		remote.close();
	}

	protected abstract void processRequest(RequestHandle handle) throws IOException;

}
