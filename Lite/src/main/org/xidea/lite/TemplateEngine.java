package org.xidea.lite;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.parser.ResourceContext;
import org.xidea.lite.parser.impl.ResourceContextImpl;

public class TemplateEngine {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TemplateEngine.class);
	private HashMap<String, Object> lock = new HashMap<String, Object>();
	protected ResourceContext base;
	/**
	 * WeakHashMap 回收的太快了?
	 */
	protected Map<String, Template> templateMap = new java.util.WeakHashMap<String, Template>();

	public TemplateEngine(URI base) {
		this.base = new ResourceContextImpl(base);
	}

	public void render(String path, Object context, Writer out)
			throws IOException {
		getTemplate(path).render(context, out);
		out.flush();
	}

	public Template getTemplate(String path) {
		Template template = (Template) templateMap.get(path);
		if (template == null || isModified(path)) {
			Object lock2 = null;
			synchronized (lock) {
				lock2 = lock.get(path);
				if (lock2 == null) {
					lock.put(path, lock2 = new Object());
				}
			}
			synchronized (lock2) {
				template = (Template) templateMap.get(path);
				if (template == null || isModified(path)) {
					template = createTemplate(path);
					templateMap.put(path, template);
				}
			}
			lock.remove(path);
			return template;
		} else {
			return template;
		}
	}

	protected boolean isModified(String path) {
		return false;
	}

	protected Template createTemplate(String path) {
		try {
			List<List<Object>> data =  JSONDecoder.decode(getLiteCode(path));
			return new Template(data.get(1));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected String getLiteCode(String path) throws IOException {
		URI uri =  base.createURI(path.replace('/', '.'),null);
		InputStream in = base.openStream(uri);
		try {
			InputStreamReader reader = new InputStreamReader(in, "UTF-8");
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