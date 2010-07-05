package org.xidea.lite.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import org.xidea.lite.impl.dtd.DefaultEntityResolver;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseContext;

public class DefaultXMLNodeParser implements NodeParser<Node> {

	public static final Pattern SCRIPT_TAG = Pattern.compile("^script$",
			Pattern.CASE_INSENSITIVE);
	final static Pattern PRIM_PATTERN = Pattern
			.compile("^\\s*([\\r\\n])\\s*|\\s*([\\r\\n])\\s*$|^(\\s)+|(\\s)+$");

	public void parse(Node node, ParseContext context, ParseChain chain) {
		switch (node.getNodeType()) {
		case 1: // NODE_ELEMENT
			parseElement(node, context);
			break;
		case 2: // NODE_ATTRIBUTE
			parseAttribute(node, context);
			break;
		case 3: // NODE_TEXT
			parseTextNode(node, context);
			break;
		case 4: // NODE_CDATA_SECTION
			parseCDATA(node, context);
			break;
		case 5: // NODE_ENTITY_REFERENCE
			parseEntityReference(node, context);
			break;
		case 6: // NODE_ENTITY
			parseEntity(node, context);
			break;
		case 7: // NODE_PROCESSING_INSTRUCTION
			parseProcessingInstruction(node, context);
			break;
		case 8: // NODE_COMMENT
			parseComment(node, context);
			break;
		case 9: // NODE_DOCUMENT
			parseDocument(node, context);
			break;
		case 11:// NODE_DOCUMENT_FRAGMENT
			parseDocument(node, context);
			break;
		case 10:// NODE_DOCUMENT_TYPE
			parseDocumentType(node, context);
			break;
		// case 11://NODE_DOCUMENT_FRAGMENT
		// return parseDocumentFragment(node,context);
		case 12:// NODE_NOTATION
			parseNotation(node, context);
			break;
		}
	}

	protected void parseProcessingInstruction(Node node, ParseContext context) {
		context.append("<?" + node.getNodeName() + " "
				+ ((ProcessingInstruction) node).getData() + "?>");
	}

	private void parseCDATA(Node node, ParseContext context) {
		boolean needFormat = needFormat(node);
		if (needFormat) {
			context.beginIndent();// false);
		}
		try {
			context.append("<![CDATA[");
			context.appendAll(context
					.parseText(((CDATASection) node).getData(),
							Template.EL_TYPE));
			context.append("]]>");
		} finally {
			if (needFormat) {
				context.endIndent();
			}
		}
	}

	private void parseNotation(Node node, ParseContext context) {
		throw new UnsupportedOperationException("parseNotation not support");
	}

	private void parseDocumentType(Node node0, ParseContext context) {
		DocumentType node = (DocumentType) node0;
		String pubid = node.getPublicId();
		String sysid = node.getSystemId();
		
		if (pubid != null) {
			if(pubid.equals(DefaultEntityResolver.DEFAULT__HTML_DTD)){
				//跳过容错补充dtd申明
				if(sysid!=null && sysid.startsWith("%3")){
					try {
						context.append(URLDecoder.decode(sysid, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
					}
				}
				return;
			}
			context.append("<!DOCTYPE ");
			context.append(node.getNodeName());
			context.append(" PUBLIC \"");
			context.append(pubid);
			if (sysid == null) {
				context.append("\" \"");
				context.append(sysid);
			}
			context.append("\">");
		} else if (sysid != null) {
			context.append("<!DOCTYPE ");
			context.append(node.getNodeName());
			context.append(" SYSTEM \"");
			context.append(sysid);
			context.append("\">");
		} else {
			context.append("<!DOCTYPE ");
			context.append(node.getNodeName());
			String sub = node.getInternalSubset();
			if(sub!=null && sub.trim().length()>0){
				context.append("[");
				context.append(sub);
				context.append("]");
			}
			context.append(">");
			
		}
		// context.appendFormatEnd();
	}

	private void parseDocument(Node node, ParseContext context) {
		for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
			context.parse(n);
		}
	}
	private void parseComment(Node node, ParseContext context) {
		return;
	}

	private void parseEntity(Node node, ParseContext context) {
		throw new UnsupportedOperationException("parseNotation not support");
	}

	private void parseEntityReference(Node node, ParseContext context) {
		context.append("&");
		context.append(node.getNodeName());
		context.append(";");
	}

	private void parseTextNode(Node node, ParseContext context) {
		String text = ((Text) node).getData();
		if (!context.isReserveSpace()) {
			// String text2 = text.trim();
			// if(text2.length()==0){
			// 比较复杂，算了
			// if(node.getPreviousSibling()!=null ||
			// node.getNextSibling()!=null){
			// }
			// }
			// text = text2;
			if (context.isCompress()) {
				text = safeTrim(text);
			} else if (context.isFormat()) {
				text = text.trim();
			}
		}
		if (text.length() > 0) {
			boolean needFormat = needFormat(node);
			if (needFormat) {
				context.beginIndent();// false);
			}
			try {
				context.appendAll(context.parseText(text, Template.XML_TEXT_TYPE));
			} finally {
				if (needFormat) {
					context.endIndent();
				}
			}
		}
		return;
	}

	protected String safeTrim(String text) {
		text = PRIM_PATTERN.matcher(text).replaceAll("$1$2$3$4");
		return text;
	}

	private void parseAttribute(Node node, ParseContext context) {
		Attr attr = (Attr) node;
		String name = attr.getName();
		String value = attr.getValue();
		if (ParseUtil.isCoreNS("xmlns:c".equals(name) ? "c" : attr
				.getPrefix(), value)) {
			return;
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
		if (isDynamic && !isStatic && buf.size() == 1) {
			// remove attribute;
			// context.append(" "+name+'=""');
//			if (buf.size() > 1) {
//				// TODO:....
//				throw new RuntimeException("只能有单个EL表达式");
//			} else {// 只考虑单一EL表达式的情况
				Object[] el = (Object[]) buf.get(0);
				context.appendAttribute(name, el[1]);
//			}
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
		return;
	}

	private List<Object> parseAttributeValue(ParseContext context, String value) {
		return context.parseText(value, Template.XML_ATTRIBUTE_TYPE);
	}

	private void parseElement(Node node, ParseContext context) {
		context.beginIndent();// false);
		String closeTag = null;
		try {
			Element el = (Element) node;
			NamedNodeMap attributes = node.getAttributes();
			String tagName = el.getTagName();
			context.append("<" + tagName);
			for (int i = 0; i < attributes.getLength(); i++) {
				context.parse(attributes.item(i));
			}
			Node child = node.getFirstChild();
			if (child != null) {
				context.append(">");
				while (true) {
					context.parse(child);
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
