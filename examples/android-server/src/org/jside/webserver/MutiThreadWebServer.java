package org.jside.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;

public class MutiThreadWebServer extends SimpleWebServer {
	private static final Log log = LogFactory.getLog(MutiThreadWebServer.class);
	private List<RequestThread> requestThreads = new ArrayList<RequestThread>();
	private Object taskNotifier = new Object();
	private List<Socket> taskList = Collections
			.synchronizedList(new ArrayList<Socket>());

	int inc;
	public MutiThreadWebServer(URL webRoot) {
		super(webRoot);
	}

	public void start() {
		start(10);
	}

	public void start(int threadCount) {
		for (int i = 0; i < threadCount; i++) {
			RequestThread thread = new RequestThread();
			thread.start();
			requestThreads.add(thread);
		}
		super.start();
	}

	protected void scheduleRequest(final Socket remote) {
		inc++;
		log.info("process:"+remote);
		taskList.add(remote);
		log.debug("new request:"+remote);
		synchronized (taskNotifier) {
			taskNotifier.notify();
		}
	}

	protected void processRequest(final Socket remote){
		try {
			log.debug("accept request:"+remote);
			InputStream in = remote.getInputStream();
			OutputStream out = remote.getOutputStream();
			try {
				RequestContext.enter(this, in, out);
				processRequest();
			} catch (Exception e) {
				e.printStackTrace();
				log.warn(e);
				HttpUtil.printResource(e, "text");
			}
			RequestContext context = RequestContext.get();
			context.getOutputStream().flush();
			in.close();
			out.close();
		} catch (Exception e) {
			log.error(e);
		} finally {
			try {
				log.info("inc:"+inc);
				log.info("complete:"+RequestContext.get().getRequestURI()+"\n"+remote);
				remote.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private synchronized Socket offerTask() {
		if (taskList.size() > 0) {
			return taskList.remove(0);
		}
		return null;
	}

	class RequestThread extends Thread {
		private boolean running = true;

		public RequestThread() {
		}

		public void run() {
			while (this.running) {
				Socket remote = offerTask();
				if (remote != null) {
					inc--;
					processRequest(remote);
				}
				if (taskList.isEmpty()) {
					try {
						synchronized (taskNotifier) {
							taskNotifier.wait();
						}
					} catch (InterruptedException e) {
						log.warn(e);
					}
				}
			}
			this.running = false;
		}

	}
}
