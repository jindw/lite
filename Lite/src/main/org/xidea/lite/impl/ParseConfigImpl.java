package org.xidea.lite.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.TextParser;
import org.xml.sax.InputSource;

public class ParseConfigImpl implements ParseConfig {
	private static final Log log = LogFactory.getLog(ParseConfigImpl.class);
	// config data
	protected Map<URIMatcher, String> includeMap;
	protected Map<URIMatcher, String> excludeMap;
	private Map<String, String> featrueMap;
	private boolean debugModel;
	private TextParser[] textParsers;
	private NodeParser<? extends Object>[] nodeParsers;
	// check data
	protected long lastModified = -2;// not found or error :0
	private URI root;
	protected URI config;
	protected File checkFile;

	public ParseConfigImpl(URI root, URI config) {
		this.root = root;
		this.config = config;
		if (config.getScheme().equals("file")) {
			File checkFile = new File(config.getPath());
			this.checkFile = checkFile;
		}
	}

	public ParseConfigImpl(URI root) {
		this.root = root;
	}

	public InputStream openStream(URI uri) {
		try {
			if ("data".equalsIgnoreCase(uri.getScheme())) {
				String data = uri.getRawSchemeSpecificPart();
				int p = data.indexOf(',') + 1;
				String h = data.substring(0, p).toLowerCase();
				String charset = "UTF-8";
				data = data.substring(p);
				p = h.indexOf("charset=");
				if (p > 0) {
					charset = h.substring(h.indexOf('=', p) + 1, h.indexOf(',',
							p));
				}
				return new ByteArrayInputStream(URLDecoder
						.decode(data, charset).getBytes(charset));
				// charset=
			} else if ("classpath".equalsIgnoreCase(uri.getScheme())) {
				ClassLoader cl = this.getClass().getClassLoader();
				uri = uri.normalize();
				String path = uri.getPath();
				path = path.substring(1);
				InputStream in = cl.getResourceAsStream(path);
				if (in == null) {
					ClassLoader cl2 = Thread.currentThread()
							.getContextClassLoader();
					if (cl2 != null) {
						in = cl2.getResourceAsStream(path);
					}
				}
				return in;
			} else {
				return uri.toURL().openStream();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public File getFile() {
		return checkFile;
	}

	protected long lastModified() {
		return checkFile == null ? 0 : checkFile.lastModified();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.xidea.lite.parser.DecoratorMapperI#getDecotatorPage(java.lang.String)
	 */
	public String getDecotatorPage(String path) {
		if (lastModified != this.lastModified()) {
			this.reset();
			this.lastModified = this.lastModified();
		}
		if (this.includeMap != null && excludeMap != null) {
			find: for (Map.Entry<URIMatcher, String> include : includeMap
					.entrySet()) {
				if (include.getKey().match(path)) {
					String match = include.getValue();
					log.info("装饰器配置：" + path + "->" + match);
					for (Map.Entry<URIMatcher, String> exclude : excludeMap
							.entrySet()) {
						if (exclude.getKey().match(path)) {
							if (match.equals(exclude.getValue())) {
								continue find;
							}
						}
					}
					return path.equals(match) ? null : match;
				}
			}
		}
		return null;
	}

	protected void reset() {
		if (config != null) {
			reset(new InputSource(config.toString()));
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

	protected void reset(Document config) {
		ConfigParser pc = new ConfigParser();
		if (config != null) {
			NodeList els = config.getElementsByTagName("*");
			for (int i = 0; i < els.getLength(); i++) {
				pc.parse((Element) els.item(i));
			}
		}
		this.debugModel = pc.debugModel;
		this.excludeMap = pc.getExcludeMap();
		this.includeMap = pc.getIncludeMap();
		this.featrueMap = pc.featrueMap;
		this.nodeParsers = null;
		this.textParsers = null;
	}

	public Map<String, String> getFeatrueMap(String path) {
		return featrueMap;
	}

	public NodeParser<? extends Object>[] getNodeParsers(String path) {
		return nodeParsers;
	}

	public TextParser[] getTextParsers(String path) {
		return textParsers;
	}

	public boolean isDebugModel() {
		return debugModel;
	}

	public URI getRoot() {
		return root;
	}


}

class ConfigParser implements NodeParser<Element> {
	boolean debugModel;
	ArrayList<String> layout = new ArrayList<String>();
	Map<URIMatcher, Integer> includeMap = new HashMap<URIMatcher, Integer>();
	Map<URIMatcher, Integer> excludeMap = new HashMap<URIMatcher, Integer>();

	ArrayList<URIMatcher> defaultExclude = new ArrayList<URIMatcher>();
	Map<String, String> featrueMap = new HashMap<String, String>();

	public void parse(Element node, ParseContext context, ParseChain chain) {
		parse(node);
		chain.process(node);
	}

	public void parse(Element node) {
		String localName = node.getNodeName();
		if ("lite".equals(localName)) {
			paseLite(node);
		} else if ("group".equals(localName)) {
			paseGroup(node);
			for (URIMatcher match : defaultExclude) {
				excludeMap.put(match, layout.size() - 1);
			}
		} else if ("include".equals(localName)) {
			paseInclude(node);
		} else if ("exclude".equals(localName)) {
			paseExclude(node);
		} else if ("featrue".equals(localName)) {
			paseFeatrue(node);
		}

	}

	public Map<URIMatcher, String> getExcludeMap() {
		return buildMap(excludeMap);
	}

	public Map<URIMatcher, String> getIncludeMap() {
		return buildMap(includeMap);
	}

	public Map<URIMatcher, String> buildMap(Map<URIMatcher, Integer> map) {
		ArrayList<URIMatcher> list = new ArrayList<URIMatcher>(map.keySet());
		Collections.sort(list);
		LinkedHashMap<URIMatcher, String> result = new LinkedHashMap<URIMatcher, String>();
		for (URIMatcher matcher : list) {
			result.put(matcher, layout.get(map.get(matcher)));
		}
		return result;
	}

	private void paseFeatrue(Element el) {
		String key = ParseUtil.getAttributeOrNull(el, "key", "name");
		String value = ParseUtil.getAttributeOrNull(el, "value", "#text");
		featrueMap.put(key, value);
	}

	private void paseGroup(Element el) {
		layout.add(ParseUtil.getAttributeOrNull(el, "layout", "page"));
	}

	private void paseInclude(Element el) {
		includeMap.put(URIMatcher.createMatcher(el.getTextContent()), layout
				.size() - 1);
	}

	private void paseExclude(Element el) {
		URIMatcher match = URIMatcher.createMatcher(el.getTextContent());
		if (layout.isEmpty()) {
			defaultExclude.add(match);
		} else {
			excludeMap.put(match, layout.size() - 1);
		}
	}

	private void paseLite(Element el) {
		String debug = ParseUtil.getAttributeOrNull(el, "debug-model",
				"debugModel");
		if ("true".equals(debug) || "on".equals(debug)) {
			debugModel = true;
		} else if (debug == null) {
			debugModel = true;
		} else {
			debugModel = false;
		}
	}

}
