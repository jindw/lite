package org.xidea.lite.tools.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public abstract class MutiThreadWebServer extends AbstractWebServer {
	private RequestThread[] requestThreads;
	private int requestLastIndex = 3;

	public void start() {
		requestThreads = new RequestThread[requestLastIndex + 1];
		for (int i = 0; i < requestThreads.length; i++) {
			requestThreads[i] = new RequestThread();
			requestThreads[i].start();
		}
		super.start();
	}

	protected void scheduleRequest(final Socket remote) {

		final RequestThread thread;
		if (requestLastIndex >= 0) {
			thread = this.requestThreads[requestLastIndex];
			requestLastIndex--;
		} else {
			thread = null;
		}
		Runnable task = new Runnable() {
			public void run() {
				try {
					InputStream in = remote.getInputStream();
					OutputStream out = remote.getOutputStream();
					try {
						processRequest(new RequestHandle(
								MutiThreadWebServer.this, in, out));
					} finally {
						in.close();
						out.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						remote.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (thread != null) {
						requestThreads[++requestLastIndex] = thread;
					}
				}

			}
		};
		if (requestLastIndex >= 0) {
			thread.addTask(task);
		} else {
			// TODO������զ���أ�
			task.run();
		}

	}

	static class RequestThread extends Thread {
		private Runnable task;
		private Object lock = new Object();

		public void addTask(Runnable task) {
			synchronized (lock) {
				if (this.task == null) {
					this.task = task;
					lock.notify();
				} else {
					throw new RuntimeException();
				}
			}
		}

		public void run() {
			while (true) {
				if (this.task != null) {
					try{
					this.task.run();
					}catch (Exception e) {
					}
				}
				synchronized (lock) {
					this.task = null;
					try {
						lock.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

	}
}
