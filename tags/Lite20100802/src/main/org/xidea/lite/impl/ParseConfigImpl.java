package org.xidea.lite.impl;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xidea.el.json.JSONDecoder;
import org.xidea.jsi.JSIRuntime;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
import org.xml.sax.InputSource;

/**
 * "includes":"^[\\\\/]example[\\\\/][^\\\\/]*\.xhtml$", "excludes":"",
 * "featrueMap":{ "http://www.xidea.org/featrues/lite/layout":"/layout.xhtml",
 * "http://www.xidea.org/featrues/lite/output-encoding":"utf-8",
 * "http://www.xidea.org/featrues/lite/output-mime-type":"text/html",
 * "http://www.xidea.org/featrue/lite/html-javascript-compressor"
 * :"org.jside.jsi.tools.JSACompressor" }, "extensionMap":[]
 * 
 * @author jindawei
 * 
 */
public class ParseConfigImpl implements ParseConfig {
	private static final Log log = LogFactory.getLog(ParseConfigImpl.class);
	private static Group DEFAULT_GROUP = new Group();

	// config data
	protected long lastModified = -2;// not found or error :0
	private URI root;
	protected URI config;
	protected File checkFile;
	private List<Group> groups = new ArrayList<Group>();

	public ParseConfigImpl(URI root, URI config) {
		this.root = root;
		this.config = config;
		groups.add(DEFAULT_GROUP);
		if (config != null && config.getScheme().equals("file")) {
			File checkFile = new File(config.getPath());
			this.checkFile = checkFile;
		}
	}

//	protected ParseConfigImpl(URI root) {
//		this.root = root;
//		groups.add(DEFAULT_GROUP);
//	}

	public URI getRoot() {
		return root;
	}

	private Group find(String path, boolean defaultRoot) {
		for (Group f : this.groups) {
			if (f.match(path)) {
				return f;
			}
		}
		if (defaultRoot) {
			return this.groups.get(this.groups.size() - 1);
		} else {
			return null;
		}
	}

	public Map<String, List<String>> getExtensions(String path) {
		return find(path, true).extensionMap;
	}

	public Map<String, String> getFeatrueMap(String path) {
		return find(path, true).featrueMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.xidea.lite.parser.DecoratorMapperI#getDecotatorPage(java.lang.String)
	 */
	public String getDecotatorPage(String path) {
		this.reset();
		Group g = find(path, false);
		if (g != null) {
			return g.featrueMap.get(ParseContext.FEATRUE_CONFIG_LAYOUT);
		}
		return null;
	}

	File getFile() {
		return checkFile;
	}

	protected long lastModified() {
		return checkFile == null ? 0 : checkFile.lastModified();
	}

	protected void reset() {
		if (lastModified != this.lastModified()) {
			if (config != null) {
				reset(new InputSource(config.toString()));
			}
			this.lastModified = this.lastModified();
		}
	}

	protected void reset(InputSource source) {
		try {
			Document doc = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder().parse(source);
			reset(doc);
		} catch (Exception e) {
			log.fatal("装饰配置解析失败:" + e.getMessage());
			reset((Document) null);
		}
	}

	protected void reset(Document doc) {
		JSIRuntime rt = ParseUtil.getJSIRuntime();
		Object parse = rt
				.eval("function(stringifyJSON,parseConfig,doc){return stringify((doc))}");
		Object parseConfig = rt
				.eval("$import('org.xidea.lite.parse:parseConfig',{})");
		Object stringifyJSON = rt
				.eval("$import('org.xidea.lite.util:stringifyJSON',{})");
		String json = (String) rt.invoke(null, parse, stringifyJSON,
				parseConfig, doc);
		List<Map<String, Object>> groups = JSONDecoder.decode(json);
		List<Group> groups2 = new ArrayList<Group>();
		if (groups.size() > 0) {
			for (Map<String, Object> item : groups) {
				groups2.add(new Group(item));
			}
		} else {
			groups2.add(DEFAULT_GROUP);
		}
		this.groups = groups2;
	}

	private static class Group {
		private Map<String, String> featrueMap = Collections.emptyMap();
		private Map<String, List<String>> extensionMap = Collections.emptyMap();
		private Pattern includes;
		private Pattern excludes;

		boolean match(String path) {
			return includes.matcher(path).find()
					&& !excludes.matcher(path).find();
		}

		private Group() {
			this.includes = Pattern.compile("\\.xhtml$");
			this.excludes = Pattern.compile("^[\\.^]");
			this.featrueMap = new HashMap<String, String>();
			this.featrueMap.put(ParseContext.FEATRUE_ENCODING, "UTF-8");
			this.featrueMap = Collections.unmodifiableMap(this.featrueMap);
		}

		@SuppressWarnings("unchecked")
		public Group(Map<String, Object> item) {
			this.includes = buildMatch((String) item.get("includes"));
			this.excludes = buildMatch((String) item.get("excludes"));
			this.featrueMap = Collections
					.unmodifiableMap((Map<String, String>) item
							.get("featrueMap"));
			this.extensionMap = Collections
					.unmodifiableMap((Map<String, List<String>>) item
							.get("extensionMap"));
		}

		private Pattern buildMatch(String includes) {
			if (includes == null || includes.length() == 0) {
				includes = "^$";
			}
			return Pattern.compile(includes);
		}

	}

}
