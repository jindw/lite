package org.xidea.lite.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parser.DecoratorMapper;
import org.xidea.lite.parser.XMLParser;

@SuppressWarnings("serial")
public class LiteCompilerServlet extends HttpServlet {
	private static Log log = LogFactory.getLog(LiteCompilerServlet.class);
	private XMLParser parser;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String transformerFactory = config
				.getInitParameter("transformerFactory");
		String xpathFactory = config.getInitParameter("xpathFactory");
		parser = new XMLParser(transformerFactory, xpathFactory);
	}
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		resp.setContentType("text/html;charset=utf-8");

		req.setAttribute("source", getExampleSource());
		req.setAttribute("path", "/index.html");
		req.setAttribute("base", "/");
		req.getRequestDispatcher("/compiler.xhtml").forward(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain;charset=utf-8");
		final HashMap<String, String> sourceMap = new HashMap<String, String>();
		String[] source;
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
					return v == null ? null : v.toArray(new String[v.size()]);
				}
			};
			source = params.get("file").toArray(new String[0]);
		} else {
			source = req.getParameterValues("source");
		}
		String base = req.getParameter("base");
		String url = "/";
		String[] path = req.getParameterValues("path");
		int i = source.length;
		while (i-- > 0) {
			if (path != null && path.length > i) {
				url = path[i];
			}
			sourceMap.put(url, source[i]);

		}
		ProxyParseContext context = new ProxyParseContext(base, sourceMap, req
				.getCharacterEncoding());
		try {
			String decoratorxml = sourceMap.get("/WEB-INF/decorators.xml");
			if(decoratorxml != null){
				DecoratorMapper mapper = new DecoratorMapper(new StringReader(decoratorxml));
				String layout = mapper.getDecotatorPage(url);
				if(layout != null){
					context.setAttribute("#page", parser.loadXML(url, context));
					layout = url;
				}
			}
		} catch (Exception e) {
		}
		context.setCompress(true);
		parser.parse(context.createURL(null, url), context);
		resp.getWriter().println(JSONEncoder.encode(context.toResultTree()));
	}

	public static boolean isMultiPart(HttpServletRequest request) {
		String content_type = request.getContentType();
		return content_type != null
				&& content_type.indexOf("multipart/form-data") != -1;
	}

	public HashMap<String, List<String>> getMutiParams(HttpServletRequest req)
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
				values.add(getString(item.openStream(),charset));
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

	private String getExampleSource() throws IOException {
		return getString(this.getClass().getResourceAsStream("example.xml"),
				"utf-8");

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