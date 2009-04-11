package org.xidea.lite.servlet;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.TemplateEngine;

public class TemplateServlet extends GenericServlet {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TemplateServlet.class);

	protected TemplateEngine templateEngine;

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		templateEngine = new ServletTemplateEngine(config);
	}

	@Override
	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		String path = request.getServletPath();
		templateEngine.render(path, createModel(request), resp.getWriter());
	}

	protected Map<Object, Object> createModel(final HttpServletRequest req) {
		return new RequestMap(req);
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
			throw new UnsupportedOperationException();
		}
	}

}
