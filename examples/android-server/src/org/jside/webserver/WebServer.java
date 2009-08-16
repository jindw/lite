package org.jside.webserver;

import java.net.URL;
import java.util.Map;

public interface WebServer {

	public Map<String, Object> getApplication();

	public URL getWebBase();

	public int getPort();

	public void start();

	public void restart();

	public void stop();

	public void processRequest() throws Exception ;

}