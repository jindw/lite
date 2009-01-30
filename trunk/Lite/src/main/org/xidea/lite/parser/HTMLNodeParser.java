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

public class HTMLNodeParser implements NodeParser {
	public static final Pattern HTML_LEAF = Pattern.compile(
			"^(?:meta|link|img|br|hr|input)$", Pattern.CASE_INSENSITIVE);
	public static final Pattern PRE_LEAF = Pattern.compile(
			"^(?:script|style|pre|textarea)$", Pattern.CASE_INSENSITIVE);

	private static final String XHTMLNS = "http://www.w3.org/1999/xhtml";
	private static final String EL_INPUT = "input";
	private static final String EL_TEXTAREA = "textarea";

	private static final Map<String, String> BOOLEAN_ATTBUTE_MAP = new HashMap<String, String>();
	static {
		BOOLEAN_ATTBUTE_MAP.put("checked", "checked");
		BOOLEAN_ATTBUTE_MAP.put("selected", "selected");
		BOOLEAN_ATTBUTE_MAP.put("disabled", "disabled");

	}

	private static final String ATTRIBUTE_SELECTED = "selected";
	private static final String ATTRIBUTE_CHECKED = "checked";
	private static final String ATTRIBUTE_TYPE = "type";
	private static final String ATTRIBUTE_NAME = "name";
	private static final String ATTRIBUTE_VALUE = "value";

	private static final String TYPE_CHECKBOX = "checkbox";
	private static final String TYPE_RADIO = "radio";

	private static final String EL_SELECT = "select";
	private static final String EL_OPTION = "option";

	private static final Object KEY_SELECT = new Object();

	private boolean autoFillForm;
	private XMLParser parser;

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
					if (EL_TEXTAREA.equals(localName)) {
						Document document = el.getOwnerDocument();
						if (el.hasAttribute(ATTRIBUTE_VALUE)) {
							el = (Element) el.cloneNode(false);
							String value = el.getAttribute(ATTRIBUTE_VALUE);
							el.removeAttribute(ATTRIBUTE_VALUE);
							el.appendChild(document.createTextNode(value));
						} else {
							el = (Element) el.cloneNode(true);
							Node child = el.getFirstChild();
							String name = el.getAttribute(ATTRIBUTE_NAME);
							if (child == null) {
								if (name.length() > 0) {
									el.appendChild(document.createTextNode("${"
											+ name + "}"));
								}
							} else if (child.getNextSibling() == null) {
								if (child instanceof Text) {
									String value = ((Text) child).getData()
											.trim();
									if (value.length() == 0
											&& name.length() > 0) {
										el.appendChild(document
												.createTextNode("${" + name
														+ "}"));
									}
								}
							}
						}
						return parseElement(el, context, null);
					} else if (EL_INPUT.equals(localName)) {
						return parseCloneBooleanInput(el, context);
					} else if (EL_SELECT.equals(localName)) {
						context.put(KEY_SELECT, el);
						return parseElement(node, context, null);
					} else if (EL_OPTION.equals(localName)) {
						return parseCloneBooleanInput(el, context);
					}
				}
				return parseElement(node, context, null);
			}
		}
		return node;
	}

	private Node parseCloneBooleanInput(Element element, ParseContext context) {
		element = (Element) element.cloneNode(true);// options 有值，textarea有值
		String type = element.getAttribute(ATTRIBUTE_TYPE);
		if (EL_OPTION.equals(element.getLocalName())) {
			Element selectNode = (Element) context.get(KEY_SELECT);
			if (!element.hasAttribute(ATTRIBUTE_SELECTED)) {
				if (selectNode.hasAttribute(ATTRIBUTE_NAME)
						&& selectNode.hasAttribute(ATTRIBUTE_VALUE)) {
					String name = selectNode.getAttribute(ATTRIBUTE_NAME);
					String value = selectNode.getAttribute(ATTRIBUTE_VALUE);
					final String valueEL;
					if (value.startsWith("${") && value.endsWith("}")) {
						value = value.substring(2, value.length() - 1);
						valueEL = value;
					} else if (value != null) {
						valueEL = JSONEncoder.encode(value);
					} else {
						valueEL = name;
					}
					List<Object> attributes = new ArrayList<Object>();
					final String collectionEL = name;
					// attributes.add(new Object[] { Template.IF_STRING_IN_TYPE,
					// collectionEL, valueEL });
					attributes.add(new Object[] { Template.IF_TYPE,
							createCSEL(context,collectionEL, valueEL) });
					attributes.add(" " + ATTRIBUTE_SELECTED + "=\""
							+ BOOLEAN_ATTBUTE_MAP.get(ATTRIBUTE_SELECTED)
							+ "\"");
					attributes.add(END);

					return parseElement(element, context, attributes);
				}
			}
		} else if (TYPE_CHECKBOX.equals(type) || TYPE_RADIO.equals(type)) {
			if (element.hasAttribute(ATTRIBUTE_CHECKED)) {
				return parseElement(element, context, null);
			} else if (element.hasAttribute(ATTRIBUTE_NAME)
					&& element.hasAttribute(ATTRIBUTE_VALUE)) {
				String name = element.getAttribute(ATTRIBUTE_NAME);
				String value = element.getAttribute(ATTRIBUTE_VALUE);
				final String valueEL;
				if (value.startsWith("${") && value.endsWith("}")) {
					value = value.substring(2, value.length() - 1);
					valueEL = value;
				} else {
					valueEL = JSONEncoder.encode(value);
				}
				List<Object> attributes = new ArrayList<Object>();

				final String collectionEL = name;

				// attributes.add(new Object[] { Template.IF_STRING_IN_TYPE,
				// collectionEL,
				// valueEL });
				attributes.add(new Object[] { Template.IF_TYPE,
						createCSEL(context,collectionEL, valueEL) });
				attributes.add(" " + ATTRIBUTE_CHECKED + "=\""
						+ BOOLEAN_ATTBUTE_MAP.get(ATTRIBUTE_CHECKED) + "\"");
				attributes.add(END);
				element.removeAttribute(ATTRIBUTE_CHECKED);

				return parseElement(element, context, attributes);
			}
		} else {
			if (!element.hasAttribute(ATTRIBUTE_VALUE)
					&& element.hasAttribute(ATTRIBUTE_NAME)) {
				element.setAttribute(ATTRIBUTE_VALUE, "${"
						+ element.getAttribute(ATTRIBUTE_NAME) + "}");
				return parseElement(element, context, null);
			}
		}
		return parseElement(element, context, null);
	}

	/**
	 * ContainsStringEL
	 * 
	 * @param valueEL
	 * @param collectionEL
	 * @return
	 */
	protected Object createCSEL(ParseContext context,final String collectionEL, final String valueEL) {
		String id=context.addGlobalInvocable(TextContains.class);
		return this.parser.optimizeEL(id +"(" + collectionEL
				+ "," + valueEL + ")");
	}

	private void appendBooleanAttribute(Attr node, ParseContext context) {
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
					context.append(new Object[] { Template.IF_TYPE, el });
					context.append(trueValue);
					context.append(END);
				} else {
					context.append(trueValue);
				}
				return;
			}
		}
		this.parser.parseNode(node, context);

	}

	private Node parseElement(Node node, ParseContext context, List<Object> exts) {
		context.appendIndent();
		Element el = (Element) node;
		NamedNodeMap attributes = node.getAttributes();
		String tagName = el.getTagName();
		context.append("<" + tagName);
		for (int i = 0; i < attributes.getLength(); i++) {
			appendBooleanAttribute((Attr) attributes.item(i), context);
			// this.parser.parseNode(attributes.item(i), context);
		}
		if (exts != null) {
			context.appendList(exts);
		}
		if (HTML_LEAF.matcher(tagName).find()) {
			context.append("/>");
			// context.appendFormatEnd();
		} else {
			context.append(">");
			Node next = node.getFirstChild();
			if (next != null) {
				boolean reserveSpace = PRE_LEAF.matcher(tagName).find();
				boolean oldReserveSpace = context.isReserveSpace();
				context.setReserveSpace(oldReserveSpace || reserveSpace);

				boolean format = next.getNodeType() != Node.TEXT_NODE
						|| next.getNextSibling() != null;
				// if (format) {
				// context.appendFormatEnd();
				// }
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
			// context.appendFormatEnd();
		}
		return null;
	}
}
