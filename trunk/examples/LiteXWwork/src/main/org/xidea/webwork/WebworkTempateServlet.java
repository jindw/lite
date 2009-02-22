package org.xidea.webwork;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.TempateEngine;
import org.xidea.lite.parser.DecoratorMapper;
import org.xidea.webwork.result.ServletDispatcherResult;

import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.util.OgnlValueStack;

public class WebworkTempateServlet extends GenericServlet {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory
			.getLog(WebworkTempateServlet.class);

	private TempateEngine tempateEngine;
	public WebworkTempateServlet() {
	}

	protected Map<Object, Object> createDefaultModel(
			final HttpServletRequest req) {
		final OgnlValueStack stack = ActionContext.getContext().getValueStack();
		@SuppressWarnings("unchecked")
		Map<Object, Object> model = new HashMap<Object, Object>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public Object get(Object key) {
				if (super.containsKey(key)) {
					return super.get(key);
				} else if (key instanceof String) {
					return stack.findValue((String) key);
				}
				return super.get(key);
			}
		};
		// super.createDefaultModel(req);
		model.put("params", req.getParameterMap());
		Object uri = req
				.getAttribute(ServletDispatcherResult.WEBWORK_REQUEST_URI);
		if (uri == null) {
			uri = req.getRequestURI();
		}
		model.put("requestURI", uri);
		return model;
	}
	@Override
	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		String path = request.getServletPath();
		tempateEngine.render(path, createDefaultModel(request), resp
				.getWriter());
	}

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		try {
			final ServletContext context = config.getServletContext();
			tempateEngine = new TempateEngine() {
				{
					String decoratorPath = config
							.getInitParameter("decoratorMapping");
					if(decoratorPath == null){
						decoratorPath = DEFAULT_DECORATOR_MAPPING;
					}
					this.decoratorMapper = new DecoratorMapper(context
							.getResourceAsStream(decoratorPath));
				}
				protected URL getResource(String pagePath)
						throws MalformedURLException {
					return new File(context.getRealPath(pagePath)).toURI().toURL();
				}
			};

		} catch (Exception e) {
			log.error("装载页面装饰配置信息失败", e);
		}
	}
}
