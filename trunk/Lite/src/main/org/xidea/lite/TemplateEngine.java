package org.xidea.lite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.parser.ResourceContext;

public class TemplateEngine implements ResourceContext{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TemplateEngine.class);
	private HashMap<String, Object> lock = new HashMap<String, Object>();
	protected URI baseURI;
	/**
	 * WeakHashMap 回收的太快了?
	 */
	protected Map<String, Template> templateMap = new java.util.WeakHashMap<String, Template>();

	public TemplateEngine(URI base) {
		this.baseURI = base;
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

	@SuppressWarnings("unchecked")
	protected Template createTemplate(String path) {
		try {
			URI uri = getResource(path.replace('/', '.'));
			InputStream in = openInputStream(uri);
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

	protected URI getResource(String path) {
		return createURI(path,null);
	}

	public URI createURI(String path, URI parentURI) {
		try {
			URI parent = parentURI != null ? parentURI : this.baseURI;
			if (path.startsWith("/")) {
				if (parentURI == null
						|| parent.toString().startsWith(baseURI.toString())) {
					String prefix = baseURI.getPath();
					int p  =prefix.lastIndexOf('/');
					path = prefix.substring(0,p)+path;
				}
			}
			return parent.resolve(path);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream openInputStream(URI uri) {
		try {
			if ("data".equalsIgnoreCase(uri.getScheme())) {
				String data = uri.getRawSchemeSpecificPart();
				int p = data.indexOf(',')+1;
				String h = data.substring(0,p).toLowerCase();
				String charset = "UTF-8";
				data = data.substring(p);
				p = h.indexOf("charset=");
				if(p >0){
					charset = h.substring(h.indexOf('=',p)+1,h.indexOf(',',p));
				}
				return new ByteArrayInputStream(URLDecoder.decode(data,charset).getBytes(charset));
				//charset=
			}else if ("classpath".equalsIgnoreCase(uri.getScheme())) {
				ClassLoader cl = this.getClass().getClassLoader();
				uri = uri.normalize();
				String path = uri.getPath();
				path = path.substring(1);
				InputStream in = cl.getResourceAsStream(path);
				if (in == null) {
					ClassLoader cl2 = Thread.currentThread()
							.getContextClassLoader();
					if (cl2 != null) {
						in = cl2.getResourceAsStream(path);
					}
				}
				return in;
			} else {
				return uri.toURL().openStream();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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