package org.xidea.lite.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.*;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.parser.DecoratorContext;
import org.xidea.lite.parser.impl.DecoratorContextImpl;

/**
 * @author jindw
 * 
 */
@SuppressWarnings("serial")
public class LiteCompilerServlet extends HttpServlet {
	private static final String[] FEATRUES_KEY = { "compress", "format" };
	private static Log log = LogFactory.getLog(LiteCompilerServlet.class);
	private HashMap<String, String> defaultFeatrues;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		@SuppressWarnings("unchecked")
		Enumeration<String> names = config.getInitParameterNames();
		HashMap<String, String> featrues = new HashMap<String, String>();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			featrues.put(name, config.getInitParameter(name));
		}
		this.defaultFeatrues = featrues;
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		resp.setContentType("text/html;charset=utf-8");
		req.setAttribute("source", getExampleSource("example-lite.xml"));
		req.setAttribute("plugin", getExampleSource("example-plugin.js"));
		req.setAttribute("path", "/index.html");
		req.setAttribute("base", "/");
		req.getRequestDispatcher("/compiler.xhtml").forward(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain;charset=utf-8");
		req = wrapRequest(req);
		String base = req.getParameter("base");
		String[] paths = req.getParameterValues("path");
		String[] sources = req.getParameterValues("source");
		int i = sources.length;
		String templateURL = "/";
		HashMap<String, String> resourceMap = new HashMap<String, String>();
		while (i-- > 0) {
			if (paths != null && paths.length > i) {
				templateURL = paths[i];
			}
			resourceMap.put(templateURL, sources[i]);
		}
		ProxyParseContext context = new ProxyParseContext(base,
				buildFeatrueMap(req), resourceMap, req.getParameter("plugin"));
		String model = req.getParameter("model");
		if ("text".equals(model)) {
			PrintWriter out = resp.getWriter();
			printResult(context, resourceMap.get(templateURL), out);
		} else {
			try {
				String decoratorxml = resourceMap
						.get("/WEB-INF/decorators.xml");
				if (decoratorxml != null) {
					DecoratorContext mapper = new DecoratorContextImpl(
							new StringReader(decoratorxml));
					String layout = mapper.getDecotatorPage(templateURL);
					if (layout != null) {
						if (resourceMap.containsKey(layout)) {
							context.setAttribute("#page", context
									.loadXML(context.createURL(null,
											templateURL)));
							templateURL = layout;
						} else {
							context.addMissedResource(layout);
						}
					}

				}
			} catch (Exception e) {
			}

			PrintWriter out = resp.getWriter();
			URL source = context.createURL(null, templateURL);
			printResult(context, source, out);
		}
	}

	private void printResult(ProxyParseContext context, Object source,
			PrintWriter out) {
		String error = null;
		List<Object> result = null;
		try {
			context.parse(source);
			result = context.toList();
		} catch (Throwable e) {
			StringWriter buf = new StringWriter();
			PrintWriter pbuf = new PrintWriter(buf);
			e.printStackTrace(pbuf);
			pbuf.flush();
			error = "编译失败：" + buf.toString();
			error = error == null ? "unknow error" : error;
			log.info(e);
		} finally {
			List<String> missed = context.getMissedResources();
			if (error != null || !missed.isEmpty()) {
				HashMap<Object, Object> map = new LinkedHashMap<Object, Object>();
				map.put("missed", missed);
				if (error != null) {
					map.put("error", error);
				}
				out.print(JSONEncoder.encode(map));
			} else {
				out.println(JSONEncoder.encode(result));
			}
		}
	}

	private HashMap<String, String> buildFeatrueMap(ServletRequest req) {
		HashMap<String, String> featrue = new HashMap<String, String>(
				defaultFeatrues);
		for (String name : FEATRUES_KEY) {
			featrue.put(name, featrue.get(name));
		}
		return featrue;
	}

	private HttpServletRequest wrapRequest(HttpServletRequest req)
			throws IOException {
		if (isMultiPart(req)) {
			final HashMap<String, List<String>> params = getMutiParams(req);
			req = new HttpServletRequestWrapper(req) {
				@Override
				public String getParameter(String name) {
					String[] v = getParameterValues(name);
					return v == null ? null : v[0];
				}

				@Override
				public String[] getParameterValues(String name) {
					List<String> v = params.get(name);
					if (v == null) {
						return findPhpParams(this, name);
					} else {
						return v.toArray(new String[v.size()]);
					}
				}
			};
		} else {
			req = new HttpServletRequestWrapper(req) {

				@Override
				public String[] getParameterValues(String name) {
					String[] v = super.getParameterValues(name);
					if (v == null) {
						return findPhpParams(this, name);
					} else {
						return v;
					}
				}
			};
		}
		return req;
	}

	private String[] findPhpParams(HttpServletRequest req, String name) {
		String[] source = req.getParameterValues(name);
		if (source == null) {
			ArrayList<String> values = new ArrayList<String>();
			int i = 0;
			name = name + '[';
			while (true) {
				String value = req.getParameter(name + (i++) + ']');
				if (value == null) {
					break;
				} else {
					values.add(value);
				}
			}
			source = values.toArray(new String[i]);
		}
		return source;
	}

	public static boolean isMultiPart(HttpServletRequest request) {
		String content_type = request.getContentType();
		return content_type != null
				&& content_type.indexOf("multipart/form-data") != -1;
	}

	private HashMap<String, List<String>> getMutiParams(HttpServletRequest req)
			throws IOException {
		DiskFileItemFactory fac = new DiskFileItemFactory();
		// fac.setSizeThreshold(0);
		HashMap<String, List<String>> params = new LinkedHashMap<String, List<String>>();
		try {

			ServletFileUpload upload = new ServletFileUpload(fac);
			FileItemIterator items = upload.getItemIterator(req);
			while (items.hasNext()) {
				FileItemStream item = items.next();
				if (log.isDebugEnabled()) {
					log.debug("Found item " + item.getFieldName());
				}
				// if (!item.isFormField()) {
				List<String> values;
				if (params.get(item.getFieldName()) != null) {
					values = params.get(item.getFieldName());
				} else {
					values = new ArrayList<String>();
				}
				// note: see http://jira.opensymphony.com/browse/WW-633
				// basically, in some cases the charset may be null, so
				// we're just going to try to "other" method (no idea if
				// this
				// will work)
				String charset = req.getCharacterEncoding();
				if (charset == null) {
					charset = "utf-8";
				}
				values.add(getString(item.openStream(), charset));
				params.put(item.getFieldName(), values);
				// } else if (item.getSize() == 0) {
				// log.warn("Item is a file upload of 0 size, ignoring");
				// }
			}
		} catch (FileUploadException e) {
			log.error(e, e);
		}
		return params;
	}

	private String getExampleSource(String fileName) throws IOException {
		return getString(this.getClass().getResourceAsStream(fileName), "utf-8");

	}

	private String getString(InputStream stream, String encode)
			throws IOException {
		Reader in = new InputStreamReader(stream, encode);
		StringBuilder buf = new StringBuilder();
		char[] cbuf = new char[1024];
		int i;
		while ((i = in.read(cbuf)) >= 0) {
			buf.append(cbuf, 0, i);
		}
		return buf.toString();

	}
}