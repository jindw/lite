package org.xidea.lite.impl.old;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Element;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.TextParser;

public class ExtensionParserImpl implements NodeParser<Element> {
	public void parse(Element el, ParseContext context, ParseChain chain) {
		String script = el.getTextContent();
		String src = ParseUtil.getAttributeOrNull(el, "src", "href", "uri",
				"url");

		if (src != null && src.length() > 0) {
			for (String s : src.split("[\\s*,\\s*]")) {
				URI uri = context.createURI(s);
				processResource(uri,context);
			}
		}
		if (script != null && script.trim().length() > 0) {
			processText(script,context);
		}
	}

	protected void processResource(URI uri, ParseContext context) {
			context.addResource(uri);
			JSIRuntime proxy = context.getAttribute(JSIRuntime.class);
			if(proxy == null){
				proxy =RuntimeSupport.create();
				context.setAttribute(JSIRuntime.class,proxy);
			}
			InputStream in = context.openStream(uri);
			if (in != null) {
				try {
					String script = ParseUtil.loadText(in, "utf-8");
					proxy.eval(context, script, this.getClass().getName(),null);
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

	private void processText(String script,ParseContext context) {
		JSIRuntime proxy = context.getAttribute(JSIRuntime.class);
		if(proxy == null){
			proxy =RuntimeSupport.create();
			context.setAttribute(JSIRuntime.class,proxy);
		}
		proxy.eval(context, script, this.getClass().getName(), null);
	}

	public static class JSContextProxy extends ParseContextProxy implements
			ParseContext {
		private ParseContext context;
		private JSIRuntime proxy;

		public JSContextProxy(ParseContext parent, JSIRuntime proxy) {
			super(parent);
			this.context = parent;
			this.proxy = proxy;
		}

		public void addNodeParser(Object function) {
			NodeParser<? extends Object> nodeParser = createNodeParser(function);
			context.addNodeParser(nodeParser);
		}

//		public void addTextParser(Object function) {
//			TextParser nodeParser = createTextParser(function);
//			context.addTextParser(nodeParser);
//		}

		public TextParser createTextParser(Object o) {
			HashMap<String, Object> varMap = new HashMap<String, Object>();
			varMap.put("impl", o);
			proxy
					.eval(
							null,
							"if(impl instanceof Function){impl.parse=impl,impl.findStart=impl}"
									+ "if(!impl.getPriority) {"
									+ "impl.getPriority=function(){"
									+ "return impl.priority == null? 1 : impl.priority;"
									+ "}};", this.getClass().toString(), varMap);
			return proxy.wrapToJava(o, TextParser.class);
		}

		@SuppressWarnings("unchecked")
		public NodeParser<? extends Object> createNodeParser(Object o) {
			HashMap<String, Object> varMap = new HashMap<String, Object>();
			varMap.put("impl", o);
			proxy.eval(null, "if(impl instanceof Function){impl.parse=impl};",
					this.getClass().toString(), varMap);
			return proxy.wrapToJava(o, NodeParser.class);
		}

		public String getFeatrue(String key) {
			return context.getFeatrue(key);
		}

		public void parse(Object source) {
			context.parse(source);
		}

		public List<Object> parseText(String text, int textType) {
			return context.parseText(text, textType);
		}
	}
}
