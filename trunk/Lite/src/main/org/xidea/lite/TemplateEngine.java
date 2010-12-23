package org.xidea.lite;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;

public class TemplateEngine {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TemplateEngine.class);
	protected URI root;
	/**
	 * WeakHashMap 回收的太快了?
	 */
	protected Map<String, Template> templateMap = new java.util.WeakHashMap<String, Template>();

	protected TemplateEngine() {
	}

	public TemplateEngine(URI base) {
		this.root = base.normalize();
	}

	public void render(String path, Object context, Writer out)
			throws IOException {
		getTemplate(path).render(context, out);
		out.flush();
	}

	public Template getTemplate(String path) throws IOException {
		Template template = (Template) templateMap.get(path);
		if (template == null) {
			template = createTemplate(path);
			templateMap.put(path, template);
			return template;
		} else {
			return template;
		}
	}

	public void clear(String path) {
		templateMap.remove(path);
	}
	protected Template createTemplate(String path) throws IOException {
		List<List<Object>> data =  JSONDecoder.decode(getLiteCode(path));
		return new Template(data.get(1));
	}

	protected String getLiteCode(String path) throws IOException {
		URI uri = root.resolve(path.substring(1));
		InputStream in ;
		if("classpath".equals(root.getScheme())){
			String bp= root.normalize().getPath();
			in = TemplateEngine.class.getResourceAsStream(bp);
		}else{
			in = uri.toURL().openStream();
		}
		return loadText(in);

	}

	protected String loadText(InputStream in)
			throws IOException {
		try {
			InputStreamReader reader = new InputStreamReader(in,"UTF-8");
			StringBuilder buf = new StringBuilder();
			char[] cbuf = new char[256];
			int len;
			while ((len= reader.read(cbuf))>= 0) {
				buf.append(cbuf, 0, len);
			}
			return buf.toString();
		} finally {
			in.close();
		}
	}



}