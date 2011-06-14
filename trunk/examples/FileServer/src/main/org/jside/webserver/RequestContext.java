package org.jside.webserver;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.Map;

/**
 * @see RequestContextImpl
 * @author test
 *
 */
public interface RequestContext {
	public WebServer getServer();

	public String getRequestURI();
	public String getRequestHeader(String key);
	
	public void setResponseHeader(String value);
	public void addResponseHeader(String value);
	
	public String getMethod();
	public String getEncoding();
	public void setEncoding(String encoding);

	public Map<String, String[]> getParams();
	public Map<String, String> getParam();
	public Map<String,Object> getContextMap();

	public void dispatch(String path);
	public void setStatus(int status, String message);


	public boolean isAccept();
	
	public OutputStream getOutputStream();
	public InputStream getInputStream();

	
	public URI getResource(String path);
	public void setMimeType(String mimeType);
	public InputStream openStream(URI path);

	public String getQuery();
	public InetAddress getRemoteAddr();



}
