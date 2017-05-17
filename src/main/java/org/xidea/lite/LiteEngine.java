package org.xidea.lite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;

public class LiteEngine implements TemplateEngine {
	private static final Log log = LogFactory.getLog(LiteEngine.class);
	private URI compiledBase;
	/**
	 * WeakHashMap 回收的太快了?
	 */
	protected Map<String, Template> templateMap = new java.util.WeakHashMap<String, Template>();
	protected ExecutorService executorService = Executors.newScheduledThreadPool(8);//最多允许是个并发写装载的lazy_widget
	protected LiteEngine() {
	}

	public LiteEngine(URI compiledBase) {
		if (compiledBase != null) {
			this.compiledBase = compiledBase.normalize();
		}
	}
	public void render(String path, Object context, Writer out)
			throws IOException {
		getTemplate(path).render(context, out);
		out.flush();
	}

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

	public void clear(String path) {
		templateMap.remove(path);
	}

	@SuppressWarnings("unchecked")
	protected Template createTemplate(String path) throws IOException {
		// [resources,litecode,config]
		String litecode = getLitecode(path);
		if(litecode !=null){
			List<Object> data = JSONDecoder.decode(litecode);
			List<Object> list = (List<Object>) data.get(1);
			Map<String, String> featureMap = (Map<String, String>) data.get(2);
			return new LiteTemplate(executorService,list, featureMap);
		}else{
			log.error("template not found!"+path+'@'+compiledBase);
			return null;
		}
	}

	private String getLitecode(String path) {
		
		try {
			InputStream in ;
			URI uri = compiledBase.resolve(compiledBase.getPath()+path);
			String scheme = compiledBase.getScheme();
			if ("classpath".equals(scheme)) {
				String bp = uri.normalize().getPath();
				in = LiteEngine.class.getResourceAsStream(bp);
			}else if ("file".equals(scheme) && !new File(uri).exists()) {
				return null;
			}else{
				
				in =  uri.toURL().openStream();
			}
			if( in !=null ){
				return loadText(in);
			}
		} catch (IOException e) {
			log.error(e);
		}
		return null;
	}

	String loadText(InputStream in) throws IOException {
		try {
			InputStreamReader reader = new InputStreamReader(in, "UTF-8");
			StringBuilder buf = new StringBuilder();
			char[] cbuf = new char[1024];
			int len;
			while ((len = reader.read(cbuf)) >= 0) {
				buf.append(cbuf, 0, len);
			}
			return buf.toString();
		} finally {
			in.close();
		}
	}

}