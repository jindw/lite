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
	private URI compiledBase;
	private boolean checkFile;

	public HotTemplateEngine(URI base) {
		super(base);
	}

	public HotTemplateEngine(URI root, URI config) {
		this(new ParseConfigImpl(root, config));
	}

	public HotTemplateEngine(ParseConfig config) {
		super(config.getRoot());
		this.checkFile = true;
		this.config = config;
	}

	public void setCompiledBase(URI compiledBase) {
		this.compiledBase = compiledBase;
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

	public String getLiteCode(String path) {
		try {
			if(buildFromCode(path) != null){
				URI uri = toCompiedURI(path);
				return ParseUtil.loadTextAndClose(ParseUtil.openStream(uri),"utf-8");
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
			Template t =  buildFromCode(path);
			if(t!=null){
				return t;
			}
		}
		return buildFromSource(path);
	}

	private Template buildFromSource(final String path) throws IOException {
		long begin = System.currentTimeMillis();
		ArrayList<File> files = new ArrayList<File>();
		ParseContext context = createParseContext(path);
		List<Object> items = parse(path, context);
		if ("file".equals(config.getRoot().getScheme())) {
			File base = new File(config.getRoot());
			for (String p : getAssociatedPaths(context)) {
				files.add(new File(base, p));
			}
		}
		Template template = new Template(items);
		
		if(compiledBase!=null && ParseUtil.isFile(compiledBase)){
			File file = new File(toCompiedURI(path));
			file.getParentFile().mkdirs();
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream(file), "UTF-8");
			out.write(buildLiteCode(context, items));
			out.close();
		}
		long end = System.currentTimeMillis();
		log.info("lite compile success;\t time used: "+(end - begin)+"ms;\t resource dependence："+path+":\t"+files);
		Info entry = new Info(files);
		infoMap.put(path, entry);
		return template;
	}
	private URI toCompiedURI(String path) throws UnsupportedEncodingException{
		path =  path.replace('/', '^');
		return this.compiledBase.resolve(URLEncoder.encode(path, "UTF-8"));
		
	}

	@SuppressWarnings("unchecked")
	private Template buildFromCode(String path) throws IOException {
		if(this.compiledBase == null){
			return null;
		}
		URI uri = toCompiedURI(path);
		InputStream in = ParseUtil.openStream(uri);
		if(in == null){
			return null;
		}
		String litecode = ParseUtil.loadTextAndClose(in,"utf-8");
		try {
			ArrayList<File> files = new ArrayList<File>();
			List<Object> list = JSONDecoder.decode(litecode);
			if (ParseUtil.isFile(uri)) {
				File lc = new File(uri);
				files.add(lc);
				if(checkFile){
					List<String> resource = (List<String>) list.get(0);
					long lm = 0;
					File root = new File(this.root);
					for(String res:resource){
						File f = new File(root,res);
						long l = f.lastModified();
						files.add(f);
						if(l > lm){
							lm = l;
						}else if(l == 0){
							return null;
						}
					}
					if(lm>lc.lastModified()){
						return null;
					}
				}
				Info entry = new Info(files);
				log.info("文件关联："+path+":\t"+files);
				infoMap.put(path, entry);
			}
			return new Template((List<Object>) list.get(1));
		} catch (RuntimeException e) {
			log.error("装载模板中间代码失败", e);
			return null;
		}
	}

	private String buildLiteCode(ParseContext context, List<Object> items) {
		ArrayList<Object> result = new ArrayList<Object>();
		List<String> resource = getAssociatedPaths(context);
		result.add(resource);
		result.add(items);
		result.add(context.getFeatureMap());
		return JSONEncoder.encode(result);

	}
	private List<String> getAssociatedPaths(ParseContext context) {
		ArrayList<String> resource = new ArrayList<String>();
		String root = config.getRoot().toString();
		for (URI uri : context.getResources()) {
			if ("lite".equals(uri.getScheme())) {
				resource.add(uri.getPath());
			} else {
				String config = uri.toString();
				if (config.startsWith(root)) {
					config = config.substring(root.length());
					if (config.length() > 0) {
						if (config.charAt(0) != '/') {
							config = '/' + config;
						}
						resource.add(config);
					}
				}else{// if("file".equals(uri.getScheme())){
					log.warn("忽略关联文件:" + uri);
				}
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
