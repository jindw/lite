package org.xidea.lite.gae;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("serial")
public class EditServlet extends HttpServlet {
	private static final Log log = LogFactory.getLog(EditServlet.class);

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		resp.setContentType("text/html;charset=utf-8");
		String path = req.getParameter("path");
		if(path == null){
			resp.sendRedirect("/");
			return ;
		}
		if (path.endsWith("/")) {
			path = path + "index.xhtml";
		}
		final InputStream templateResource = getServletContext()
				.getResourceAsStream(path);
		final InputStream jsonResource = getServletContext()
				.getResourceAsStream(JSONModelFilter.getJSONPath(path));
		String template = null;
		String json = null;
		if (templateResource != null) {
			template = JSONModelFilter.loadText(templateResource);
		} else {
			log.warn("missed path:" + path);
		}
		if (jsonResource != null) {
			json = JSONModelFilter.loadText(jsonResource);
		} else {
			log.warn("missed json:" + JSONModelFilter.getJSONPath(path));
		}
		req.setAttribute("json", json);
		req.setAttribute("template", template);
		req.getRequestDispatcher("/WEB-INF/edit.xhtml").forward(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		log.info("用户：" + req.getUserPrincipal() + "/" + req.getRemoteAddr()
				+ "/" + req.getRemoteUser());
		String path = req.getParameter("path");
		if (path.endsWith("/")) {
			path = path + "index.xhtml";
		}
		String template = req.getParameter("template");
		save(path, template);
		String json = req.getParameter("json");
		save(JSONModelFilter.getJSONPath(path), json);
		resp.sendRedirect(path);
	}

	@SuppressWarnings("unchecked")
	private void save(String path, String data) throws MalformedURLException,
			IOException {
		if (data != null && data.trim().length() > 0) {
			if (path.endsWith(".xhtml")) {
				try {
					javax.xml.parsers.SAXParserFactory.newInstance()
							.newSAXParser().parse(
									new ByteArrayInputStream(data
											.getBytes("utf-8")),
									new DefaultHandler());
					TemplateServlet.addFile(new File(this.getServletContext()
							.getRealPath(path)), data);
				} catch (Exception e) {
					throw new IOException("无效xml", e);
				}
			} else if (path.endsWith(".json")) {
				JSONModelFilter.cachedMap.put(path, (Map) JSONDecoder
						.decode(data));
			} else {
				throw new IOException("只允许编辑xhtml和json文件^_^");
			}
		}

		// final OutputStream o = new
		// FileOutputStream(getServletContext().getRealPath(path));
		// if (o != null && data != null && data.trim().length() > 0) {
		// try {
		// log.info("编辑文件"+path+"/"+data);
		// Writer out = new OutputStreamWriter(o,
		// "UTF-8");
		// out.write(data);
		// out.flush();
		// out.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
	}

}
