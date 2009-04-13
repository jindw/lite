package org.xidea.lite.parser;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class DecoratorMapper {
	private URLMatcher excludeMatcher;
	private Map<URLMatcher, String> decoratorMap;

	protected DecoratorMapper() {

	}

	public DecoratorMapper(InputStream config) {
		this(new InputSource(config));
	}
	public DecoratorMapper(Reader config) {
		this(new InputSource(config));
	}
	protected DecoratorMapper(InputSource config) {
		try {
			Document configDoc = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder().parse(config);
			initialize(configDoc);
		} catch (Exception e) {
			throw new RuntimeException("装饰配置解析失败", e);
		}
	}

	private void initialize(Document configDoc) {
		Map<URLMatcher, String> map = new HashMap<URLMatcher, String>();
		List<URLMatcher> excludes = new ArrayList<URLMatcher>();
		NodeList patterns = configDoc.getElementsByTagName("pattern");
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

	public String getDecotatorPage(String path) {
		if (this.excludeMatcher != null) {
			if (this.excludeMatcher.match(path)) {
				return null;
			}
		}
		if (this.decoratorMap != null) {
			for (Map.Entry<URLMatcher, String> entry : decoratorMap.entrySet()) {
				if (entry.getKey().match(path)) {
					return entry.getValue();
				}
			}
		}
		return null;
	}

}
