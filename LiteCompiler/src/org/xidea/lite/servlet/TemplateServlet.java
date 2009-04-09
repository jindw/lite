package org.xidea.lite.servlet;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parser.DecoratorMapper;
import org.xidea.lite.parser.XMLParser;

public class TemplateServlet extends GenericServlet {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TemplateServlet.class);

	private TemplateEngine templateEngine;

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		String transformerFactory = config.getInitParameter("transformerFactory");
		String xpathFactory = config.getInitParameter("xpathFactory");
		XMLParser parser = new XMLParser(transformerFactory,xpathFactory);
		templateEngine = new ServletTemplateEngine(parser,config.getServletContext());
	}

	@Override
	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		req.setCharacterEncoding("utf-8");
		resp.setCharacterEncoding("utf-8");
		HttpServletRequest request = (HttpServletRequest) req;
		String path = request.getServletPath();
		templateEngine.render(path, createModel(request), resp.getWriter());
	}

	protected Map<Object, Object> createModel(final HttpServletRequest req) {
		return new RequestMap(req);
	}

	static class ServletTemplateEngine extends TemplateEngine {
		private ServletContext context;

		public ServletTemplateEngine(XMLParser parser,ServletContext context) {
			this.parser = parser;
			this.context = context;
			try {
				String decoratorPath = context
						.getInitParameter("decoratorMapping");
				if (decoratorPath == null) {
					decoratorPath = DEFAULT_DECORATOR_MAPPING;
				}
				this.decoratorMapper = new DecoratorMapper(context
						.getResourceAsStream(decoratorPath));
			} catch (Exception e) {
				log.error("装载页面装饰配置信息失败", e);
			}
		}

		@Override
		protected URL getResource(String path) throws MalformedURLException {
			return new File(context.getRealPath(path)).toURI().toURL();
		}
	}

	static class RequestMap extends AbstractMap<Object, Object> {
		private static final long serialVersionUID = 1L;
		private final HttpServletRequest request;

		public RequestMap(final HttpServletRequest request) {
			this.request = request;
		}

		@Override
		public Object put(Object key,Object value) {
			request.setAttribute((String) key,value);
			return null;
		}
		@Override
		public Object get(Object key) {
			return request.getAttribute((String) key);
		}

		@Override
		public boolean containsKey(Object key) {
			return key instanceof String
					&& request.getAttribute((String) key) != null;
		}

		@Override
		public Set<java.util.Map.Entry<Object, Object>> entrySet() {
			return Collections.emptySet();
		}
	}

}
