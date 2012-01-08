package org.xidea.lite.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xidea.jsi.JSIRuntime;
import org.xidea.lite.parse.ExtensionParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseContext;

public class ExtensionParserImpl implements ExtensionParser {
	private static Pattern NAME_FORMAT = Pattern.compile("^[\\w\\-]+\\:|[\\-]");
	private static Pattern FN_SEEKER = Pattern
			.compile("^(?:(?:\\w*\\:)?\\w*|[#!])[\\$\\{]");
	private static ThreadLocal<Node> CURRENT_LOCAL_NODE = new ThreadLocal<Node>();
	private Object impl;
//	private final ExtensionParser proxy;
	private JSIRuntime rt = ParseUtil.getJSIRuntime();
	private Map<String, Map<String, Object>> tagMap = new HashMap<String, Map<String, Object>>();
	private Map<String, Map<String, Object>> beforeMap = new HashMap<String, Map<String, Object>>();
	private Map<String, Map<String, Object>> attributeMap = new HashMap<String, Map<String, Object>>();
	private Map<String, Map<Pattern, Object>> patternTagMap = new HashMap<String, Map<Pattern, Object>>();
	private Map<String, Map<Pattern, Object>> patternAttributeMap = new HashMap<String, Map<Pattern, Object>>();
	private Map<String, Map<String, Object>> typeMap = new HashMap<String, Map<String, Object>>();

	public ExtensionParserImpl() {
		Object fn = rt
				.eval("(function(){var p = {};$export('org/xidea/lite/parse',p);return new p.ExtensionParser(this)})");
		impl = rt.invoke(CURRENT_LOCAL_NODE, fn);
//		proxy = rt.wrapToJava(impl, ExtensionParser.class);
		reset();
	}

	public void addExtension(String namespace, Object parserMap) {
		rt.invoke(impl, "addExtension", namespace, parserMap);
//		proxy.addExtension(namespace, parserMap);
		reset();
	}

	@SuppressWarnings("unchecked")
	private void reset() {
		Object obj = rt.invoke(this.impl, "mapJava", HashMap.class);
		Map<String, Map<String, Map<String, Object>>> packageMap = (Map<String, Map<String, Map<String, Object>>>) obj;
		tagMap.clear();
		attributeMap.clear();
		patternTagMap.clear();
		patternAttributeMap.clear();
		typeMap.clear();
		beforeMap.clear();
		for (String namespace : packageMap.keySet()) {
			Map<String, Map<String, Object>> parserMap = packageMap
					.get(namespace);
			this.initParser(namespace, tagMap, parserMap.get("tagMap"));
			this.initParser(namespace, attributeMap, parserMap
					.get("attributeMap"));
			this.initPatternParser(namespace, patternTagMap, parserMap
					.get("patternTagMap"));
			this.initPatternParser(namespace, patternAttributeMap, parserMap
					.get("patternAttributeMap"));
			this.initParser(namespace, typeMap, parserMap.get("typeMap"));
			this.initParser(namespace, beforeMap, parserMap.get("beforeMap"));
		}
	}

	private void initParser(String namespace,
			Map<String, Map<String, Object>> dest, Map<String, Object> parserMap) {
		if (parserMap != null) {
			dest.put(namespace, parserMap);
		}
	}

	private void initPatternParser(String namespace,
			Map<String, Map<Pattern, Object>> dest,
			Map<String, Object> parserMap) {
		if (parserMap != null) {
			Map<Pattern, Object> p = new HashMap<Pattern, Object>();
			for (String key : parserMap.keySet()) {
				p.put(Pattern.compile("^" + key.replaceAll("[*]", ".*") + "$"),
						parserMap.get(key));
			}
			dest.put(namespace, p);
		}
	}

	public int parseText(String text, int start, ParseContext context) {
		Number rtv = (Number) rt.invoke(impl, "parseText", text, start, context);
		return rtv.intValue();
		//return proxy.parseText(text, start, context);
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
		if (type == 1) {
			Node old = CURRENT_LOCAL_NODE.get();
			try {
				CURRENT_LOCAL_NODE.set(node);
				if (this.parseElement((Element) node, context, chain)) {
					return;
				}
			} finally {
				CURRENT_LOCAL_NODE.set(old);
			}
		} else if (type == Node.ATTRIBUTE_NODE) { // 2
			if (this.parseAttribute((Attr) node, context, chain)) {
				return;
			}
		} else if (type == Node.DOCUMENT_NODE || type == Node.COMMENT_NODE) {// 9
			// rt.invoke(impl, "parse", node, context, chain);
			for (String pkg : typeMap.keySet()) {
				Map<String, Object> p = typeMap.get(pkg);
				// objectMap.namespaceURI = namespace
				Object fns = p.get(String.valueOf(type));
				if (fns != null) {
					doParse(node, fns, chain, pkg);
					return;
				}
			}
		}
		chain.next(node);
	}

	private boolean parseElement(Element el, ParseContext context,
			ParseChain chain) {
		if (parseBefore(el, context, chain)) {
			return true;
		}
		String ns = el.getNamespaceURI();
		ns = ns == null ? "" : ns;
		Map<String, Object> parserMap = this.tagMap.get(ns);
		if (parserMap != null) {
			String name = formatName(el.getNodeName());
			Object fns = parserMap.get(name);
			if (fns != null) {
				doParse(el, fns, chain, ns);
				return true;
			} else {
				return doPatternParse(el, this.patternTagMap, name, chain, ns);
				// System.out.println(el.getTagName());
			}
		} else {
			return doPatternParse(el, this.patternTagMap, formatName(el.getNodeName()), chain, ns);
		}
	}

	private final boolean doPatternParse(Node el,
			Map<String, Map<Pattern, Object>> patternMap, String name,
			ParseChain chain, String nns) {
		Map<Pattern, Object> pm = patternMap.get(nns);
		if (pm != null) {
			//javascript array
			Object fns = null;
			for (Pattern p : pm.keySet()) {
				if (p.matcher(name).find()) {
					Object fns1 = pm.get(p);
					if(fns == null){
						//test
//						Object a1 = rt.eval("([1,2])");
//						a1=rt.invoke(a1, "concat", a1);
//						System.err.println(rt.invoke(null, "uneval", a1));
						fns = fns1;
					}else{
						fns = rt.invoke(fns, "concat", fns1);
					}
				}
			}
			if (fns != null) {
				doParse(el, fns, chain, nns);
				return true;
			}
		}
		return false;
	}

	private final void doParse(Node el, Object fns, ParseChain chain, String pkg) {
		rt.invoke(impl, "doParse", el, fns, chain, pkg);
	}

	private final boolean parseBefore(Element el, ParseContext context,
			ParseChain chain) {
		List<Attr> list = ParseUtil.getOrderedNSAttrList(el);
		while (list.size() > 0) {
			Attr attr = list.remove(0);
			String ns = attr.getNamespaceURI();
			Map<String, Object> beforeMap = this.beforeMap.get(ns);
			if (beforeMap != null) {
				String name = formatName(attr.getName());
				if (beforeMap.containsKey(name)) {
					rt.invoke(chain, beforeMap.get(name), attr);
					return true;
				}
			}
		}

		return false;
	}

	private boolean parseAttribute(Attr attr, ParseContext context,
			ParseChain chain) {
		String ns = attr.getNamespaceURI();
		if (ParseUtil.CORE_INFO.equals(ParseUtil.getLocalName(attr))
				&& ParseUtil.CORE_URI.equals(ns)) {
			return true;
		}

		Element el = attr.getOwnerElement();
		if ("http://www.w3.org/2000/xmlns/".equals(ns)) {
			if ((Boolean) rt.invoke(impl, "parseNamespace", attr, context,
					chain)) {
				return true;
			}
		} else if (ns == null) {
			ns = el.getNamespaceURI();
			if (ns == null) {
				ns = "";
			}
		}
		Map<String, Object> attributeMap = this.attributeMap.get(ns);
		if (attributeMap != null) {
			String name = formatName(attr.getName());
			Object fns = attributeMap.get(name);
			if (fns != null) {
				doParse(attr, fns, chain, ns);
				// rt.invoke(impl,"doParse",el,fns, chain);
				return true;
			}else{
				return doPatternParse(attr, this.patternAttributeMap, name, chain, ns);
			}
		}
		return doPatternParse(attr, this.patternAttributeMap, formatName(attr.getName()), chain, ns);
	}

	private String formatName(String name) {
		return NAME_FORMAT.matcher(name).replaceAll("").toLowerCase();
	}

}
