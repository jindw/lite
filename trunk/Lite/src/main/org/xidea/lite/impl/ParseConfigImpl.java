package org.xidea.lite.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
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
import org.xidea.lite.LiteTemplate;
import org.xidea.lite.parse.ParseConfig;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <pre>
 * {
 *   "includes":"^[\\\\/]example[\\\\/][^\\\\/]*\.xhtml$", 
 *   "excludes":"^$",//不匹配
 *   "featureMap":{ "http://www.xidea.org/lite/features/layout":"/layout.xhtml",
 *     "http://www.xidea.org/lite/features/encoding":"utf-8",
 *     "http://www.xidea.org/lite/features/content-type":"text/html",
 *     "http://www.xidea.org/feature/lite/html-javascript-compressor":"org.jside.jsi.tools.JSACompressor" 
 *   }, 
 *   "extensionMap":[]
 * }
 * </pre>
 * 
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

	private transient Map<String, Group> cached = new java.util.WeakHashMap<String, Group>();

	public ParseConfigImpl(URI root, URI config) {
		this.root = root;
		this.config = config;
		groups.add(DEFAULT_GROUP);
		if (config != null && config.getScheme().equals("file")) {
			File checkFile = new File(config.getPath());
			this.checkFile = checkFile;
		}
	}

	public URI getRoot() {
		return root;
	}

	public final Collection<URI> getResources() {
		Collection<URI> result = new ArrayList<URI>();
		if (config != null) {
			result.add(config);
		}
		return result;
	}

	protected Group find(String path, boolean defaultRoot) {
		this.reset();
		Group g = cached.get(path);
		if (g == null) {
			for (Group f : this.groups) {
				if (f.match(path)) {
					g = f;
					break;
				}
			}
			if (g == null && defaultRoot) {
				g = this.groups.get(this.groups.size() - 1);
			}
			cached.put(path, g);
		}
		return g;
	}
	public boolean isTemplate(String path){
		return find(path, false) != null;
	}

	public Map<String, List<String>> getExtensions(String path) {
		return find(path, true).extensionMap;
	}

	public Map<String, String> getFeatureMap(String path) {
		return find(path, true).featureMap;
	}

	private long lastModified() {
		return checkFile == null ? 0 : checkFile.lastModified();
	}

	protected void reset() {
		if (lastModified != this.lastModified()) {
			if (config != null) {
				if (checkFile == null || checkFile.exists()) {
					reset(new InputSource(config.toString()));
				}
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
			log.fatal("装饰配置解析失败:", e);
			reset((Document) null);
		}
	}

	protected void reset(Document doc) {
		JSIRuntime rt = ParseUtil.getJSIRuntime();
		Object parseConfig = rt.eval("(function(doc){var om = {};"
				+ "$export('org/xidea/lite/parse',om);"
				+ "$export('org/xidea/lite/util',om);"
				+ "return om.stringifyJSON(om.parseConfig(doc));})");
		String json = (String) rt.invoke(null, parseConfig, doc);
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
		this.cached.clear();
	}

	protected static class Group {
		protected Map<String, String> featureMap = Collections.emptyMap();
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
			this.featureMap = new HashMap<String, String>();
			this.featureMap.put(LiteTemplate.FEATURE_ENCODING, "UTF-8");
			this.featureMap.put(LiteTemplate.FEATURE_CONTENT_TYPE, "text/html;charset=UTF-8");
			this.featureMap = Collections.unmodifiableMap(this.featureMap);
		}

		@SuppressWarnings("unchecked")
		public Group(Map<String, Object> item) {
			this.includes = buildMatch((String) item.get("includes"));
			this.excludes = buildMatch((String) item.get("excludes"));
			this.featureMap = Collections
					.unmodifiableMap((Map<String, String>) item
							.get("featureMap"));
			this.extensionMap = Collections
					.unmodifiableMap((Map<String, List<String>>) item
							.get("extensionMap"));
		}

		private Pattern buildMatch(String includes) {
			return Pattern.compile(includes == null ? "^$" : includes);
		}

	}

	/**
	 * 如果file相于根目录（/path/...），以base作为根目录处理 否则以parentURI，或者base作为parent直接new
	 * URL处理。
	 * 
	 * @param file
	 * @param parentURI
	 * @see org.xidea.lite.impl.ParseContextImpl#createURI
	 * @return
	 */
	protected InputStream openStream(URI uri) throws IOException {
		if("lite".equals(uri.getScheme())){
			String path = uri.getPath();
			if(path.startsWith("/")){
				path = path.substring(1);
			}
			uri = this.getRoot().resolve(path);
		}
		return ParseUtil.openStream(uri);
	}


	public String loadText(URI uri) throws IOException {
		return ParseUtil.loadTextAndClose(this.openStream(uri),null);
	}

	public Document loadXML(URI uri) throws SAXException, IOException {
		try{

			String text = ParseUtil.loadXMLSourceAndClose(this.openStream(uri),null);
			String id = uri.toString();
			text = ParseUtil.normalize(text, id);
			return ParseUtil.loadXMLBySource(text, id);
		}catch (SAXException e) {
			log.error("XML 解析失败："+uri,e);
			throw e;
		}catch (IOException e) {
			log.error("模板读取失败："+uri,e);
			throw e;
		}catch (RuntimeException e) {
			log.error("XML装载失败："+uri,e);
			throw e;
		}
	}
}
