package org.xidea.lite.gae;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;

public class JSONModelFilter implements javax.servlet.Filter {
	private static final Log log = LogFactory.getLog(JSONModelFilter.class);
	
	private ServletContext context;
	static Map<String, Map<String, Object>> cachedMap = new HashMap<String, Map<String, Object>>();

	@Override
	public void destroy() {

	}

	@SuppressWarnings("unchecked")
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		Map<String, Object> json = getJSON(getJSONPath(request.getServletPath()));
		if (json instanceof Map) {
			for (Map.Entry<String, Object> entry : json.entrySet()) {
				req.setAttribute(entry.getKey(), entry.getValue());
			}
		}
		resp.setContentType("text/html;charset=utf-8");
		req.setAttribute("requestURI", request.getRequestURI());
		chain.doFilter(req, resp);
	}

	public static String getJSONPath(String path) {
		if (path.endsWith("/")) {
			return path + "index.json";
		}
		return path.replaceFirst("\\.xhtml$", ".json");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getJSON(String path) {
		Map<String, Object> json = cachedMap.get(path);
		log.debug("load JSON :"+path);
		if (json == null) {
			InputStream jsonResource = context.getResourceAsStream(path);
			if(jsonResource!=null){
				json = (Map<String, Object>) JSONDecoder.decode(loadText(jsonResource));
			}else{
				log.debug("missed JSON :"+path);
			}
		}
		return json;
	}

	public static String loadText(InputStream ins) {
		try {
			Reader in = new InputStreamReader(ins, "utf-8");
			StringBuilder buf = new StringBuilder();
			char[] cbuf = new char[1024];
			for (int len = in.read(cbuf); len > 0; len = in.read(cbuf)) {
				buf.append(cbuf, 0, len);
			}
			return buf.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		this.context = config.getServletContext();
	}

}
