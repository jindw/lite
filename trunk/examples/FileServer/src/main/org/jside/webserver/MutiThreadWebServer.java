package org.jside.webserver;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MutiThreadWebServer extends SimpleWebServer {
	private static final Log log = LogFactory.getLog(MutiThreadWebServer.class);
	private List<RequestThread> requestThreads = new ArrayList<RequestThread>();
	private Object taskNotifier = new Object();
	private List<Socket> taskList = new ArrayList<Socket>();
	private long timeout = 1000 * 60 * 10; //默认十分钟上线，超时忽略
	private int alive = 0;
	private boolean started = false;

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public MutiThreadWebServer(URI webRoot) {
		super(webRoot);
	}

	@Override
	public void start() {
		start(20);
	}

	public void start(int threadCount) {
		for (int i = 0; i < threadCount; i++) {
			RequestThread thread = new RequestThread("Wait Request:" + requestThreads.size());
			thread.start();
			requestThreads.add(thread);
		}
		if(!started){
			started = true;
			super.start();
		}
	}

	@Override
	protected void scheduleRequest(final Socket remote) {
		synchronized (taskNotifier) {
			if (alive <1) {
				for (RequestThread t : requestThreads) {
					if (t.start > 0
							&& t.start - System.currentTimeMillis() > timeout){
						//超时关闭
						log.warn("WEB线程超时关闭："+t);
						t.running = false;
					}
				}
				log.info("WEB 线程池耗尽，启用新线程:"+requestThreads.size());
				start(1);
			}else {
				if(log.isDebugEnabled()){
					if(taskList.size()>0){
						log.debug("历史任务未处理："+taskList);
					}
				}
				
			}
			taskList.add(remote);
			taskNotifier.notify();
		}
	}

	private void processRequest(final Socket remote) {
		RequestContext context = null;
		try {
			context = RequestUtil.enter(this, remote);
//			RequestThread t = (RequestThread) Thread.currentThread();
//			String n = t.url;
			try {
				//t.setName("Running Request:" + context.getRequestURI());
				processRequest(context);
			} catch (java.net.SocketException e) {
				log.debug(context.getRequestURI() + ":", e);
			} catch (Exception e) {
				log.info(context.getRequestURI() + ":", e);
			} finally {
				RequestUtil.exit();
				//t.setName(n);
			}
		} catch (Exception e) {
			// e.printStackTrace();
			log.debug(e);
		} finally {
			try {
				if (log.isDebugEnabled()) {
					log.debug("complete:" + context.getRequestURI()
							+ "\n" + remote);
				}
				remote.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private Socket offerTask() {
		synchronized (taskNotifier) {
			if (taskList.size() > 0) {
				return taskList.remove(0);
			}
		}
		return null;
	}

	class RequestThread extends Thread {
		private boolean running = true;

		private long start;

		public RequestThread(String name) {
			super(name);
		}

		public void run() {
			try {
				alive++;
				while (this.running) {
					Socket remote = offerTask();
					if (remote != null) {
						alive--;
						try {
							start = System.currentTimeMillis();
							processRequest(remote);
						} finally {
							alive++;
						}
					}
					start = 0;
					synchronized (taskNotifier) {
						if (taskList.isEmpty()) {
							try {
								taskNotifier.wait();
							} catch (InterruptedException e) {
								log.warn(e);
							}
						}
					}
				}
				// this.running = false;
			} finally {
				alive--;
			}
		}

	}

}
