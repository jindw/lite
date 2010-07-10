package org.xidea.lite.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseContext;

public abstract class AbstractHTMLNodeParser implements NodeParser<Element> {
	private static final Log log = LogFactory.getLog(AbstractHTMLNodeParser.class);
	protected static final Pattern HTML_LEAF = Pattern.compile(
			"^(?:link|input|meta|img|br|hr)$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern PRE_LEAF = Pattern.compile(
			"^(?:script|style|pre|textarea)$", Pattern.CASE_INSENSITIVE);
	protected static final String XHTMLNS = "http://www.w3.org/1999/xhtml";

	protected static final Map<String, String> BOOLEAN_ATTBUTE_MAP = new HashMap<String, String>();
	static {
		BOOLEAN_ATTBUTE_MAP.put("checked", "checked");//input
		BOOLEAN_ATTBUTE_MAP.put("selected", "selected");//option
		BOOLEAN_ATTBUTE_MAP.put("disabled", "disabled");//any
		BOOLEAN_ATTBUTE_MAP.put("readonly", "readonly");//input
		BOOLEAN_ATTBUTE_MAP.put("multiple", "multiple");//selected 
		
		
		BOOLEAN_ATTBUTE_MAP.put("nowrap", "nowrap");//td/th
		BOOLEAN_ATTBUTE_MAP.put("defer", "defer");//defer script
		BOOLEAN_ATTBUTE_MAP.put("compact", "compact");//compact ul,ol,menu,dir,dl
		BOOLEAN_ATTBUTE_MAP.put("noshade", "noshade");//noshade hr
		BOOLEAN_ATTBUTE_MAP.put("declare", "declare");//declare object
		BOOLEAN_ATTBUTE_MAP.put("ismap", "ismap");//ismap img
		BOOLEAN_ATTBUTE_MAP.put("nohref", "nohref");//nohref area
	}

	public void parse(Element node,ParseContext context,ParseChain chain) {
		String namespace = node.getNamespaceURI();
		if (namespace == null && node.getPrefix()==null || XHTMLNS.equals(namespace)) {
			parseHTMLElement(node, context,chain);
		}else{
			chain.process(node);
		}
	}

	protected void parseHTMLElement(Element node, ParseContext context,ParseChain chain) {
		appendHTMLElement(node, context, null);
	}

	protected void appendHTMLElement(Element node, ParseContext context,
			List<Object> exts) {
		XMLContext.get(context).beginIndent();//false);
		String closeTag = null;
		try {
			Element el = (Element) node;
			NamedNodeMap attributes = node.getAttributes();
			String tagName = el.getTagName();
			context.append("<" + tagName);
			for (int i = 0; i < attributes.getLength(); i++) {
				appendHTMLAttribute((Attr) attributes.item(i), context);
			}
			if (exts != null) {
				context.appendAll(exts);
			}
			if (HTML_LEAF.matcher(tagName).find()) {
				closeTag = "/>";
			} else {
				context.append(">");
				Node child = node.getFirstChild();
				if (child != null) {
					boolean reserveSpace = PRE_LEAF.matcher(tagName).find();
					boolean oldReserveSpace = context.isReserveSpace();
					context.setReserveSpace(oldReserveSpace || reserveSpace);
					try {

						while (true) {
							context.parse(child);
							Node next = child.getNextSibling();
							if (next == null) {
								break;
							} else {
								child = next;
							}
						}
					} finally {
						context.setReserveSpace(oldReserveSpace);
					}
				}
				closeTag = "</" + tagName + '>';
			}
		} finally {
			XMLContext.get(context).endIndent();
			context.append(closeTag);
		}
	}

	protected void appendHTMLAttribute(Attr node, ParseContext context) {
		String attributeName = node.getName();
		String attributeValue = node.getValue();
		String trueValue = BOOLEAN_ATTBUTE_MAP.get(attributeName);
		if (trueValue != null) {
			attributeValue = attributeValue.trim();
			if (attributeValue.length() > 0 && ! "false".equals(attributeValue)) {
				String trueAttr = " " + attributeName + "=\"" + trueValue + "\"";
				if (attributeValue.startsWith("${") && attributeValue.endsWith("}")) {
					attributeValue = attributeValue.substring(2, attributeValue.length() - 1);
					final Object el = context.parseEL(attributeValue);
					context.appendIf(el);
					context.append(trueAttr);
					context.appendEnd();
				} else {
					if(!trueValue.equals(attributeValue)){
						log.error("HTML 固定属性值不对：期待："+trueValue+ "；实际值为："+attributeValue);
					}
					context.append(trueAttr);
				}
				
			}
		}else{
			context.parse(node);
		}
	}
}
