package org.xidea.lite.parser.impl;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xidea.lite.parser.DecoratorContext;
import org.xml.sax.InputSource;

public class DecoratorContextImpl implements DecoratorContext {
	private static final Log log = LogFactory.getLog(DecoratorContextImpl.class);
	protected URLMatcher excludeMatcher;
	protected Map<URLMatcher, String> decoratorMap;
	protected long lastModified = -1;// not found or error :0
	protected URI config;
	protected File checkFile;

	public DecoratorContextImpl(URI config,File checkFile) {
		if (checkFile == null || checkFile.exists()) {
			this.config = config;
			this.checkFile = checkFile;
		}
	}

	protected long lastModified() {
		return checkFile == null ? 0 : checkFile.lastModified();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xidea.lite.parser.DecoratorMapperI#getDecotatorPage(java.lang.String)
	 */
	public String getDecotatorPage(String path) {
		if (lastModified != this.lastModified()) {
			this.reset();
			this.lastModified = this.lastModified();
		}
		if (this.excludeMatcher != null) {
			if (this.excludeMatcher.match(path)) {
				return null;
			}
		}
		if (this.decoratorMap != null) {
			for (Map.Entry<URLMatcher, String> entry : decoratorMap.entrySet()) {
				if (entry.getKey().match(path)) {
					String decorator = entry.getValue();
					log.info("装饰器配置："+path+"->"+decorator);
					return path.equals(decorator)?null:decorator;
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
			throw new RuntimeException("装饰配置解析失败", e);
		}
	}

	protected void reset(Document config) {
		Map<URLMatcher, String> map = new HashMap<URLMatcher, String>();
		List<URLMatcher> excludes = new ArrayList<URLMatcher>();
		NodeList patterns = config.getElementsByTagName("pattern");
		for (int i = 0; i < patterns.getLength(); i++) {
			Element patternEl = (Element) patterns.item(i);
			Element parentEl = (Element) patternEl.getParentNode();
			String pattern = patternEl.getTextContent().trim();
			if ("excludes".equals(parentEl.getTagName())) {
				excludes.add(URLMatcher.createMatcher(pattern));
			} else if ("decorator".equals(parentEl.getTagName())) {
				String layout = parentEl.getAttribute("layout");
				if (layout.length() == 0) {
					layout = parentEl.getAttribute("page");// 兼容sitemesh
				}
				map.put(URLMatcher.createMatcher(pattern), layout);
			}
		}
		ArrayList<URLMatcher> list = new ArrayList<URLMatcher>(map.keySet());
		Collections.sort(list);
		LinkedHashMap<URLMatcher, String> decoratorMap = new LinkedHashMap<URLMatcher, String>();

		for (URLMatcher matcher : list) {
			decoratorMap.put(matcher, map.get(matcher));
		}
		this.decoratorMap = decoratorMap;
		this.excludeMatcher = URLMatcher.createOrMatcher(excludes
				.toArray(new URLMatcher[excludes.size()]));
	}

}
