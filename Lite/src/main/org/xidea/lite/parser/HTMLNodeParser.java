package org.xidea.lite.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.operation.TextContains;
import org.xidea.lite.Template;

public abstract class HTMLNodeParser implements NodeParser {
	protected static final Pattern HTML_LEAF = Pattern.compile(
			"^(?:meta|link|img|br|hr|input)$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern PRE_LEAF = Pattern.compile(
			"^(?:script|style|pre|textarea)$", Pattern.CASE_INSENSITIVE);
	protected static final Object[] END = new Object[0];
	protected static final String XHTMLNS = "http://www.w3.org/1999/xhtml";
	
	protected static final String EL_INPUT = "input";
	protected static final String EL_TEXTAREA = "textarea";
	protected static final String EL_SELECT = "select";
	private static final String EL_OPTION = "option";

	protected static final Map<String, String> BOOLEAN_ATTBUTE_MAP = new HashMap<String, String>();
	static {
		BOOLEAN_ATTBUTE_MAP.put("checked", "checked");
		BOOLEAN_ATTBUTE_MAP.put("selected", "selected");
		BOOLEAN_ATTBUTE_MAP.put("disabled", "disabled");

	}
	protected boolean autoFillForm;
	protected XMLParser parser;

	public void setAutoFillForm(boolean autoFillForm) {
		this.autoFillForm = autoFillForm;
	}

	public HTMLNodeParser(XMLParser parser, boolean autoFillForm) {
		this.parser = parser;
		this.setAutoFillForm(autoFillForm);
	}
	public Node parseNode(Node node, ParseContext context) {
		String namespace = node.getNamespaceURI();
		if (namespace == null || XHTMLNS.equals(namespace)) {
			if (node instanceof Element) {
				Element el = (Element) node;
				String localName = el.getLocalName();
				if (autoFillForm) {
					if (EL_INPUT.equals(localName)) {
						return parseInput(el, context);
					} else if (EL_TEXTAREA.equals(localName)) {
						return parseTextArea(el, context);
					} else if (EL_SELECT.equals(localName)) {
						return parseSelect(el, context);
					} else if (EL_OPTION.equals(localName)) {
						return parseSelectOption(el, context);
					}
				}
				return parseHTMLElement(node, context, null);
			}
		}
		return node;
	}

	protected Node parseHTMLElement(Node node, ParseContext context,
			List<Object> exts) {
		context.appendIndent();
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
			context.append("/>");
		} else {
			context.append(">");
			Node next = node.getFirstChild();
			if (next != null) {
				boolean reserveSpace = PRE_LEAF.matcher(tagName).find();
				boolean oldReserveSpace = context.isReserveSpace();
				context.setReserveSpace(oldReserveSpace || reserveSpace);

				boolean format = next.getNodeType() != Node.TEXT_NODE
						|| next.getNextSibling() != null;
				try {
					do {
						this.parser.parseNode(next, context);
					} while ((next = next.getNextSibling()) != null);
				} finally {
					context.setReserveSpace(oldReserveSpace);
				}
				if (format) {
					context.appendIndent();
				}
			}
			context.append("</" + tagName + '>');
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
					final Object el = this.parser.optimizeEL(value);
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
