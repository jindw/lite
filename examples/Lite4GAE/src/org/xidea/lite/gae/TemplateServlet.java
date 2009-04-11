package org.xidea.lite.gae;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.ParseContextImpl;

/**
 * 因为GAE不支持文件读写，所只能覆盖这些实现了
 * 
 * @see org.xidea.lite.servlet.TemplateServlet
 */
@SuppressWarnings( { "unused", "serial" })
public class TemplateServlet extends org.xidea.lite.servlet.TemplateServlet {
	private static final Log log = LogFactory.getLog(TemplateServlet.class);
	private static final HashMap<String, Resource> cache = new HashMap<String, Resource>();

	static class Resource {
		private String data;
		private long time = System.currentTimeMillis();
	}
	public static void addFile(File file,String content){
		Resource res = new Resource();
		res.data = content;
		try {
			cache.put(file.toURI().toURL().getFile(),res);
		} catch (MalformedURLException e) {
		}
	}

	@Override
	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		log.info("req"+((HttpServletRequest)req).getRequestURI());
		super.service(req, resp);
	}

	@Override
	public void init(final ServletConfig config) throws ServletException {
		log.info("Template Initialized...");
		super.init(config);
		templateEngine = new ServletTemplateEngine(config);
	}

	public class ServletTemplateEngine extends
			org.xidea.lite.servlet.ServletTemplateEngine {

		public ServletTemplateEngine(ServletConfig config) {
			super(config);
		}

		@Override
		public void render(String path, Object context, Writer out)
				throws IOException {
			log.info(path);
//			if(path.endsWith(".html")){
//				path = path.substring(0,path.lastIndexOf('.'))+".xhtml";
//			}
			super.render(path, context, out);
		}

		@Override
		protected ParseContext createParseContext() {
			try {
				ParseContext context = new ParseContextImpl(getResource("/")) {

					@Override
					public InputStream getInputStream(URL url) {
						Resource data = cache.get(url.getFile());
						if (data != null) {
							try {
								return new ByteArrayInputStream(data.data
										.getBytes("utf-8"));
							} catch (UnsupportedEncodingException e) {
							}
						}
						return super.getInputStream(url);
					}

				};
				context.setCompress(compress);
				context.setFormat(format);
				context.setFeatrueMap(featrues);
				return context;
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected long getLastModified(File[] files) {
			long v = super.getLastModified(files);
			for (File file : files) {
				try {
					String path = file.toURI().toURL().getFile();
					Resource data = cache.get(path);
					if (data != null) {
						v = Math.max(v, data.time);
					}
				} catch (MalformedURLException e) {
				}
			}
			return v;
		}

	}

}
