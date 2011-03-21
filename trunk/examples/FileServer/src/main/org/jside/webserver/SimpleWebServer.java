package org.jside.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class SimpleWebServer implements WebServer {

	private enum Status{
		CREATED,
		STARTING,
		RUNNING,
		STOPING,
		CLOSED
	}
	
	private static final Log log = LogFactory.getLog(SimpleWebServer.class);
	protected int defaultPort = 80;
	private int port;
	private Status state = Status.CREATED;
	private Object stateLock = new Object();

	private ServerSocket serverSocket;

	protected URI webBase;
	protected Map<String, Object> application = new HashMap<String, Object>();

	public SimpleWebServer(URI webRoot) {
		this.webBase = webRoot;
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
	public URI getWebBase() {
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
		if (state != Status.CREATED && state != Status.CLOSED) {
			stop();
		}
		synchronized (stateLock) {
			state = Status.STARTING;
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
			waitWhen(Status.STARTING);
			state = Status.STOPING;
			waitWhen(Status.STOPING);
		}
	}

	private void waitWhen(Status state) {
		while (true) {
			if (state != this.state) {
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
		log.debug("Webserver starting ...");
		int port = this.defaultPort;
		for (int i = 0; i < 5; i++) {
			try {
				// create the main server socket
				serverSocket = new ServerSocket(port);
				this.port = port;
				log.info("Webserver started on port:"+port);
				log.debug("Waiting for connection:" + port);
				break;
			} catch (java.net.SocketException e) {
				port++;
				if(i ==0){
					port+=1900;
				}
			} catch (Exception e) {
				log.warn("Error: ", e);
				return;
			}
		}
	}

	private class RunThread extends Thread {

		public void run() {
			state = Status.RUNNING;
			while (state == Status.RUNNING) {// STOPING
				try {
					if (serverSocket.isBound() && !serverSocket.isClosed()) {
						Socket remote = serverSocket.accept();
						remote.setSoTimeout(1000 * 60 * 2);
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
			state = Status.CLOSED;

		}
	}

	protected void scheduleRequest(Socket remote) throws Exception {
		// remote.setSoTimeout(1000 * 60);
		RequestContext context = RequestUtil.enter(this, remote);
		processRequest(context);
		RequestUtil.exit();
	}

	public void processRequest(RequestContext context) throws Exception {
		RequestUtil.printResource();
	}
}
