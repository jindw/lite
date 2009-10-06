package org.jside.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

public abstract class RequestContext {
	private static ThreadLocal<RequestContext> tl = new ThreadLocal<RequestContext>();

	public static RequestContext enter(WebServer server, InputStream in, OutputStream out) {
		RequestContext context = new RequestContextImpl(server, in, out);
		tl.set(context);
		return context;
	}

	public static RequestContext get() {
		return tl.get();
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

	public abstract OutputStream getOutputStream();
	public void println(String line){
		try {
			getOutputStream().write(line.getBytes(getEncoding()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public abstract URL getResource(String path);

	public abstract Object[] getValueStack();

	public abstract void push(Object value);

	public abstract Object pop();

	public abstract void setContentType(String contentType);

	public abstract String getQuery();

	public abstract boolean isAccept();

}
