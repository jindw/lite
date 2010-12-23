package org.xidea.lite.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.impl.HotTemplateEngine;
import org.xidea.lite.impl.ParseConfigImpl;

public class TemplateServlet extends GenericServlet {
	/**
	 * refresh
	 * model  model,url
	 * source
	 */
	private static final String LITE_DEBUG = "LITE_DEBUG";
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(TemplateServlet.class);

	protected TemplateEngine templateEngine;
	protected String contentType;
	private boolean autocompile = true;
	private boolean debugModel = false;

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		contentType = config.getInitParameter("contentType");
		String autocompile = config.getInitParameter("autocompile");
		if (autocompile != null) {
			this.autocompile = Pattern.matches("true|on|1", autocompile.toLowerCase());
		}

		ServletContext context = config.getServletContext();
		String configPath = config.getInitParameter("config");
		if (configPath == null) {
			configPath = "/WEB-INF/lite.xml";
		}
		final File root = new File(context.getRealPath("/"));
		final File litecode = new File(root,"WEB-INF/litecode/.");
		if (this.autocompile) {
			final File configFile = new File(context.getRealPath(configPath));
			ParseConfigImpl parseConfig = new ParseConfigImpl(root.toURI(),
					configFile.toURI());
			templateEngine = new HotTemplateEngine(parseConfig);
			((HotTemplateEngine)templateEngine).setCompiledBase(litecode.toURI());
		} else {
			if(debugModel){
				templateEngine = new HotTemplateEngine(litecode.toURI());
			}else{
				templateEngine = new TemplateEngine(litecode.toURI());
			}
		}
	}

	@Override
	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		String path = request.getServletPath();
		if (contentType != null) {
			resp.setContentType(contentType);
		}
		if(debugModel){
			for(Cookie cookie : request.getCookies()){
				if(LITE_DEBUG.equals(cookie.getName())){
					String value = cookie.getValue();
					if("refresh".equals(value)){
						templateEngine.clear(path);
					}else if("source".equals(value)){
						File f = new File(this.getServletContext().getRealPath(path));
						resp.setContentType("text/plain");
						ServletOutputStream out = resp.getOutputStream();
						FileInputStream in = new FileInputStream(f);
						byte[] buf = new byte[64];
						for(int c=in.read(buf);c>=0 ;){
							out.write(buf,0,c);
						}
						in.close();
						out.flush();
						return ;
					}else if("model".equals(value)){
						log.warn("该版本尚不支持数据模型展现");
						//templateEngine.clear(path);
					}
				}
			}
		}
		templateEngine.render(path, createModel(request), resp.getWriter());
	}

	protected Map<Object, Object> createModel(final HttpServletRequest req) {
		return new RequestSessionMap(req);
	}
}
