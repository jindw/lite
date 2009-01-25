package org.xidea.lite.parser;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.lite.dtd.DefaultEntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLParser extends TextParser {
	private static final Pattern XML_HEADER_SPACE_PATTERN = Pattern
			.compile("^[\\s\\ufeff]*<");

	private DocumentBuilder documentBuilder;
	private NodeParser[] parserList = { new DefaultXMLNodeParser(this),
			new HTMLNodeParser(this,true),
			new CoreXMLNodeParser(this) };

	public XMLParser() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			//factory.setExpandEntityReferences(false);
			factory.setCoalescing(false);
			//factory.setXIncludeAware(true);
			documentBuilder = factory.newDocumentBuilder();
			documentBuilder.setEntityResolver(new DefaultEntityResolver());
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	@SuppressWarnings("unchecked")
	public <T extends NodeParser> T getNodeParser(Class<T> clazz){// extends NodeParser
		for(NodeParser p : parserList){
			if(clazz.isInstance(p)){
				return (T)p;
			}
		}
		return null;
	}

	public void addNodeParser(NodeParser parser){
		int length = this.parserList.length;
		NodeParser[] newParserList = new NodeParser[length+1];
		System.arraycopy(this.parserList, 0, newParserList, 0, length);
		newParserList[length] = parser;
		this.parserList = newParserList;
	}
	
	public List<Object> parse(Object data) {
		return parse(data, new ParseContext());
	}
	public List<Object> parse(Object data,ParseContext context) {
		try {
			Node node = null;
			if (data instanceof String) {
				String path = (String) data;
				if (XML_HEADER_SPACE_PATTERN.matcher(path).find()) {
					node = documentBuilder.parse(new InputSource(
							new StringReader(path)));
				} else {
					int pos = path.indexOf('#');
					String xpath = null;
					if (pos > 0) {
						xpath = path.substring(pos + 1);
						path = path.substring(0, pos);
					}
					node = loadXML(path, context);
					if (xpath != null) {
						node = selectNodes(xpath, node);
					}
				}

			} else if (data instanceof URL) {
				node = loadXML((URL) data, context);
			} else if (data instanceof File) {
				node = loadXML(((File) data).toURI().toURL(), context);
			}
			if (node != null) {
				parseNode(node, context);
			}
			return context.toResultTree();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Node loadXML(String url, ParseContext context) throws SAXException,
			IOException, XPathExpressionException {
		return loadXML(new URL(url), context);
	}

	public Node loadXML(URL url, ParseContext context) throws SAXException,
			IOException, XPathExpressionException {
		context.setCurrentURL(url);
		Document doc = documentBuilder.parse(getInputStream(url));
		// selectNodes(xpath, doc);
		return doc;
	}


	public NamespaceContext createNamespaceContext(Document doc){
		NamedNodeMap attributes = doc.getDocumentElement().getAttributes();
		final HashMap<String, String> prefixMap = new HashMap<String, String>();
		for (int i = 0; i < attributes.getLength(); i++) {
			Attr attr = (Attr) attributes.item(i);
			String value = attr.getNodeValue();
			if("xmlns".equals(attr.getNodeName())){
				int p1 = value.lastIndexOf('/');
				String prefix = value;
				if(p1>0){
					prefix = value.substring(p1+1);
					if(prefix.length()==0){
						int p2 = value.lastIndexOf('/',p1-1);
						prefix = value.substring(p2+1,p1);
					}
				}
				prefixMap.put(prefix, value);
			}else if("xmlns".equals(attr.getPrefix())){
				prefixMap.put(attr.getLocalName(), value);
			}
		}
		return new NamespaceContext() {
			public String getNamespaceURI(String prefix) {
				return prefixMap.get(prefix);
			}
			public String getPrefix(String namespaceURI) {
				throw new UnsupportedOperationException("xpath not use");
			}

			@SuppressWarnings("unchecked")
			public Iterator getPrefixes(String namespaceURI) {
				throw new UnsupportedOperationException("xpath not use");
			}
		};
	}

	public DocumentFragment selectNodes(String xpath, Node currentNode)
			throws XPathExpressionException {
		Document doc;
		if (currentNode instanceof Document) {
			doc = (Document) currentNode;
		} else {
			doc = currentNode.getOwnerDocument();
		}
		XPath xpathEvaluator = javax.xml.xpath.XPathFactory.newInstance()
				.newXPath();
		xpathEvaluator.setNamespaceContext(createNamespaceContext(doc));
		NodeList nodes = (NodeList) xpathEvaluator.evaluate(xpath, currentNode,
				XPathConstants.NODESET);

		DocumentFragment frm = toDocumentFragment(doc, nodes);
		return frm;
	}

	public DocumentFragment toDocumentFragment(Node node, NodeList nodes) {
		Document doc;
		if (node instanceof Document) {
			doc = (Document) node;
		} else {
			doc = node.getOwnerDocument();
		}
		DocumentFragment frm = doc.createDocumentFragment();
		for (int i = 0; i < nodes.getLength(); i++) {
			frm.appendChild(nodes.item(i).cloneNode(true));
		}
		return frm;
	}

	public void parseNode(Object node, ParseContext context) {
		if (node instanceof Node) {
			final int depth = context.getDepth();
			if(node instanceof Element){
				context.setDepth(depth+1);
			}
			int i = parserList.length;
			Node newNode = (Node) node;
			while (i-- > 0 && newNode!=null) {
				newNode = parserList[i].parseNode(newNode, context);
			}
			if(node instanceof Element){
				context.setDepth(depth);
			}
		} else if (node instanceof NodeList) {
			NodeList list = (NodeList) node;
			for (int i = 0; i < list.getLength(); i++) {
				parseNode(list.item(i), context);
			}
		}
	}
}
