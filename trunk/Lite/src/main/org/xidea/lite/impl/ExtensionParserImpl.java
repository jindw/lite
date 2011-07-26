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
			.compile("^(?:\\w*\\:)?\\w*[\\$\\{]");
	private static ThreadLocal<Node> CURRENT_LOCAL_NODE = new ThreadLocal<Node>();
	private Object impl;
	private final ExtensionParser proxy;
	private JSIRuntime rt = ParseUtil.getJSIRuntime();
	private Map<String, Map<String, Map<String, Object>>> packageMap;


	public ExtensionParserImpl() {
		Object fn = rt
				.eval("(function(){return new ($import('org.xidea.lite.parse:ExtensionParser',{}))(this)})");
		impl = rt.invoke(CURRENT_LOCAL_NODE, fn);
		proxy = rt.wrapToJava(impl, ExtensionParser.class);
		reset();
	}

	public void addExtension(String namespace, Object parserMap) {
		proxy.addExtension(namespace, parserMap);
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
		if (type == 1) {
			Node old = CURRENT_LOCAL_NODE.get();
			try{
				CURRENT_LOCAL_NODE.set(node);
				if(this.parseElement((Element) node, context, chain)){
					return;
				}
			}finally{
				CURRENT_LOCAL_NODE.set(old);
			}
		}else if (type == Node.ATTRIBUTE_NODE) { //2
			if (this.parseAttribute((Attr)node, context, chain)) {
				return;
			}
		} else if (type == Node.DOCUMENT_NODE || type == Node.COMMENT_NODE) {//9
			//rt.invoke(impl, "parse", node, context, chain);
			for(String pkg : packageMap.keySet()){
				Map<String, Map<String, Object>>v = packageMap.get(pkg);
				//objectMap.namespaceURI = namespace
				Map<String, Object> p = v.get("parserMap");
				Object fns = p.get(String.valueOf(type));
				if(fns!=null){
					doParse(node,fns,chain,pkg);
					return;
				}
			}
		} 
		chain.next(node);
	}

	private boolean parseElement(Element el, ParseContext context,
			ParseChain chain) {
		String nns = el.getNamespaceURI();
		if (parseBefore(el, context, chain)) {
			return true;
		}
		Map<String, Map<String, Object>> packageInfo = this.packageMap.get(nns==null?"":nns);
		if (packageInfo != null) {
			Map<String, Object> parserMap = packageInfo.get("parserMap");
			if (parserMap != null) {
				String name = el.getNodeName();
				name = formatName(name);
				Object fns = parserMap.get(name);
				if(fns == null){
					fns = parserMap.get("");
				}
				if (fns !=null) {
					doParse(el, fns, chain,nns);
					return true;
				} else {
					// System.out.println(el.getTagName());
				}
			}
		}
		return false;
	}

	private void doParse(Node el, Object fns, ParseChain chain,String pkg) {
		rt.invoke(impl,"doParse", el, fns,chain,pkg);
	}


	private boolean parseBefore(Element el, ParseContext context,
			ParseChain chain) {
		List<Attr> list = ParseUtil.getOrderedNSAttrList(el);
		while (list.size() > 0) {
			Attr attr = list.remove(0);
			Map<String, Map<String, Object>> packageInfo = this.packageMap
					.get(attr.getNamespaceURI());
			if (packageInfo != null) {
				Map<String, Object> beforeMap = packageInfo.get("beforeMap");
				if (beforeMap != null) {
					String name = formatName(attr.getName());
					if (beforeMap.containsKey(name)) {
						rt.invoke(chain, beforeMap.get(name), attr);
						return true;
					}
				}
			}
		}

		return false;
	}



	private boolean parseAttribute(Attr attr, ParseContext context,
			ParseChain chain) {
		String ns = attr.getNamespaceURI();
		if(ParseUtil.CORE_INFO.equals(ParseUtil.getLocalName(attr))
				&& ParseUtil.CORE_URI.equals(ns)){
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
			if(ns == null){
				ns = "";
			}
		}
		Map<String, Map<String, Object>> packageInfo = this.packageMap.get(ns);
		if (packageInfo != null) {
			Map<String, Object> parserMap = packageInfo.get("parsersMap");
			if (parserMap != null) {
				String name = formatName(attr.getName());
				Object fns = parserMap.get('2'+name);
				if(fns != null){
					doParse(attr, fns, chain,ns);
					//rt.invoke(impl,"doParse",el,fns,  chain);
				}
			}
		}
		return false;
	}

	private String formatName(String name) {
		return NAME_FORMAT.matcher(name).replaceAll("").toLowerCase();
	}

}
