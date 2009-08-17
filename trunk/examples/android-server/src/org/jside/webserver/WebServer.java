package org.jside.webserver;

import java.net.URL;
import java.util.Map;

public interface WebServer {
	public int CREATED = 0;
	public int STARTING = 1;
	public int RUNNING = 2;
	public int STOPING = 3;
	public int CLOSED = 4;

	public Map<String, Object> getApplication();

	public URL getWebBase();

	public int getPort();

	public void start();

	public void stop();

	public void processRequest(RequestContext context) throws Exception ;

}