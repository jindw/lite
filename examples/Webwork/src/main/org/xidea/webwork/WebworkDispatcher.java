package org.xidea.webwork;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WebworkDispatcher implements Servlet, Filter {
	private static final long serialVersionUID = 1143707809140387749L;

	private static final Log log = LogFactory.getLog(WebworkDispatcher.class);

	protected WebworkContext webworkContext;

	private ServletConfig servletConfig;

	protected String getAction(String servletPath) {
		int split0 = servletPath.lastIndexOf('/');
		int split1 = servletPath.indexOf('.', split0);
		if (split1 > 0) {
			return servletPath.substring(split0 + 1, split1);
		} else {
			return "index";
		}
	}

	protected String getNamespace(String servletPath, String action) {
		int split0 = servletPath.lastIndexOf("/");
		int split1 = servletPath.indexOf('.', split0);
		String perfix = null;
		if (split1 > 0) {
			perfix = servletPath.substring(0, split0);
		} else {
			perfix = servletPath;
		}
		return acceptNamespace(perfix, action);
	}

	protected String acceptNamespace(String perfix, String action) {
		String namespace = webworkContext.getNamespace(action, perfix);
		if (perfix.equals(namespace)) {
			return namespace;
		} else {
			if (log.isDebugEnabled()) {
				log.debug("无匹配命名空：perfix=" + perfix + ";action=" + action);
			}
			return null;
		}
	}

	/**
	 * @see com.opensymphony.webwork.dispatcher.ServletDispatcher#service(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		invoke(request, (HttpServletResponse) servletResponse);
	}

	public void service(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		this.invoke((HttpServletRequest) request,
				(HttpServletResponse) response);
	}

	public void invoke(HttpServletRequest request, HttpServletResponse response)
			throws ServletException {
		if (log.isDebugEnabled()) {
			log.debug("初始化Action入口");
		}
		// request = new VS2AttriRequestWrapper(request);
		String servletPath = (String) request
				.getAttribute("javax.servlet.include.servlet_path");
		if (servletPath == null) {
			servletPath = request.getServletPath();
		}
		String action = getAction(servletPath);
		String namespace = getNamespace(servletPath, action);
		if (namespace != null) {
			if (log.isDebugEnabled()) {
				log.debug("执行Action:path=" + servletPath + ";namespace="
						+ namespace + ";action=" + action);
			}
			try {
				webworkContext.execute(namespace, action, request,
						(HttpServletResponse) response);
			} catch (Exception e) {
				if (log.isErrorEnabled()) {
					log.error("执行Action失败:" + servletPath, e);
				}
			}
		} else {
			log.error("找不到指定Action:" + servletPath);
		}
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		if (this.webworkContext == null) {
			WebworkContext webworkContext = new WebworkContext();
			webworkContext.initialize(filterConfig.getServletContext());
			this.webworkContext = webworkContext;
		}
	}

	public void destroy() {
	}

	/**
	 * @param webworkContext
	 *            The webworkContext to set.
	 * @spring.property ref="webworkContext"
	 */
	public void setWebworkContext(WebworkContext webworkContext) {
		this.webworkContext = webworkContext;
	}

	public void init(ServletConfig servletConfig) throws ServletException {
		this.servletConfig = servletConfig;
		if (this.webworkContext == null) {
			WebworkContext webworkContext = new WebworkContext();
			webworkContext.initialize(servletConfig.getServletContext());
			this.webworkContext = webworkContext;
		}
	}

	public ServletConfig getServletConfig() {
		return servletConfig;
	}

	public String getServletInfo() {
		return "Webwork Dispatcher name="
				+ (servletConfig == null ? null : servletConfig
						.getServletName());
	}

}
