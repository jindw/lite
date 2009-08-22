package org.xidea.lite.parser.impl;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.xidea.lite.Template;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parser.DecoratorContext;
import org.xidea.lite.parser.ParseContext;

public class HotTemplateEngine extends TemplateEngine {
	public static final String DEFAULT_DECORATOR_MAPPING = "/WEB-INF/decorators.xml";
	private static final Log log = LogFactory.getLog(HotTemplateEngine.class);
	private HashMap<String, Object> lock = new HashMap<String, Object>();
	private HashMap<String, Info> infoMap = new HashMap<String, Info>();
	protected Map<String, String> featrues = new HashMap<String, String>();
	protected DecoratorContext decoratorContext;

	protected HotTemplateEngine() {
		super(null);
	}

	public HotTemplateEngine(File webRoot) {
		this(webRoot, new File(webRoot, DEFAULT_DECORATOR_MAPPING));
	}

	public HotTemplateEngine(File webRoot, File config) {
		super(webRoot);
		if (config != null && config.exists()) {
			this.decoratorContext = new DecoratorContextImpl(config);
		} else {
			log.warn("找不到装饰器配置信息:" + config.getAbsolutePath());
		}
	}

	class Info {
		File[] files;
		long lastModified;

		Info(File[] files) {
			this.files = files;
			this.lastModified = getLastModified(files);
		}

		boolean isModified() {
			return this.lastModified != getLastModified(files);
		}
	}

	protected File[] getAssociatedFiles(ParseContext context) {
		ArrayList<File> files = new ArrayList<File>();
		for (URL url : context.getResources()) {
			if ("file".equals(url.getProtocol())) {
				files.add(new File(url.getFile()));
			}
		}
		return files.toArray(new File[files.size()]);
	}

	protected long getLastModified(File[] files) {
		long i = 0;
		long j = 0;
		for (File file : files) {
			long k = file.lastModified();
			if (k == 0) {
				j++;
			}
			j *= 2;
			i = Math.max(k, i);
		}
		return i + j;
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
					ParseContext parseContext = createParseContext();
					template = createTemplate(path, parseContext);
					File[] files = getAssociatedFiles(parseContext);
					Info entry =  new Info(files);
					infoMap.put(path, entry);
				}
			}
			lock.remove(path);
			return template;
		} else {
			templateMap.remove(path);
			infoMap.remove(path);
			// return getTemplate(path);
			return template;
		}
	}

	private boolean isModified(String path) {
		Info templateEntry = (Info) infoMap.get(path);
		return templateEntry == null || templateEntry.isModified();
	}

	protected Template createTemplate(String path) {
		throw new UnsupportedOperationException();
	}

	protected ParseContext createParseContext() {
		return new ParseContextImpl(getResource("/"), featrues, null, null);
	}

	protected Template createTemplate(String path, ParseContext parseContext) {
		String decoratorPath = null;
		if (decoratorContext != null) {
			decoratorPath = decoratorContext.getDecotatorPage(path);
		}
		if (decoratorPath != null && !decoratorPath.equals(path)) {
			try {
				Node node = parseContext.loadXML(getResource(path));
				parseContext.setAttribute("#page", node);
				path = decoratorPath;
			} catch (Exception e) {
				log.error("模板解析失败", e);
				StringWriter out = new StringWriter();
				out.append("模板编译失败：\r\n<hr>");
				PrintWriter pout = new PrintWriter(out, true);
				e.printStackTrace(pout);
				parseContext.append(out.toString());
			}
		}
		parseContext.parse(getResource(path));
		List<Object> items = parseContext.toList();
		return new Template(items);
	}

}
