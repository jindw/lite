package org.xidea.lite.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.Template;
import org.xidea.lite.TemplateEngine;

public class TemplateServlet extends GenericServlet {
	/**
	 * refresh model model,url source
	 */
	private static final String LITE_DEBUG = "LITE_DEBUG";
	private static final String LOCAL_IP = "^(?:127\\.0\\.0\\.1|10\\..+|172\\.(?:1[6789]|2.|30|31)\\..+|192\\.168\\..+|([0:]+1))$";
	
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(TemplateServlet.class);

	protected TemplateEngine templateEngine;
	private Pattern debug = null;

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext context = config.getServletContext();
		String configPath = config.getInitParameter("config");
		if (configPath == null) {
			configPath = "/WEB-INF/lite.xml";
		}
		String debugModel = config.getInitParameter("debug");
		if (debugModel == null) {
			this.debug = Pattern.compile(LOCAL_IP);
		}else{
			String model = debugModel.trim();
			if("true".equalsIgnoreCase(model)){
				this.debug = Pattern.compile(".");
			}else if("false".equalsIgnoreCase(model)){
				this.debug = null;
			}else{
				this.debug = Pattern.compile(model);
			}
		}
		final File root = new File(context.getRealPath("/"));
		final File litecode = new File(root, "WEB-INF/litecode/");
		try {
			final File configFile = new File(context.getRealPath(configPath));
			Class<?> configClass = Class
					.forName("org.xidea.lite.impl.ParseConfigImpl");
			Class<? extends Object> engineClass = Class
					.forName("org.xidea.lite.impl.HotTemplateEngine");
			Object parseConfig = configClass.getConstructor(URI.class,
					URI.class).newInstance(root.toURI(), configFile.toURI());
			templateEngine = (TemplateEngine) engineClass.getConstructor(
					Class.forName("org.xidea.lite.parse.ParseConfig"), URI.class).newInstance(parseConfig,
					litecode.toURI());
			log.info("Lite HotTemplateEngine(runtime and compiler) is used");
		} catch (ClassNotFoundException e) {
			templateEngine = new TemplateEngine(litecode.toURI());
			log.info("Lite TemplateEngine(runtime only) is used");
		} catch (Exception e) {
			log.error("Lite HotTemplateEngine init faild!", e);
			templateEngine = new TemplateEngine(litecode.toURI());
		}
	}

	@Override
	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		String path = request.getServletPath();
		if (debug != null && debug.matcher(request.getRemoteAddr()).find()) {
			for (Cookie cookie : request.getCookies()) {
				if (LITE_DEBUG.equals(cookie.getName())) {
					String value = cookie.getValue();
					if ("refresh".equals(value)) {
						templateEngine.clear(path);
					} else if ("source".equals(value)) {
						File f = new File(this.getServletContext().getRealPath(
								path));
						resp.setContentType("text/plain");
						ServletOutputStream out = resp.getOutputStream();
						FileInputStream in = new FileInputStream(f);
						byte[] buf = new byte[64];
						for (int c = in.read(buf); c >= 0;) {
							out.write(buf, 0, c);
						}
						in.close();
						out.flush();
						return;
					} else if ("model".equals(value)) {
						// 只支持数据预览，不支持数据修改
						// log.warn("该版本尚不支持数据模型展现");
						// templateEngine.clear(path);
					}
				}
			}
		}

		Template template = templateEngine.getTemplate(path);
		String contentType = template.getFeature(Template.FEATURE_CONTENT_TYPE);
		resp.setContentType(contentType);
		template.render(createModel(request), resp.getWriter());
	}

	protected Map<Object, Object> createModel(final HttpServletRequest req) {
		return new RequestSessionMap(req);
	}
}
