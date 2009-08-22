package org.xidea.lite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.xidea.el.json.JSONDecoder;

public class TemplateEngine {
	private File base;
	protected Map<String, Template> templateMap = new java.util.WeakHashMap<String, Template>();

	public TemplateEngine(File base) {
		this.base = base;
	}

	public void render(String path, Object context, Writer out)
			throws IOException {
		getTemplate(path).render(context, out);
		out.flush();
	}

	public Template getTemplate(String path) {
		Template o = templateMap.get(path);
		if (o != null) {
			o = createTemplate(path);
			templateMap.put(path, o);
		}
		return (Template) o;
	}

	@SuppressWarnings("unchecked")
	protected Template createTemplate(String path) {
		try {
			URL url = getResource(path.replace('/', '.'));
			InputStream in = url.openStream();
			try {
				List data = (List) JSONDecoder.decode(loadText(in, "utf-8"));
				return new Template((List) data.get(1));
			} finally {
				in.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected URL getResource(String path) {
		try {
			if (base != null) {
				File file = new File(base, path);
				if (file.exists()) {
					return file.toURI().toURL();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	static String loadText(InputStream in, String encoding) throws IOException {
		InputStreamReader reader = new InputStreamReader(in, encoding);
		StringBuilder buf = new StringBuilder();
		char[] cbuf = new char[256];
		for (int len = reader.read(cbuf); len > 0; len = reader.read(cbuf)) {
			buf.append(cbuf, 0, len);
		}
		return buf.toString();
	}
}