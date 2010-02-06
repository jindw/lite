package org.jside.webserver;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

public abstract class RequestContext {
	static ThreadLocal<RequestContext> holder = new ThreadLocal<RequestContext>();

	public static RequestContext enter(RequestContext context) {
		holder.set(context);
		return context;
	}

	public static RequestContext get() {
		return holder.get();
	}
	public abstract Map<String, Object> getApplication();

	public abstract String getRequestURI();

	public abstract String getMethod();

	public abstract String getEncoding();

	public abstract void setEncoding(String encoding);

	public abstract Map<String, String[]> getParams();

	public abstract Map<String, String> getParam();

	public abstract List<String> getHeaders();

	public abstract String findHeader(String key);

	public abstract void dispatch(String result);

	public abstract void setHeader(String value);

	public abstract void addHeader(String value);

	public abstract void setStatus(int status, String message);

	public abstract void setContentType(String contentType);

	public abstract boolean isAccept();
	
	public abstract OutputStream getOutputStream();

	public abstract Object[] getValueStack();

	public abstract void push(Object value);

	public abstract Object pop();

	
	public abstract URI getResource(String path);

	public abstract InputStream openStream(URI path);

}
