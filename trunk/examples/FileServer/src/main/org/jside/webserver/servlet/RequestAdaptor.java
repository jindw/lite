package org.jside.webserver.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jside.webserver.RequestContextImpl;
import org.jside.webserver.RequestUtil;
import org.jside.webserver.WebServer;

@SuppressWarnings("unchecked")
abstract class RequestAdaptor implements HttpServletRequest {
	final WebServer server;
	public RequestAdaptor(WebServer server) {
		this.server = server;
	}

	public String getAuthType() {
		return null;
	}

	public String getContextPath() {
		return "";
	}

	public Cookie[] getCookies() {
		String cookie = base().getRequestHeader("Cookie");
		if (cookie != null) {
			String[] cookies = cookie.split("\\s*[;]\\s*");
			int p = cookies.length;
			Cookie[] results = new Cookie[p];
			if (cookies != null && p > 0) {
				while (p-- > 0) {
					cookie = cookies[p];
					int s = cookie.indexOf('=');
					if (s >= 0) {
						results[p] = new Cookie(cookie.substring(0, s), cookie
								.substring(s + 1));
					} else {
						results[p] = new Cookie(cookie, "");
					}
				}
			}
			return results;
		}
		return new Cookie[0];
	}

	private static SimpleDateFormat formats[] = {
			new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
			new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
			new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) };

	public long getDateHeader(String key) {
		String value = getHeader(key);
		if (value == null) {
			return -1;
		}
		for (SimpleDateFormat df : formats) {
			synchronized (df) {
				try {
					return df.parse(value).getTime();
				} catch (Exception e) {
				}
			}
		}
		throw new IllegalArgumentException(value);

	}

	protected RequestContextImpl base() {
		return (RequestContextImpl) RequestUtil.get();
	}

	public String getHeader(String key) {
		return base().getRequestHeader(key);
	}

	public Enumeration getHeaderNames() {
		List<String> headers = base().getRequestHeaders();
		List<String> keys = new ArrayList<String>();
		for (String key : headers) {
			keys.add(key.substring(0, key.indexOf(':')));
		}
		return Collections.enumeration(keys);
	}

	public Enumeration getHeaders(String key) {
		return Collections.enumeration(Arrays.asList(getHeader(key)));
	}

	public int getIntHeader(String key) {
		return Integer.parseInt(getHeader(key));
	}

	public String getMethod() {
		return base().getMethod();
	}

	public String getPathInfo() {
		//TODO:...
		return "";
	}

	public String getPathTranslated() {
		//TODO:....
		return base().getRequestURI();
	}

	public String getQueryString() {
		return base().getQuery();
	}

	public String getRemoteUser() {
		//TODO:...
		return "test";
	}

	public String getRequestURI() {
		return base().getRequestURI();
	}

	public StringBuffer getRequestURL() {
		return new StringBuffer(base().getRequestURI());
	}

	public String getRequestedSessionId() {
		return null;
	}

	public String getServletPath() {
		return this.getRequestURI();
	}

	public HttpSession getSession() {
		return null;
	}

	public HttpSession getSession(boolean arg0) {
		return null;
	}

	public Principal getUserPrincipal() {
		return null;
	}

	public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	public boolean isRequestedSessionIdValid() {
		return false;
	}

	public boolean isUserInRole(String arg0) {
		return false;
	}

	public Enumeration getAttributeNames() {
		return Collections.enumeration(Collections.emptyList());
	}

	public String getCharacterEncoding() {
		return RequestUtil.get().getEncoding();
	}

	public int getContentLength() {
		return 0;
	}

	public String getContentType() {
		String ct = RequestUtil.get().getRequestHeader("Content-Type");
		return ct == null?"text/html":ct;
	}

	public ServletInputStream getInputStream() throws IOException {
		RequestContextImpl rc = (RequestContextImpl) RequestUtil.get();
		
		final byte[] data= rc.getPost().getBytes("ISO-8859-1");
		return new ServletInputStream(){
			int pos = 0;
			@Override
			public int read() throws IOException {
				return pos<data.length?data[pos++]:-1;
			}
			
		};
	}

	public String getLocalAddr() {
		return "127.0.0.1";
	}

	public String getLocalName() {
		return "localhost";
	}

	public int getLocalPort() {
		return 0;
	}

	public Locale getLocale() {
		return null;
	}

	public Enumeration getLocales() {
		return null;
	}

	public String getParameter(String arg0) {
		return null;
	}

	public Map getParameterMap() {
		return base().getParams();
	}

	public Enumeration getParameterNames() {
		return Collections.enumeration(base().getParam().keySet());
	}

	public String[] getParameterValues(String key) {
		return base().getParams().get(key);
	}

	public String getProtocol() {
		return null;
	}

	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new StringReader(base().getPost()));
	}

	public abstract String getRealPath(String path);

	public String getRemoteAddr() {
		return "127.0.0.1";
	}

	public String getRemoteHost() {
		return "localhost";
	}

	public int getRemotePort() {
		return 0;
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		return null;
	}

	public String getScheme() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServerName() {
		return "localhost";
	}

	public int getServerPort() {
		return 0;
	}

	public boolean isSecure() {
		return false;
	}

	public void removeAttribute(String key) {
		base().getContextMap().remove(key);
	}

	public void setAttribute(String key, Object value) {
		base().getContextMap().put(key,value);
	}
	public Object getAttribute(String key) {
		Object path = base().getContextMap().get(key);
		if (path == null && "javax.servlet.include.servlet_path".equals(key)) {
			return base().getRequestURI();
		}
		return path;
	}
	public void setCharacterEncoding(String encoding)
			throws UnsupportedEncodingException {
		base().setEncoding(encoding);
	}

}
