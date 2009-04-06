package org.xidea.lite.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public abstract class HTMLNodeParser implements NodeParser {
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
	protected XMLParser parser;
	public HTMLNodeParser(XMLParser parser){
		this.parser = parser;
	}

	public Node parseNode(Node node, ParseContext context) {
		String namespace = node.getNamespaceURI();
		if (namespace == null || XHTMLNS.equals(namespace)) {
			if (node instanceof Element) {
				return parse(node, context);
			}
		}
		return node;
	}

	protected Node parse(Node node, ParseContext context) {
		return parseHTMLElement(node, context, null);
	}

	protected Node parseHTMLElement(Node node, ParseContext context,
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
							this.parser.parseNode(child, context);
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
		return null;
	}

	protected void appendHTMLAttribute(Attr node, ParseContext context) {
		String name = node.getName();
		String value = node.getValue();
		String trueValue = BOOLEAN_ATTBUTE_MAP.get(name);
		if (trueValue != null) {
			value = value.trim();
			if (value.length() == 0 || "false".equals(value)) {
				return;
			} else {
				trueValue = " " + name + "=\"" + trueValue + "\"";
				if (value.startsWith("${") && value.endsWith("}")) {
					value = value.substring(2, value.length() - 1);
					final Object el = context.optimizeEL(value);
					context.appendIf(el);
					context.append(trueValue);
					context.appendEnd();
				} else {
					context.append(trueValue);
				}
				return;
			}
		}
		this.parser.parseNode(node, context);

	}
}
