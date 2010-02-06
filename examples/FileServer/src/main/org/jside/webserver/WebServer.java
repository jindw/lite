package org.jside.webserver;

import java.net.URI;
import java.util.Map;

public interface WebServer {

	public Map<String, Object> getApplication();

	public URI getWebBase();

	public String getEncoding();

	public int getPort();

	public void start();

	public void stop();

	public void processRequest(RequestContext context) throws Exception ;

}