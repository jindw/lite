package org.xidea.lite.parser;

import java.util.List;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xidea.lite.Template;

public class DefaultXMLNodeParser implements NodeParser {

	public static final Pattern SCRIPT_TAG = Pattern.compile("^script$",
			Pattern.CASE_INSENSITIVE);

	private XMLParser parser;

	public DefaultXMLNodeParser(XMLParser parser) {
		this.parser = parser;
	}

	public Node parseNode(Node node, ParseContext context) {
		switch (node.getNodeType()) {
		case 1: // NODE_ELEMENT
			return parseElement(node, context);
		case 2: // NODE_ATTRIBUTE
			return parseAttribute(node, context);
		case 3: // NODE_TEXT
			return parseTextNode(node, context);
		case 4: // NODE_CDATA_SECTION
			return parseCDATA(node, context);
		case 5: // NODE_ENTITY_REFERENCE
			return parseEntityReference(node, context);
		case 6: // NODE_ENTITY
			return parseEntity(node, context);
		case 7: // NODE_PROCESSING_INSTRUCTION
			return parseProcessingInstruction(node, context);
		case 8: // NODE_COMMENT
			return parseComment(node, context);
		case 9: // NODE_DOCUMENT
		case 11:// NODE_DOCUMENT_FRAGMENT
			return parseDocument(node, context);
		case 10:// NODE_DOCUMENT_TYPE
			return parseDocumentType(node, context);
			// case 11://NODE_DOCUMENT_FRAGMENT
			// return parseDocumentFragment(node,context);
		case 12:// NODE_NOTATION
			return parseNotation(node, context);
		default:// 文本节点
			// this.println("<!-- ERROR＄1�7 UNKNOW
			// nodeType:"+node.nodeType+"-->")
			return node;
		}
	}

	protected Node parseProcessingInstruction(Node node, ParseContext context) {
		context.append("<?" + node.getNodeName() + " "
				+ ((ProcessingInstruction) node).getData() + "?>");
		return null;
	}

	private Node parseCDATA(Node node, ParseContext context) {
		if(needFormat(node)){
			context.beginIndent(false);
		}
		context.append("<![CDATA[");
		this.parser.parseText(context, ((CDATASection) node).getData(),
				Template.EL_TYPE);
		context.append("]]>");
		return null;
	}

	private Node parseNotation(Node node, ParseContext context) {
		throw new UnsupportedOperationException("parseNotation not support");
	}

	private Node parseDocumentType(Node node0, ParseContext context) {
		DocumentType node = (DocumentType) node0;
		if (node.getPublicId() != null) {
			context.append("<!DOCTYPE ");
			context.append(node.getNodeName());
			context.append(" PUBLIC \"");
			context.append(node.getPublicId());
			context.append("\" \"");
			context.append(node.getSystemId());
			context.append("\">");
		} else {
			context.append("<!DOCTYPE ");
			context.append(node.getNodeName());
			context.append("[");
			context.append(node.getInternalSubset());
			context.append("]>");
		}
		// context.appendFormatEnd();
		return null;
	}

	private Node parseDocument(Node node, ParseContext context) {
		for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
			this.parser.parseNode(n, context);
		}
		return null;
	}

	private Node parseComment(Node node, ParseContext context) {
		return null;
	}

	private Node parseEntity(Node node, ParseContext context) {
		throw new UnsupportedOperationException("parseNotation not support");
	}

	private Node parseEntityReference(Node node, ParseContext context) {
		context.append("&");
		context.append(node.getNodeName());
		context.append(";");
		return null;
	}

	private Node parseTextNode(Node node, ParseContext context) {
		String text = ((Text) node).getData();
		if (context.isCompress() && !context.isReserveSpace()) {
			// String text2 = text.trim();
			// if(text2.length()==0){
			// 比较复杂，算了
			// if(node.getPreviousSibling()!=null ||
			// node.getNextSibling()!=null){
			// }
			// }
			// text = text2;
			text = text.replaceAll("^(\\s)+|(\\s)+$", "$1$2");
		}
		if (text.length() > 0) {
			if(needFormat(node)){
				context.beginIndent(false);
			}
			this.parser.parseText(context, text, Template.XML_TEXT_TYPE);
		}
		return null;
	}

	private Node parseAttribute(Node node, ParseContext context) {
		Attr attr = (Attr) node;
		String name = attr.getName();
		String value = attr.getValue();
		if (CoreXMLNodeParser.isCoreNS("xmlns:c".equals(name) ? "c" : attr
				.getPrefix(), value)) {
			return null;
		}
		List<Object> buf = parseAttributeValue(context, value);
		boolean isStatic = false;
		boolean isDynamic = false;
		// hack parseText is void
		int i = buf.size();
		while (i-- > 0) {
			// hack reuse value param
			Object item = buf.get(i);
			if (item instanceof String) {
				if (((String) item).length() > 0) {
					isStatic = true;
				} else {
					buf.remove(i);
				}
			} else {
				isDynamic = true;
			}
		}
		if (isDynamic && !isStatic) {
			// remove attribute;
			// context.append(" "+name+'=""');
			if (buf.size() > 1) {
				// TODO:....
				throw new RuntimeException("只能有单个EL表达式");
			} else {// 只考虑单一EL表达式的情况
				Object[] el = (Object[]) buf.get(0);
				context.appendAttribute(name, el[1]);
			}
		} else {
			context.append(" " + name + "=\"");
			if (name.startsWith("xmlns")) {
				if (buf.size() == 1
						&& "http://www.xidea.org/ns/lite/xhtml".equals(buf
								.get(0))) {
					buf.set(0, "http://www.w3.org/1999/xhtml");
				}
			}
			context.appendAll(buf);
			context.append("\"");
		}
		return null;
	}

	private List<Object> parseAttributeValue(ParseContext context, String value) {
		int mark = context.mark();
		this.parser.parseText(context, value, Template.XML_ATTRIBUTE_TYPE);
		return context.reset(mark);
	}

	private Node parseElement(Node node, ParseContext context) {
		context.beginIndent(true);
		String closeTag = null;
		try {
			Element el = (Element) node;
			NamedNodeMap attributes = node.getAttributes();
			String tagName = el.getTagName();
			context.append("<" + tagName);
			for (int i = 0; i < attributes.getLength(); i++) {
				this.parser.parseNode(attributes.item(i), context);
			}
			Node child = node.getFirstChild();
			if (child != null) {
				context.append(">");
				while (true) {
					this.parser.parseNode(child, context);
					Node next = child.getNextSibling();
					if (next == null) {
						break;
					} else {
						child = next;
					}
				}

				closeTag = "</" + tagName + '>';
			} else {
				closeTag = "/>";
			}
		} finally {
			context.endIndent();
			context.append(closeTag);
		}
		return null;
	}

	/**
	 * 有兄弟节点需要格式化，非文本节点需要格式化
	 * 
	 * @param next
	 * @return
	 */
	static boolean needFormat(Node node) {
		return node.getNodeType() != Node.TEXT_NODE
				|| node.getPreviousSibling() != null
				|| node.getNextSibling() != null;
	}

}
