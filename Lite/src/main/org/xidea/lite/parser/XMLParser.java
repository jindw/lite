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
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	private static Log log = LogFactory.getLog(XMLParser.class);

	private static final Pattern XML_HEADER_SPACE_PATTERN = Pattern
			.compile("^[\\s\\ufeff]*<");

	private XPathFactory xpathFactory;
	private TransformerFactory transformerFactory;

	private String transformerFactoryClass = null;// "org.apache.xalan.processor.TransformerFactoryImpl";
	private String xpathFactoryClass = null;// "org.apache.xpath.jaxp.XPathFactoryImpl";

	private DocumentBuilder documentBuilder;
	private NodeParser[] parserList = { new DefaultXMLNodeParser(this),
			new HTMLFormNodeParser(this), new CoreXMLNodeParser(this) };

	public XMLParser() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			// factory.setExpandEntityReferences(false);
			factory.setCoalescing(false);
			// factory.setXIncludeAware(true);
			documentBuilder = factory.newDocumentBuilder();
			documentBuilder.setEntityResolver(new DefaultEntityResolver());
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public XMLParser(String transformerFactory, String xpathFactory) {
		this();
		this.transformerFactoryClass = transformerFactory;
		this.xpathFactoryClass = xpathFactory;
	}

	@SuppressWarnings("unchecked")
	public <T extends NodeParser> T getNodeParser(Class<T> clazz) {
		for (NodeParser p : parserList) {
			if (clazz.isInstance(p)) {
				return (T) p;
			}
		}
		return null;
	}

	public void addNodeParser(NodeParser parser) {
		int length = this.parserList.length;
		NodeParser[] newParserList = new NodeParser[length + 1];
		System.arraycopy(this.parserList, 0, newParserList, 0, length);
		newParserList[length] = parser;
		this.parserList = newParserList;
	}

	@Override
	public List<Object> parse(Object data, ParseContext context) {
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

	public void parseNode(Object node, ParseContext context) {
		if (node instanceof Node) {
			int i = parserList.length;
			Node newNode = (Node) node;
			while (i-- > 0 && newNode != null) {
				newNode = parserList[i].parseNode(newNode, context);
			}
		} else if (node instanceof NodeList) {
			NodeList list = (NodeList) node;
			for (int i = 0; i < list.getLength(); i++) {
				parseNode(list.item(i), context);
			}
		} else if (node instanceof NamedNodeMap) {
			NamedNodeMap list = (NamedNodeMap) node;
			for (int i = 0; i < list.getLength(); i++) {
				parseNode(list.item(i), context);
			}
		}
	}

	public Node loadXML(String url, ParseContext context) throws SAXException,
			IOException, XPathExpressionException {
		return loadXML(new URL(url), context);
	}

	public Document loadXML(URL url, ParseContext context) throws SAXException,
			IOException, XPathExpressionException {
		context.setCurrentURL(url);
		Document doc = documentBuilder.parse(context.getInputStream(url));
		// selectNodes(xpath, doc);
		return doc;
	}

	protected NamespaceContext createNamespaceContext(Document doc) {
		// nekohtml bug,not use doc.getDocumentElement()
		Node node = doc.getFirstChild();
		while (!(node instanceof Element)) {
			node = node.getNextSibling();
		}
		NamedNodeMap attributes = node.getAttributes();
		final HashMap<String, String> prefixMap = new HashMap<String, String>();
		for (int i = 0; i < attributes.getLength(); i++) {
			Attr attr = (Attr) attributes.item(i);
			String value = attr.getNodeValue();
			if ("xmlns".equals(attr.getNodeName())) {
				int p1 = value.lastIndexOf('/');
				String prefix = value;
				if (p1 > 0) {
					prefix = value.substring(p1 + 1);
					if (prefix.length() == 0) {
						int p2 = value.lastIndexOf('/', p1 - 1);
						prefix = value.substring(p2 + 1, p1);
					}
				}
				prefixMap.put(prefix, value);
			} else if ("xmlns".equals(attr.getPrefix())) {
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

	protected DocumentFragment selectNodes(String xpath, Node currentNode)
			throws XPathExpressionException {
		Document doc;
		if (currentNode instanceof Document) {
			doc = (Document) currentNode;
		} else {
			doc = currentNode.getOwnerDocument();
		}
		XPath xpathEvaluator = createXPath();
		xpathEvaluator.setNamespaceContext(createNamespaceContext(doc));
		NodeList nodes = (NodeList) xpathEvaluator.evaluate(xpath, currentNode,
				XPathConstants.NODESET);

		DocumentFragment frm = toDocumentFragment(doc, nodes);
		return frm;
	}

	protected Node transform(ParseContext context, URL parentURL, Node doc,
			String xslt) throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException,
			IOException {
		Source xsltSource;
		if (xslt.startsWith("#")) {
			Node node1 = ((Node) context.getAttribute(xslt));
			Transformer transformer = createTransformer();
			DOMResult result = new DOMResult();
			if (node1.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
				node1 = node1.getFirstChild();
				while (node1.getNodeType() != Node.ELEMENT_NODE) {
					node1 = node1.getNextSibling();
				}
			}
			transformer.transform(new DOMSource(node1), result);
			xsltSource = new javax.xml.transform.dom.DOMSource(result.getNode());
		} else {
			xsltSource = new javax.xml.transform.stream.StreamSource(context
					.createURL(parentURL, xslt).openStream());
		}

		// create an instance of TransformerFactory
		Transformer transformer = javax.xml.transform.TransformerFactory
				.newInstance().newTransformer(xsltSource);
		// javax.xml.transform.TransformerFactory
		// .newInstance().set
		// transformer.setNamespaceContext(parser.createNamespaceContext(doc.getOwnerDocument()));

		Source xmlSource;
		if (doc.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
			Element root = doc.getOwnerDocument().createElement("root");
			root.appendChild(doc);
			xmlSource = new DOMSource(root);
		} else {
			xmlSource = new DOMSource(doc);
		}
		DOMResult result = new DOMResult();

		transformer.transform(xmlSource, result);
		return result.getNode();
	}

	protected XPath createXPath() {
		if (xpathFactory == null) {
			try {
				xpathFactory = XPathFactory.newInstance(
						XPathFactory.DEFAULT_OBJECT_MODEL_URI,
						xpathFactoryClass, this.getClass().getClassLoader());
			} catch (Exception e) {
			}
			if (xpathFactory == null) {
				xpathFactory = XPathFactory.newInstance();
			}
		}
		XPath xpathEvaluator = xpathFactory.newXPath();
		return xpathEvaluator;
	}

	protected Transformer createTransformer()
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError {
		if (transformerFactory == null) {
			if (transformerFactoryClass != null) {
				try {
					transformerFactory = TransformerFactory.newInstance(
							transformerFactoryClass, this.getClass()
									.getClassLoader());
				} catch (Exception e) {
					log.error("创建xslt转换器失败", e);
				}
				if (transformerFactory == null) {
					transformerFactory = TransformerFactory.newInstance();
				}
			}
		}
		return transformerFactory.newTransformer();
	}

	DocumentFragment toDocumentFragment(Node node, NodeList nodes) {
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

}
