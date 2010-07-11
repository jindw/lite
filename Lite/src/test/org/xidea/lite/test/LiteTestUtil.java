package org.xidea.lite.test;

import java.net.URI;
import java.util.List;

import org.w3c.dom.Document;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseContext;

public class LiteTestUtil {
	private static boolean useJS = true;
	private static JSIRuntime rt = RuntimeSupport.create();
	static{
		rt.eval("$import('org.xidea.lite.impl.*')");
		rt.eval("$import('org.xidea.lite.parse.*')");
	}

	public static ParseContext buildParseContext(URI base) {
		if (useJS) {
			Object rb = rt.eval("new ParseContext(null,'/')");
			ParseContext context = rt.wrapToJava(rb, ParseContext.class);
			return context;

		} else {
			ParseContextImpl context = new ParseContextImpl(
					new ParseConfigImpl(base), "/");
			context.setCurrentURI(base);
			return context;
		}
	}

	public static List<Object> parse(URI uri) {
		ParseContext parseContext = LiteTestUtil.buildParseContext(uri);
		parseContext.parse(uri);
		return parseContext.toList();
	}

	public static List<Object> parse(String source) {
		URI uri = ParseUtil.createSourceURI(source);
		ParseContext parseContext = LiteTestUtil.buildParseContext(uri);
		parseContext.parse(loadXML(source, parseContext));
		return parseContext.toList();
	}

	public static Document loadXML(String source, ParseContext parseContext) {
		try {
			URI uri = ParseUtil.createSourceURI(source);
			if (parseContext == null) {
				parseContext = LiteTestUtil.buildParseContext(URI
						.create("classpath:///"));
			}
			return parseContext.loadXML(uri);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
