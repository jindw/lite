package org.xidea.lite.compiler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.*;

import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.XMLParser;

@SuppressWarnings("serial")
public class LiteCompilerServlet extends HttpServlet {
	private XMLParser parser = new XMLParser();
	private Template homeTemplate = new Template(parser.parse(this.getClass().getResource("index.xhtml")));
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html;charset=utf-8");
		PrintWriter out = resp.getWriter();
		homeTemplate.render(null, out);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain;charset=utf-8");
		String[] path = req.getParameterValues("path");
		String[] source = req.getParameterValues("source");
		HashMap<String, String> sourceMap = new HashMap<String, String>();
		for (int i = 0; i < path.length; i++) {
			sourceMap.put(path[i], source[i]);
		}
		ProxyParseContext context = new ProxyParseContext(
				sourceMap, req.getCharacterEncoding());
		context.setCompress(true);
		parser.parse(context.createURL(null,path[0]), context);
		resp.getWriter().println(JSONEncoder.encode(context.toResultTree()));
	}
}
