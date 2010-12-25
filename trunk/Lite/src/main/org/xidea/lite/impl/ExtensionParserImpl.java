package org.xidea.lite.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.parse.ExtensionParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseContext;

public class ExtensionParserImpl implements ExtensionParser {

	private static Pattern pattern = Pattern.compile("^[\\w\\-]\\:|[\\-]");
	private static final Object CURRENT_NODE_KEY = new Object();
	private static Pattern FN_SEEKER = Pattern
			.compile("^(?:\\w*\\:)?\\w*[\\$\\{]");
	private Object impl;
	private final ExtensionParser proxy;
	private JSIRuntime rt = ParseUtil.getJSIRuntime();
	private Map<String, Map<String, Map<String, Object>>> packageMap;

	public ExtensionParserImpl() {
		this.impl = rt
				.eval("new ($import('org.xidea.lite.parse:ExtensionParser',{}))()");
		proxy = rt.wrapToJava(impl, ExtensionParser.class);
		reset();
	}

	public void addExtensionPackage(String namespace, String packageName) {
		proxy.addExtensionPackage(namespace, packageName);
		reset();
	}

	public void addExtensionObject(String namespace, Object parserMap) {
		proxy.addExtensionObject(namespace, parserMap);
		reset();
	}

	@SuppressWarnings("unchecked")
	private void reset() {
		Object obj = rt.invoke(this.impl, "mapJava", HashMap.class);
		packageMap = (Map<String, Map<String, Map<String, Object>>>) obj;
	}

	public int parseText(String text, int start, ParseContext context) {
		return proxy.parseText(text, start, context);
	}

	public int getPriority() {
		return 2;
	}

	public int findStart(String text, int start, int otherStart) {
		int begin = start;
		while (true) {
			begin = text.indexOf('$', begin);
			if (begin < 0 || otherStart <= begin) {
				return -1;
			}

			if (FN_SEEKER.matcher(text.substring(begin + 1)).find()) {
				return begin;
			}
			begin++;
		}
	}

	public void parse(Node node, ParseContext context, ParseChain chain) {
		int type = node.getNodeType();
		try {
			RuntimeSupport.setInfo(context.getCurrentURI().getPath());
			if (type == 9) {
				if (this.parseDocument(node, context, chain)) {
					return;
				}
			} else if (type == 2) {
				if (this.parseAttribute(node, context, chain)) {
					return;
				}
			} else if (type == 1) {
				if (this.parseElement((Element) node, context, chain)) {
					return;
				}
			}
		} finally {
			// RuntimeSupport.setInfo(null);
		}
		//System.out.println(((Element) node).getTagName());
		chain.next(node);
	}

	private boolean parseElement(Element el, ParseContext context,
			ParseChain chain) {
		context.setAttribute(CURRENT_NODE_KEY, el);
		String nns = getNS(el);
		if (parseBefore(el, context, chain)) {
			return true;
		}
		Map<String, Map<String, Object>> packageInfo = this.packageMap.get(nns);
		if (packageInfo != null) {
			Map<String, Object> parserMap = packageInfo.get("parserMap");
			if (parserMap != null) {
				String name = el.getNodeName();
				name = formatName(name);
				if (parserMap.containsKey(name)) {
					rt.invoke(null, parserMap.get(name), el, context, chain);
					return true;
				} else if (parserMap.containsKey("")) {
					System.out.println(parserMap);
					rt.invoke(null, parserMap.get(""), el, context, chain);
					return true;
				}else{
					//System.out.println(el.getTagName());
				}
			}
		}
		return false;
	}

	private boolean parseBefore(Element el, ParseContext context,
			ParseChain chain) {
		NamedNodeMap attrs = el.getAttributes();
		int len = attrs.getLength();
		LinkedHashMap<String, Attr> exclusiveMap = null;
		for (int i = len - 1; i >= 0; i--) {
			Attr attr = (Attr) attrs.item(i);
			String ans = getNS(attr);
			Map<String, Map<String, Object>> packageInfo = this.packageMap
					.get(ans);
			if (packageInfo != null) {
				Map<String, Object> beforeMap = packageInfo.get("beforeMap");
				if (beforeMap != null) {
					String name = formatName(attr.getName());
					if (beforeMap.containsKey(name)) {
						el.removeAttributeNode(attr);
						if (parseBefore(beforeMap.get(name), attr, context,
								chain)) {
							return true;
						} else {
							el.setAttributeNode(attr);
						}
					} else if (beforeMap.containsKey(name += '$')) {
						if (exclusiveMap == null) {
							exclusiveMap = new LinkedHashMap<String, Attr>();
						}
						exclusiveMap.put(name, attr);
					}
				}
			}
		}
		if (exclusiveMap != null) {
			for (String an : exclusiveMap.keySet()) {
				Attr attr = exclusiveMap.get(an);
				String ans = getNS(attr);
				Map<String, Map<String, Object>> packageInfo = this.packageMap
						.get(ans);
				Map<String, Object> beforeMap = packageInfo.get("beforeMap");
				if (parseBefore(beforeMap.get(an), attr, context, chain)) {
					return true;
				} else {
					el.setAttributeNode(attr);
				}
			}
		}
		return false;
	}

	private String getNS(Node attr) {
		String ans = attr.getNamespaceURI();
		return ans == null ? "" : ans;
	}

	private boolean parseBefore(Object fn, Attr attr, ParseContext context,
			ParseChain chain) {
		rt.invoke(null, fn, attr, context, chain);
		return true;
	}

	private boolean parseDocument(Node node, ParseContext context,
			ParseChain chain) {
		return (Boolean) rt.invoke(impl, "parseDocument", node, context, chain);
	}

	private boolean parseAttribute(Node node, ParseContext context,
			ParseChain chain) {
		Attr attr = (Attr) node;
		String ns = attr.getNamespaceURI();
		if ("http://www.w3.org/2000/xmlns/".equals(ns)) {
			if ((Boolean) rt.invoke(impl, "parseNamespace", attr, context,
					chain)) {
				return true;
			}
		} else if (ns == null) {
			ns = getNS(attr.getOwnerElement());
		}
		Map<String, Map<String, Object>> packageInfo = this.packageMap.get(ns);
		if (packageInfo != null) {
			Map<String, Object> onMap = packageInfo.get("onMap");
			if (onMap != null) {
				String name = formatName(attr.getName());
				if (onMap.containsKey(name)) {
					rt.invoke(null, onMap.get(name), attr, context, chain);
					return true;
				} else if (onMap.containsKey("")) {
					rt.invoke(null, onMap.get(""), attr, context, chain);
					return true;
				}
			}
		}
		return false;
	}

	private String formatName(String name) {
		return pattern.matcher(name).replaceAll("").toLowerCase();
	}

}
