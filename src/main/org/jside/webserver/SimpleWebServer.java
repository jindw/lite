package org.jside.webserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class SimpleWebServer implements WebServer {
	private static final Log log = LogFactory.getLog(SimpleWebServer.class);
	protected int defaultPort = 1981;
	private int port;
	private int state = CREATED;
	private Object stateLock = new Object();

	private ServerSocket serverSocket;

	protected URL webBase;
	protected Map<String, Object> application = new HashMap<String, Object>();

	public SimpleWebServer(URL webRoot) {
		this.webBase = webRoot;
	}
	public SimpleWebServer(String webRoot) {
		try{
			this.webBase = new URL(webRoot);
		}catch (MalformedURLException e) {
			try {
				this.webBase = new File(webRoot).toURI().toURL();
			} catch (MalformedURLException e2) {
				log.error(e2);
				throw new RuntimeException(e2);
			}
		}
	}
	
	private String encoding = "utf-8";

	public String getEncoding() {
		return encoding ;
	}
	public void setEncoding(String encoding) {
		this.encoding = encoding ;
	}
	/**
	 * @see org.jside.webserver.WebServer#getApplication()
	 */
	public Map<String, Object> getApplication() {
		return application;
	}

	/**
	 * @see org.jside.webserver.WebServer#getWebBase()
	 */
	public URL getWebBase() {
		return webBase;
	}

	/**
	 * @see org.jside.webserver.WebServer#getPort()
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @see org.jside.webserver.WebServer#start()
	 */
	public void start() {
		if (state != CREATED && state != CLOSED) {
			stop();
		}
		synchronized (stateLock) {
			state = STARTING;
			Thread thread = new RunThread();
			thread.setName("WebServer");
			resetSocket();
			thread.start();
		}

	}

	/**
	 * @see org.jside.webserver.WebServer#stop()
	 */
	public void stop() {
		synchronized (stateLock) {
			if (state == STARTING) {
				waitTo(RUNNING);
			}
			if (state == RUNNING) {
				state = STOPING;
			}
			waitTo(CLOSED);
		}

	}

	private void waitTo(int state) {
		while (true) {
			if (state >= this.state) {
				return;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				log.warn(e);
			}
		}
	}

	private void resetSocket() {
		try {
			if (this.serverSocket != null) {
				log.info("Webserver stoping ...");
				this.serverSocket.close();
			}
		} catch (IOException e) {
			log.error("关闭socket异常", e);
		}
		log.info("Webserver starting ...");
		int port = this.defaultPort;
		for (int i = 0; i < 5; i++) {
			try {
				// create the main server socket
				serverSocket = new ServerSocket(port);
				this.port = port;
				log.debug("Waiting for connection:" + port);
				break;
			} catch (java.net.BindException e) {
				port++;
			} catch (Exception e) {
				log.warn("Error: ", e);
				return;
			}
		}
	}

	private class RunThread extends Thread {

		public void run() {
			state = RUNNING;
			while (state == RUNNING) {// STOPING
				try {
					if (serverSocket.isBound() && !serverSocket.isClosed()) {
						Socket remote = serverSocket.accept();
						// remote is now the connected socket
						log.debug("Connectioned, begin schedule.");
						scheduleRequest(remote);
					} else {
						log.debug("waiting...");
						Thread.sleep(1000);
					}
				} catch (Exception e) {
					log.error(e);
				}
			}
			state = CLOSED;

		}
	}

	protected void scheduleRequest(Socket remote) throws Exception {
		InputStream in = remote.getInputStream();
		OutputStream out = remote.getOutputStream();
		// remote.setSoTimeout(1000 * 60);
		RequestContext context = RequestContext.enter(this, in, out);
		processRequest(context);
		context.getOutputStream().flush();
		in.close();
		out.close();
		remote.close();
	}

	public void processRequest(RequestContext context) throws Exception {
		HttpUtil.printResource(new URL(webBase, context.getRequestURI()
				.substring(1)));
	}

}
