package org.jside.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MutiThreadWebServer extends SimpleWebServer {
	private static final Log log = LogFactory.getLog(MutiThreadWebServer.class);
	private List<RequestThread> requestThreads = new ArrayList<RequestThread>();
	private Object taskNotifier = new Object();
	private List<Socket> taskList = Collections
			.synchronizedList(new ArrayList<Socket>());

	public MutiThreadWebServer(URI webRoot) {
		super(webRoot);
	}

	@Override
	public void start() {
		start(20);
	}

	public void start(int threadCount) {
		for (int i = 0; i < threadCount; i++) {
			RequestThread thread = new RequestThread();
			thread.start();
			requestThreads.add(thread);
		}
		super.start();
	}

	@Override
	protected void scheduleRequest(final Socket remote) {
		taskList.add(remote);
		synchronized (taskNotifier) {
			taskNotifier.notify();
		}
	}

	private void processRequest(final Socket remote){
		try {
			InputStream in = remote.getInputStream();
			OutputStream out = remote.getOutputStream();
			RequestContext context = RequestContext.enter(createRequestContext(this, in, out));
			try {
				processRequest(context);
			} catch (Exception e) {
				log.debug(context.getRequestURI() +":",e);
				//RequestUtil.printResource(e, "text");
			}
			context.getOutputStream().flush();
			in.close();
			out.close();
		} catch (Exception e) {
			log.error(e);
		} finally {
			try {
				if(log.isDebugEnabled()){
					log.debug("complete:"+RequestContext.get().getRequestURI()+"\n"+remote);
				}
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
