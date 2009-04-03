package org.xidea.lite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.xidea.lite.Template;
import org.xidea.lite.parser.DecoratorMapper;
import org.xidea.lite.parser.HTMLFormNodeParser;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.ParseContextImpl;
import org.xidea.lite.parser.XMLParser;

public class TemplateEngine{
	protected static final String DEFAULT_DECORATOR_MAPPING = "/WEB-INF/decorators.xml";
	private static final Log log = LogFactory.getLog(TemplateEngine.class);

	private HashMap<String, Object> lock = new HashMap<String, Object>();
	private Map<String, TemplateEntry> cachedMap = new HashMap<String, TemplateEntry>();

	protected File webRoot;
	protected DecoratorMapper decoratorMapper;
	protected XMLParser parser = new XMLParser();

	protected TemplateEngine() {
	}

	public TemplateEngine(File webRoot) {
		this(webRoot, new File(webRoot, DEFAULT_DECORATOR_MAPPING));
	}

	public TemplateEngine(File webRoot, File config) {
		try {
			if(config != null && config.exists()){
				this.decoratorMapper = new DecoratorMapper(new FileInputStream(config));
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
	protected Template getTemplate(String path) {
		TemplateEntry templateEntry = cachedMap.get(path);
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
				if (this.cachedMap.containsKey(path)) {
					entry = this.cachedMap.get(path);
				} else {
					entry = createTemplateEntry(path);
					this.cachedMap.put(path, entry);
				}
			}
			lock.remove(path);
			return entry.getTemplate();
		} else {
			if (templateEntry.isModified()) {
				cachedMap.remove(path);
				return getTemplate(path);
			} else {
				return templateEntry.getTemplate();
			}
		}
	}

	protected List<File> getAssociatedFiles(Set<URL> resources) {
		ArrayList<File> files = new ArrayList<File>();
		for (URL url : resources) {
			if ("file".equals(url.getProtocol())) {
				files.add(new File(url.getFile()));
			}
		}
		return files;
	}

	protected ParseContext createParseContext() {
		try {
			ParseContext parseContext = new ParseContextImpl(getResource("/"));
			parseContext.setAttribute(HTMLFormNodeParser.class, HTMLFormNodeParser.AUTO_IN_FORM);
			return parseContext;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	protected Template createTemplate(String path, ParseContext parseContext) {
		String decoratorPath = null;
		if(decoratorMapper!=null){
			decoratorPath = decoratorMapper.getDecotatorPage(path);
		}
		if(this.webRoot!=null){
			try {
				parseContext.addResource(new File(webRoot,path).toURI().toURL());
				if(decoratorPath!=null){
					parseContext.addResource(new File(webRoot,decoratorPath).toURI().toURL());
				}
			} catch (MalformedURLException e) {
				log.warn(e);
			}
		}
		if (decoratorPath != null && !decoratorPath.equals(path)) {
			try {
				Node node = parser.loadXML(getResource(path), parseContext);
				parseContext.setAttribute("#page", node);
				path = decoratorPath;
			} catch (Exception e) {
				log.error(e);
			}
		}
		try {
			List<Object> items = parser.parse(getResource(path), parseContext);
			return new Template(items);
		} catch (IOException e) {
			log.error(e);
			throw new RuntimeException(e);
		}
	}

	private TemplateEntry createTemplateEntry(String path) {
		ParseContext parseContext = createParseContext();
		Template template = createTemplate(path, parseContext);
		List<File> files = getAssociatedFiles(parseContext.getResources());
		return new TemplateEntry(template, files.toArray(new File[files.size()]));
	}
	
	private static class TemplateEntry {
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

		private long getLastModified(File[] files) {
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
	}

}
