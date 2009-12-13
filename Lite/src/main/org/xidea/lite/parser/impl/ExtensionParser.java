package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.xidea.lite.parser.NodeParser;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.TextParser;

public class ExtensionParser implements NodeParser<Element> {
	public void parse(Element el, ParseContext context, ParseChain chain) {
		String script = el.getTextContent();
		JSProxy proxy = JSProxy.newProxy();
		Map<String, Object> varMap = new HashMap<String, Object>();
		varMap.put("context", new JSContextProxy(context, proxy));
		String src = ParseUtil.getAttributeOrNull(el, "src", "href", "uri",
				"url");

		if (src != null && src.length() > 0) {
			processResource(src.split("[\\s*,\\s*]"), context,proxy, varMap);
		}
		if (script != null && script.trim().length() > 0) {
			processText(script, proxy, varMap);
		}
	}

	public void processResource(String paths, ParseContext context) {
		JSProxy proxy = JSProxy.newProxy();
		Map<String, Object> varMap = new HashMap<String, Object>();
		varMap.put("context", new JSContextProxy(context, proxy));
		processResource(paths.split("[\\s*,\\s*]"), context, proxy, varMap);

	}

	private void processResource(String[] paths, ParseContext context,
			JSProxy proxy, Map<String, Object> varMap) {
		for (String path : paths) {
			URI uri = context.createURI(path, null);
			context.addResource(uri);
			InputStream in = context.openInputStream(uri);
			if (in != null) {
				try {
					String script = ParseUtil.loadText(in, "utf-8");
					proxy.eval(script, this.getClass().getName(), varMap);
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					try {
						in.close();
					} catch (Exception e) {
					}
				}
			}
		}
	}

	public void processText(String text, ParseContext context) {
		JSProxy proxy = JSProxy.newProxy();
		Map<String, Object> varMap = new HashMap<String, Object>();
		varMap.put("context", new JSContextProxy(context, proxy));
		proxy.eval(text, this.getClass().getName(), varMap);
	}

	private void processText(String script, JSProxy proxy,
			Map<String, Object> varMap) {
		proxy.eval(script, this.getClass().getName(), varMap);
	}

	public static class JSContextProxy extends ParseContextProxy implements
			ParseContext {
		private ParseContext context;
		private JSProxy proxy;

		public JSContextProxy(ParseContext parent, JSProxy proxy) {
			super(parent);
			this.context = parent;
			this.proxy = proxy;
		}

		public void addNodeParser(Object function) {
			NodeParser<? extends Object> nodeParser = proxy
					.createNodeParser(function);
			context.addNodeParser(nodeParser);
		}

		public void addTextParser(Object function) {
			TextParser nodeParser = proxy.createTextParser(function);
			context.addTextParser(nodeParser);
		}

		public String getFeatrue(String key) {
			return context.getFeatrue(key);
		}

		public Map<String, String> getFeatrueMap() {
			return context.getFeatrueMap();
		}

		public void parse(Object source) {
			context.parse(source);
		}

		public List<Object> parseText(String text, int textType) {
			return context.parseText(text, textType);
		}

	}
}
