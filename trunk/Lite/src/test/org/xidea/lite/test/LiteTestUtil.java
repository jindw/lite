package org.xidea.lite.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseContext;
import org.xml.sax.SAXException;

public class LiteTestUtil {
	private static boolean useJS = false;
	private static JSIRuntime rt = RuntimeSupport.create();
	static{
		try{
		rt.eval("$import('org.xidea.lite.impl.*')");
		rt.eval("$import('org.xidea.lite.parse.*')");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Test
	public void test(){
		rt.eval("$import('org.xidea.lite.util.stringifyJSON')");
	}

	public static ParseContext buildParseContext(URI base) {
		if (useJS) {
			Object rb = rt.eval("new ParseContext(new ParseConfig('"+base+"',null),null)");
			ParseContext context = rt.wrapToJava(rb, ParseContext.class);
			return context;

		} else {
			if(base == null){
				base = new File("/").toURI();
			}
			ParseContextImpl context = new ParseContextImpl(
					new ParseConfigImpl(base,null), "/");
//			context.setCurrentURI(base);
			return context;
		}
	}

	public static List<Object> parse(URI uri) {
		ParseContext parseContext = LiteTestUtil.buildParseContext(uri);
		parseContext.parse(uri);
		return parseContext.toList();
	}

	public static List<Object> parse(String source) throws SAXException, IOException {
		ParseContext parseContext = LiteTestUtil.buildParseContext(URI
				.create("classpath:///"));
		parseContext.parse(ParseUtil.loadXML(source));
		return parseContext.toList();
	}

	public static Document loadXML(String source, ParseContext parseContext) {
		try {
			if (parseContext == null) {
				parseContext = LiteTestUtil.buildParseContext(URI
						.create("classpath:///"));
			}
			return ParseUtil.loadXML(source);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
