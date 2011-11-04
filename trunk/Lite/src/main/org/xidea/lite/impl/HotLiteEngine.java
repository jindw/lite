package org.xidea.lite.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.LiteTemplate;
import org.xidea.lite.LiteEngine;
import org.xidea.lite.Template;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;

public class HotLiteEngine extends LiteEngine {
	private static final String I18N_DATA_KEY = "#i18n-data";
	private static final Log log = LogFactory.getLog(HotLiteEngine.class);
	private HashMap<String, Object> lock = new HashMap<String, Object>();
	private HashMap<String, Info> infoMap = new HashMap<String, Info>();
	protected ParseConfig config;
	protected final boolean checkFile;

	public HotLiteEngine(URI root, URI config, URI compiledBase) {
		this(new ParseConfigImpl(root, config), compiledBase);
	}

	public HotLiteEngine(ParseConfig config, URI compiledBase) {
		super(compiledBase);
		URI root = config.getRoot();
		this.checkFile = isFile(root);
		this.config = config;
	}

	private boolean isFile(URI root) {
		return "file".equals(root.getScheme());
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

	public String getLitecode(String path) {
		try {
			if (buildFromCode(path) != null) {
				URI uri = toCompiedURI(path);
				return ParseUtil.loadTextAndClose(ParseUtil.openStream(uri),
						"utf-8");
			}
			ParseContext context = createParseContext(path);
			List<Object> items = parse(path, context);
			return buildLiteCode(context, items);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Template createTemplate(String path) throws IOException {
		if (compiledBase != null) {
			Template t = buildFromCode(path);
			if (t != null) {
				return t;
			}
		}
		return buildFromSource(path);
	}

	public void clear(String path) {
		templateMap.remove(path);
		if (compiledBase != null && ParseUtil.isFile(compiledBase)) {
			File file = new File(toCompiedURI(path));
			if (file.exists()) {
				file.delete();
			}
		}
	}

	private URI toCompiedURI(String path) {
		path = path.replace('/', '^');
		try {
			return this.compiledBase.resolve(URLEncoder.encode(path, "UTF-8"));
		} catch (UnsupportedEncodingException e) {// 不可能
			return null;
		}

	}

	private Template buildFromSource(final String path) throws IOException {
		long begin = System.currentTimeMillis();
		ParseContext context = createParseContext(path);
		List<Object> items = parse(path, context);
		List<File> files = getAssociatedFiles(context);

		HashMap<String, String> featureMap = new HashMap<String, String>(context.getFeatureMap());
		featureMap.put(LiteTemplate.FEATURE_I18N, (String)context.getAttribute(I18N_DATA_KEY));
		Template template = new LiteTemplate(items, featureMap);

		if (compiledBase != null && ParseUtil.isFile(compiledBase)) {
			File file = new File(toCompiedURI(path));
			try {
				file.getParentFile().mkdirs();
				OutputStreamWriter out = new OutputStreamWriter(
						new FileOutputStream(file), "UTF-8");
				out.write(buildLiteCode(context, items));
				out.close();
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("complie result(" + path + ") save failed", e);
				}
			}
		}
		long end = System.currentTimeMillis();
		log.info("lite compile success;\t time used: " + (end - begin)
				+ "ms;\t resource dependence：" + path + ":\t" + files);
		Info entry = new Info(files);
		infoMap.put(path, entry);
		return template;
	}

	@SuppressWarnings("unchecked")
	private Template buildFromCode(String path) throws IOException {
		if (this.compiledBase == null) {
			return null;
		}
		URI uri = toCompiedURI(path);
		InputStream in = ParseUtil.openStream(uri);
		if (in == null) {
			return null;
		}
		String litecode = ParseUtil.loadTextAndClose(in, "utf-8");
		try {
			ArrayList<File> files = new ArrayList<File>();
			List<Object> list = JSONDecoder.decode(litecode);
			if (ParseUtil.isFile(uri)) {
				File lc = new File(uri);
				files.add(lc);
				if (checkFile) {
					List<String> resource = (List<String>) list.get(0);
					long lm = 0;
					File root = new File(this.config.getRoot());
					for (String res : resource) {
						File f = new File(root, res);
						long l = f.lastModified();
						files.add(f);
						if (l > lm) {
							lm = l;
						} else if (l == 0) {
							return null;
						}
					}
					if (lm > lc.lastModified()) {
						return null;
					}
				}
				Info entry = new Info(files);
				log.info("文件关联：" + path + ":\t" + files);
				infoMap.put(path, entry);
			}
			return new LiteTemplate((List<Object>) list.get(1),
					(Map<String, String>) list.get(2));
		} catch (RuntimeException e) {
			log.error("装载模板中间代码失败", e);
			return null;
		}
	}

	private String buildLiteCode(ParseContext context, List<Object> items) {
		ArrayList<Object> result = new ArrayList<Object>();
		List<String> resource = toAssociatedPaths(getAssociatedFiles(context));
		result.add(resource);
		result.add(items);
		HashMap<String, Object> featureMap = new HashMap<String, Object>(context.getFeatureMap());
		featureMap.put(LiteTemplate.FEATURE_I18N, context.getAttribute(I18N_DATA_KEY));
		result.add(featureMap);
		return JSONEncoder.encode(result);

	}

	protected List<File> getAssociatedFiles(ParseContext context) {
		ArrayList<File> files = new ArrayList<File>();
		URI base = config.getRoot();
		for (URI uri : context.getResources()) {
			if ("lite".equals(uri.getScheme())) {
				uri = base.resolve(uri.getPath().replaceFirst("^[/]", ""));
			}
			if ("classpath".equals(uri.getScheme())) {
				try {
					String path = uri.getPath().replaceFirst("^[/]*", "/");
					uri = HotLiteEngine.class.getResource(path).toURI();
				} catch (Exception e) {
					continue;
				}
			}
			if (isFile(uri)) {
				files.add(new File(uri));
			}
		}
		return files;
	}

	private List<String> toAssociatedPaths(List<File> files) {
		ArrayList<String> resource = new ArrayList<String>();
		String root = config.getRoot().toString();
		for (File file : files) {
			String uri = file.toURI().toString();
			if (uri.startsWith(root)) {
				uri = uri.substring(root.length());
				if (uri.length() > 0) {
					if (uri.charAt(0) != '/') {
						uri = '/' + uri;
					}
					resource.add(uri);
				}
			} else {// if("file".equals(uri.getScheme())){
				log.warn("忽略关联文件:" + uri);
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
