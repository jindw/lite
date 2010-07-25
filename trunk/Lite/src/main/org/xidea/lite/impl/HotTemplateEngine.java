package org.xidea.lite.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;

public class HotTemplateEngine extends TemplateEngine {
	private static final Log log = LogFactory.getLog(HotTemplateEngine.class);
	private HashMap<String, Object> lock = new HashMap<String, Object>();
	private HashMap<String, Info> infoMap = new HashMap<String, Info>();
	protected ParseConfig config;

	public HotTemplateEngine(URI base) {
		super(base);
	}

	public HotTemplateEngine(ParseConfig config) {
		this.config = config;
	}

	protected ParseContext createParseContext(String path) {
		return new ParseContextImpl(config, path);
	}

	public Template getTemplate(String path) throws IOException {
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
		Info templateEntry = (Info) infoMap.get(path);
		return templateEntry == null || templateEntry.isModified();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Template createTemplate(String path) throws IOException {
		ArrayList<File> files = new ArrayList<File>();
		if (config == null) {
			URI uri = this.base.resolve(path.replace('/', '^'));
			String litecode = loadText(ParseUtil.openStream(uri));
			List<Object> list = JSONDecoder.decode(litecode);
			if ("file".equals(uri.getScheme())) {
				files.add(new File(uri));
			}
			Info entry = new Info(files);
			infoMap.put(path, entry);
			return new Template((List<Object>) list.get(1));
		} else {
			ParseContext context = createParseContext(path);
			List<Object> items = parse(path, context);
			if ("file".equals(config.getRoot().getScheme())) {
				File base = new File(config.getRoot());
				for (String p : getAssociatedPaths(context)) {
					files.add(new File(base, p));
				}
			}
			Template template = new Template(items);
			Info entry = new Info(files);
			infoMap.put(path, entry);
			return template;
		}
	}

	public String getLiteCode(String path) {
		ParseContext context = createParseContext(path);
		List<Object> items = parse(path, context);
		return getLiteCode(context, items);
	}

	private String getLiteCode(ParseContext context, List<Object> items) {
		ArrayList<Object> result = new ArrayList<Object>();
		List<String> resource = getAssociatedPaths(context);
		result.add(resource);
		result.add(items);
		result.add(context.getFeatrueMap());
		return JSONEncoder.encode(result);

	}

	private List<String> getAssociatedPaths(ParseContext context) {
		ArrayList<String> resource = new ArrayList<String>();
		if (config instanceof ParseConfigImpl) {
			File configFile = ((ParseConfigImpl) config).getFile();
			if (configFile != null) {
				String root = config.getRoot().toString();
				String config = configFile.toURI().toString();
				if (config.startsWith(root)) {
					config = config.substring(root.length());
					if (config.length() > 0) {
						if (config.charAt(0) != '/') {
							config = '/' + config;
						}
						resource.add(config);
					}
				}
			}
		}
		for (URI uri : context.getResources()) {
			if ("lite".equals(uri.getScheme())) {
				resource.add(uri.getPath());
			} else {
				// if("file".equals(uri.getScheme())){
				log.warn("忽略关联文件:" + uri);
				// }
			}
		}
		return resource;
	}

	private List<Object> parse(String path, ParseContext parseContext) {
		try {
			parseContext.parse(parseContext.createURI(path));
		} catch (RuntimeException e) {
			log.error("模板解析失败", e);
			throw e;
		}
		return parseContext.toList();
	}

	static class Info {
		File[] files;
		long lastModified;
		Info(List<File> files) {
			this.files = files.toArray(new File[files.size()]);
			this.lastModified = getLastModified(this.files);
		}
		boolean isModified() {
			return this.lastModified != getLastModified(files);
		}
		long getLastModified(File[] files) {
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
	}


}
