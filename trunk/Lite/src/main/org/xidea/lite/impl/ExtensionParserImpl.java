package org.xidea.lite.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
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
			CURRENT_LOCAL_NODE.set(node);
			boolean parsed = this.parseElement((Element) node, context, chain);
			CURRENT_LOCAL_NODE.set(old);
			if (parsed) {
				return;
			}
		}else if (type == Node.DOCUMENT_NODE) {//9
			if (this.parseDocument((Document)node, context, chain)) {
				return;
			}
		} else if (type == Node.ATTRIBUTE_NODE) { //2
			if (this.parseAttribute((Attr)node, context, chain)) {
				return;
			}
		} else if (type == Node.COMMENT_NODE) {//8
			if (this.parseComment((Comment)node, context, chain)) {
				return;
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
				if (parserMap.containsKey(name)) {
					rt.invoke(chain, parserMap.get(name), el);
					return true;
				} else if (parserMap.containsKey("")) {
					rt.invoke(chain, parserMap.get(""), el);
					return true;
				} else {
					// System.out.println(el.getTagName());
				}
			}
		}
		return false;
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
//						el.removeAttributeNode(attr);
						rt.invoke(chain, beforeMap.get(name), attr);
						return true;
					}
				}
			}
		}

		return false;
	}


	private boolean parseDocument(Document node, ParseContext context,
			ParseChain chain) {
		return (Boolean) rt.invoke(impl, "parseDocument", node, context, chain);
	}

	private boolean parseComment(Comment node, ParseContext context,
			ParseChain chain) {
		return (Boolean) rt.invoke(impl, "parseComment", node, context, chain);
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
			}else{
				//xmlns
				String info = el.getAttributeNS(ParseUtil.CORE_URI, ParseUtil.CORE_INFO);
				if(info.length() ==0 || info.indexOf("|"+attr.getName()+"|")>0){
					return false;
				}else{
					return true;//自动补全的xmlns 不处理!
				}
			}
		} else if (ns == null) {
			ns = el.getNamespaceURI();
			if(ns == null){
				ns = "";
			}
		}
		Map<String, Map<String, Object>> packageInfo = this.packageMap.get(ns);
		if (packageInfo != null) {
			Map<String, Object> onMap = packageInfo.get("onMap");
			if (onMap != null) {
				String name = formatName(attr.getName());
				if (onMap.containsKey(name)) {
					rt.invoke(chain, onMap.get(name), attr);
					return true;
				} else if (onMap.containsKey("")) {
					rt.invoke(chain, onMap.get(""), attr);
					return true;
				}
			}
		}
		return false;
	}

	private String formatName(String name) {
		return NAME_FORMAT.matcher(name).replaceAll("").toLowerCase();
	}

}
