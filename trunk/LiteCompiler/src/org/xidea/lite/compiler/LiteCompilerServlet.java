package org.xidea.lite.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.parser.XMLParser;

@SuppressWarnings("serial")
public class LiteCompilerServlet extends HttpServlet {
	private static Log log = LogFactory.getLog(LiteCompilerServlet.class);

	private XMLParser parser = new XMLParser();

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
			req = new HttpServletRequestWrapper(req){

				@Override
				public String getParameter(String name) {
					String[] v = getParameterValues(name);
					return v==null ?null:v[0];
				}

				@Override
				public String[] getParameterValues(String name) {

					List<String> v = params.get(name);
					return v == null?null : v.toArray(new String[v.size()]);
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
			throws UnsupportedEncodingException {
		FileItemFactory fac = new FileItemFactory() {
			@Override
			public FileItem createItem(final String fieldName,
					final String contentType, final boolean isFormField,
					final String fileName) {
				return new ByteFileItem(fieldName, contentType, isFormField,
						fileName);
			}

		};
		// fac.setSizeThreshold(0);
		HashMap<String, List<String>> params = new LinkedHashMap<String, List<String>>();
		try {

			ServletFileUpload upload = new ServletFileUpload(fac);
			List<FileItem> items = upload.parseRequest(req);
			for (int i = 0; i < items.size(); i++) {
				FileItem item = items.get(i);
				if (log.isDebugEnabled()) {
					log.debug("Found item " + item.getFieldName());
				}
				//if (!item.isFormField()) {
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
					if (charset != null) {
						values.add(item.getString(charset));
					} else {
						values.add(item.getString());
					}
					params.put(item.getFieldName(), values);
					// } else if (item.getSize() == 0) {
					// log.warn("Item is a file upload of 0 size, ignoring");
				//}
			}
		} catch (FileUploadException e) {
			log.error(e, e);
		}
		return params;
	}

	private String getExampleSource() throws IOException {
		Reader in = new InputStreamReader(this.getClass().getResourceAsStream(
				"example.xml"), "utf-8");
		StringBuilder buf = new StringBuilder();
		char[] cbuf = new char[1024];
		int i;
		while ((i = in.read(cbuf)) >= 0) {
			buf.append(cbuf, 0, i);
		}
		return buf.toString();

	}
}

class ByteFileItem implements FileItem{

	private String fieldName;
	private String contentType;
	private boolean formField;
	private String fileName;
	private ByteArrayOutputStream data = new ByteArrayOutputStream();

	public ByteFileItem(String fieldName, String contentType,
			boolean isFormField, String fileName) {
		this.fieldName = fieldName;
		this.contentType = contentType;
		this.formField = isFormField;
		this.fileName = fileName;
	}

	public void delete() {
		data = null;
	}
	public byte[] get() {
		return data.toByteArray();
	}

	public String getContentType() {
		return contentType;
	}

	public String getFieldName() {
		return fieldName;
	}

	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(data.toByteArray());
	}

	public String getName() {
		return fieldName;
	}

	public OutputStream getOutputStream() throws IOException {
		return data;
	}

	public long getSize() {
		return data.size();
	}

	public String getString() {
		try {
			return getString("utf-8");
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}

	public String getString(String encoding)
			throws UnsupportedEncodingException {
		return new String(get(),encoding);
	}

	public boolean isFormField() {
		return formField;
	}

	public boolean isInMemory() {
		return false;
	}

	public void setFieldName(String name) {
		this.fieldName = name;

	}

	public void setFormField(boolean state) {
		this.formField = state;
	}

	public void write(File file) throws Exception {

	}

}