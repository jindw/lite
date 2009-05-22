package org.xidea.lite.parser.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;

public abstract class AbstractHTMLParser implements Parser<Element> {
	protected static final Pattern HTML_LEAF = Pattern.compile(
			"^(?:meta|link|img|br|hr|input)$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern PRE_LEAF = Pattern.compile(
			"^(?:script|style|pre|textarea)$", Pattern.CASE_INSENSITIVE);
	protected static final String XHTMLNS = "http://www.w3.org/1999/xhtml";

	protected static final Map<String, String> BOOLEAN_ATTBUTE_MAP = new HashMap<String, String>();
	static {
		BOOLEAN_ATTBUTE_MAP.put("checked", "checked");
		BOOLEAN_ATTBUTE_MAP.put("selected", "selected");
		BOOLEAN_ATTBUTE_MAP.put("disabled", "disabled");
	}

	public void parse(Element node,ParseContext context,ParseChain chain) {
		String namespace = node.getNamespaceURI();
		if (namespace == null && node.getPrefix()==null || XHTMLNS.equals(namespace)) {
			parse(node, context);
		}else{
			chain.process(node);
		}
	}

	protected void parse(Node node, ParseContext context) {
		parseHTMLElement(node, context, null);
	}

	protected void parseHTMLElement(Node node, ParseContext context,
			List<Object> exts) {
		context.beginIndent();//false);
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
			context.endIndent();
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
					context.append(trueAttr);
				}
				
			}
		}else{
			context.parse(node);
		}
	}
}
