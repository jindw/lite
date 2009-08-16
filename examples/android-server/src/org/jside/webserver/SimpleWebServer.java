package org.jside.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

public abstract class SimpleWebServer implements Runnable, WebServer {
	private static final Log log = LogFactory.getLog(SimpleWebServer.class);
	protected int defaultPort = 1981;
	private int port;
	private ServerSocket serverSocket;
	private boolean runningFlag;
	protected URL webBase;
	protected Map<String, Object> application = new HashMap<String, Object>();

	public SimpleWebServer(URL webBase) {
		this.webBase = webBase;
	}

	/* (non-Javadoc)
	 * @see org.jside.webserver.WebServer#getApplication()
	 */
	public Map<String, Object> getApplication() {
		return application;
	}

	/* (non-Javadoc)
	 * @see org.jside.webserver.WebServer#getWebBase()
	 */
	public URL getWebBase() {
		return webBase;
	}

	/* (non-Javadoc)
	 * @see org.jside.webserver.WebServer#getPort()
	 */
	public int getPort() {
		return port;
	}

	/* (non-Javadoc)
	 * @see org.jside.webserver.WebServer#start()
	 */
	public void start() {
		Thread thread = new Thread(this);
		thread.start();
	}

	/* (non-Javadoc)
	 * @see org.jside.webserver.WebServer#restart()
	 */
	public void restart() {
		try {
			if (this.serverSocket != null) {
				log.info("Webserver stoping up");
				this.serverSocket.close();
			}
		} catch (IOException e) {
			log.error("关闭socket异常", e);
		}
		log.info("Webserver starting up");
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

	/* (non-Javadoc)
	 * @see org.jside.webserver.WebServer#stop()
	 */
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
					log.debug("Connectioned, begin schedule.");
					scheduleRequest(remote);
				} else {
					log.info("restarting...");
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				log.error(e);
			}
		}

	}

	protected void scheduleRequest(Socket remote) throws Exception {
		InputStream in = remote.getInputStream();
		OutputStream out = remote.getOutputStream();
		//remote.setSoTimeout(1000 * 60);
		RequestContext.enter(this,in, out);
		processRequest();
		RequestContext.get().getOutputStream().flush();
		in.close();
		out.close();
		remote.close();
	}

	public void processRequest() throws Exception {
		RequestContext handle = RequestContext.get();
		HttpUtil.printResource(new URL(webBase, handle.getRequestURI()
				.substring(1)));
	}

}
