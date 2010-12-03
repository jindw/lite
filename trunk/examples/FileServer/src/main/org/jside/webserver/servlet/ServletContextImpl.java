package org.jside.webserver.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jside.webserver.WebServer;

@SuppressWarnings("unchecked")
public class ServletContextImpl extends ResponseAdaptor implements
		ServletContext, ServletConfig {

	private static final String WEB_INF_CLASSES = "/WEB-INF/classes/";
	private static final Log log = LogFactory.getLog(ServletContextImpl.class);
	public ServletContextImpl( WebServer server){
		super(server);
	}


	public ServletContext getContext(String arg0) {
		return this;
	}


	public String getInitParameter(String arg0) {
		return null;
	}

	public Enumeration getInitParameterNames() {
		return Collections.enumeration(Collections.emptyList());
	}

	public int getMajorVersion() {
		return 2;
	}

	public String getMimeType(String arg0) {
		return null;
	}

	public int getMinorVersion() {
		return 5;
	}

	public RequestDispatcher getNamedDispatcher(String arg0) {
		return null;
	}

	private File toFile(URL res) {
		try {
			File file = new File(res.toURI());
			if (file != null && file.exists()) {
				return file;
			}
		} catch (Exception e) {
		}
		return null;
	}

	public String getRealPath(String path) {
		try {
			URL res = getResource(path);
			File file = toFile(res);
			if (file != null && file.exists()) {
				return file.getCanonicalFile().getAbsolutePath();
			}
			return res.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public URL getResource(String path) throws MalformedURLException {
		URL base = server.getWebBase().toURL();
		if (path.startsWith("/")) {
			path = path.substring(1);
		} else {
			String uri = base().getRequestURI();
			if (uri.length() > 1) {
				base = new URL(base, uri.substring(1));
			}
		}
		if (path.length() > 0) {
			base = new URL(base, path);
		}
		if (path.startsWith(WEB_INF_CLASSES)
				&& base.getProtocol().equals("file")) {
			File file = toFile(base);
			if (!file.exists()) {
				base = this.getClass().getResource(
						path.substring(WEB_INF_CLASSES.length() - 1));
			}
		}
		return base;
	}

	public InputStream getResourceAsStream(String path) {
		try {
			return getResource(path).openStream();
		} catch (Exception e) {
			return null;
		}
	}

	public Set getResourcePaths(String arg0) {
		return null;
	}

	public String getServerInfo() {
		return "localhost";
	}

	public Servlet getServlet(String arg0) throws ServletException {
		return null;
	}

	public String getServletContextName() {
		return "/";
	}

	public Enumeration getServletNames() {
		return Collections.enumeration(Collections.emptyList());
	}

	public Enumeration getServlets() {
		return Collections.enumeration(Collections.emptyList());
	}

	public void log(String msg) {
		log.error(msg);
	}

	public void log(Exception e, String msg) {
		log.info(msg, e);
	}

	public void log(String msg, Throwable e) {
		log.info(msg, e);

	}

	public ServletContext getServletContext() {
		return this;
	}

	public String getServletName() {
		return "/";
	}

}
