package org.xidea.lite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.xidea.lite.parser.DecoratorContext;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.impl.DecoratorContextImpl;
import org.xidea.lite.parser.impl.ParseContextImpl;

public class TemplateEngine{
	public static final String DEFAULT_DECORATOR_MAPPING = "/WEB-INF/decorators.xml";
	private static final Log log = LogFactory.getLog(TemplateEngine.class);

	private HashMap<String, Object> lock = new HashMap<String, Object>();
	protected Map<String, TemplateEntry> templateMap = new java.util.WeakHashMap<String, TemplateEntry>();

	protected Map<String, String> featrues = new HashMap<String, String>();
	protected File webRoot;
	protected DecoratorContext decoratorMapper;

	protected TemplateEngine() {
	}

	public TemplateEngine(File webRoot) {
		this(webRoot, new File(webRoot, DEFAULT_DECORATOR_MAPPING));
	}

	public TemplateEngine(File webRoot, File config) {
		try {
			if(config != null && config.exists()){
				this.decoratorMapper = new DecoratorContextImpl(new FileInputStream(config));
			}else{
				log.warn("找不到装饰器配置信息:"+config.getAbsolutePath());
			}
			this.webRoot = webRoot;
		} catch (FileNotFoundException e) {
			log.error(e);
			throw new RuntimeException(e);
		}
	}

	public void render(String path, Object context, Writer out) throws IOException {
		getTemplate(path).render(context, out);
	}

	protected URL getResource(String pagePath) throws MalformedURLException {
		return new File(webRoot, pagePath).toURI().toURL();
	}

	/**
	 * @param path
	 * @return
	 */
	public Template getTemplate(String path) {
		TemplateEntry templateEntry = templateMap.get(path);
		if (templateEntry == null) {
			Object lock2 = null;
			synchronized (lock) {
				lock2 = lock.get(path);
				if (lock2 == null) {
					lock.put(path, lock2 = new Object());
				}
			}
			TemplateEntry entry;
			synchronized (lock2) {
				if (this.templateMap.containsKey(path)) {
					entry = this.templateMap.get(path);
				} else {
					entry = createTemplateEntry(path);
					this.templateMap.put(path, entry);
				}
			}
			lock.remove(path);
			return entry.getTemplate();
		} else {
			if (templateEntry.isModified()) {
				templateMap.remove(path);
				return getTemplate(path);
			} else {
				return templateEntry.getTemplate();
			}
		}
	}

	protected File[] getAssociatedFiles(Collection<URL> resources) {
		ArrayList<File> files = new ArrayList<File>();
		for (URL url : resources) {
			if ("file".equals(url.getProtocol())) {
				files.add(new File(url.getFile()));
			}
		}
		return files.toArray(new File[files.size()]);
	}

	protected ParseContext createParseContext() {
		try {
			return new ParseContextImpl(getResource("/"),featrues,null,null);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	protected Template createTemplate(String path, ParseContext parseContext) {
		String decoratorPath = null;
		if(decoratorMapper!=null){
			decoratorPath = decoratorMapper.getDecotatorPage(path);
		}
		if (decoratorPath != null && !decoratorPath.equals(path)) {
			try {
				Node node = parseContext.loadXML(getResource(path));
				parseContext.setAttribute("#page", node);
				path = decoratorPath;
			} catch (Exception e) {
				log.error(e);
			}
		}
		try {
			parseContext.parse(getResource(path));
			List<Object> items = parseContext.toList();
			return new Template(items);
		} catch (Exception e) {
			log.error(e);
			ArrayList<Object> errors = new ArrayList<Object>();
			errors.add(e.getMessage());
			return new Template(errors);
		}
	}

	private TemplateEntry createTemplateEntry(String path) {
		ParseContext parseContext = createParseContext();
		Template template = createTemplate(path, parseContext);
		File[] files = getAssociatedFiles(parseContext.getResources());
		return new TemplateEntry(template, files);
	}

	protected long getLastModified(File[] files) {
		long i = 0;
		long j = 0;
		for (File file : files) {
			long k = file.lastModified();
			if(k == 0){
				j++;
			}
			j*=2;
			i = Math.max(k, i);
		}
		return i+j;
	}
	protected class TemplateEntry {
		private Template template;
		private File[] files;
		private long lastModified;

		public TemplateEntry(Template template, File[] files) {
			this.template = template;
			this.files = files;
			this.lastModified = getLastModified(files);
		}

		public boolean isModified() {
			return this.lastModified != getLastModified(files);
		}

		public Template getTemplate() {
			return template;
		}

	}

}
