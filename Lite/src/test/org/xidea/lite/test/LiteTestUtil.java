package org.xidea.lite.test;

import java.net.URI;
import java.util.List;

import org.w3c.dom.Document;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ParseUtil;

public class LiteTestUtil {

	public static ParseContextImpl buildParseContext(URI base) {
		ParseContextImpl context = new ParseContextImpl(new ParseConfigImpl(base),"/");
		context.setCurrentURI(base);
		return context;
	}

	public static List<Object> parse(URI uri) {
		ParseContextImpl parseContext = LiteTestUtil.buildParseContext(uri);
		parseContext.parse(uri);
		return parseContext.toList();
	}

	public static List<Object> parse(String source) {
		URI uri = ParseUtil.createSourceURI(source);
		ParseContextImpl parseContext = LiteTestUtil.buildParseContext(uri);
		parseContext.parse(loadXML(source, parseContext));
		return parseContext.toList();
	}

	public static Document loadXML(String source, ParseContextImpl parseContext) {
		try {
			URI uri = ParseUtil.createSourceURI(source);
			if (parseContext == null) {
				parseContext = LiteTestUtil.buildParseContext(URI.create("classpath:///"));
			}
			return parseContext.loadXML(uri);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
