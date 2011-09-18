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

public class LiteEngine implements TemplateEngine {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(LiteEngine.class);
	protected URI compiledBase;
	/**
	 * WeakHashMap 回收的太快了?
	 */
	protected Map<String, Template> templateMap = new java.util.WeakHashMap<String, Template>();

	protected LiteEngine() {
	}

	public LiteEngine(URI compiledBase) {
		if(compiledBase!=null){
			this.compiledBase = compiledBase.normalize();
		}
	}

	/* (non-Javadoc)
	 * @see org.xidea.lite.ITemplateEngine#render(java.lang.String, java.lang.Object, java.io.Writer)
	 */
	public void render(String path, Object context, Writer out)
			throws IOException {
		getTemplate(path).render(context, out);
		out.flush();
	}

	/* (non-Javadoc)
	 * @see org.xidea.lite.ITemplateEngine#getTemplate(java.lang.String)
	 */
	public Template getTemplate(String path) throws IOException {
		Template template = templateMap.get(path);
		if (template == null) {
			template = createTemplate(path);
			templateMap.put(path, template);
			return template;
		} else {
			return template;
		}
	}

	/* (non-Javadoc)
	 * @see org.xidea.lite.ITemplateEngine#clear(java.lang.String)
	 */
	public void clear(String path) {
		templateMap.remove(path);
	}
	@SuppressWarnings("unchecked")
	protected Template createTemplate(String path) throws IOException {
		List<Object> data =  JSONDecoder.decode(getLitecode(path));
		List<Object> list = (List<Object>) data.get(1);
		Map<String,String> featureMap = (Map<String,String>) data.get(2);
		return new LiteTemplate(list,featureMap);
	}

	protected String getLitecode(String path) throws IOException {
		URI uri = compiledBase.resolve(path.substring(1));
		InputStream in ;
		if("classpath".equals(compiledBase.getScheme())){
			String bp= compiledBase.normalize().getPath();
			in = LiteEngine.class.getResourceAsStream(bp);
		}else{
			in = uri.toURL().openStream();
		}
		return loadText(in);

	}

	private String loadText(InputStream in)
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