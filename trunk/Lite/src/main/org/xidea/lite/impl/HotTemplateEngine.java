package org.xidea.lite.impl;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.Template;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.ResourceContext;

public class HotTemplateEngine extends TemplateEngine {
	//public static final String DEFAULT_DECORATOR_MAPPING = "/WEB-INF/decorators.xml";
	private static final Log log = LogFactory.getLog(HotTemplateEngine.class);
	private HashMap<String, Info> infoMap = new HashMap<String, Info>();
	protected ParseConfig config;

	public HotTemplateEngine(URI webRoot, URI config) {
		super(webRoot);
		if (config!=null) {
			this.config = new ParseConfigImpl(
					config);
		}
	}
	public HotTemplateEngine(ResourceContext webRoot, ParseConfig context) {
		super(webRoot);
		if (context!=null) {
			this.config = context;
		}
	}

	protected ParseContext createParseContext(String path) {
		return new ParseContextImpl(path,base, config);
	}
	@Override
	protected Template createTemplate(String path) {
		ParseContext parseContext = createParseContext(path);
		Template template = createTemplate(path, parseContext);
		File[] files = getAssociatedFiles(parseContext);
		Info entry = new Info(files);
		infoMap.put(path, entry);
		return template; 
	}

	protected Template createTemplate(String path, ParseContext parseContext) {
		try {
			parseContext.parse(base.createURI(path, null));
		} catch (Exception e) {
			log.error("模板解析失败", e);
			StringWriter out = new StringWriter();
			out.append("模板编译失败：\r\n<hr>");
			PrintWriter pout = new PrintWriter(out, true);
			e.printStackTrace(pout);
			parseContext.append(out.toString());
		}
		List<Object> items = parseContext.toList();
		return new Template(items);
	}


	@Override
	public boolean isModified(String path) {
		Info templateEntry = (Info) infoMap.get(path);
		return templateEntry == null || templateEntry.isModified();
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
		if(config instanceof ParseConfigImpl){
			File configFile = ((ParseConfigImpl)config).getFile();
			if(configFile!=null){
				files.add(configFile);
			}
		}
		for (URI url : context.getResources()) {
			if ("file".equals(url.getScheme())) {
				files.add(new File(url.getPath()));
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

}
