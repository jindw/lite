package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.XMLContext;
import org.xidea.lite.parser.impl.dtd.DefaultEntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLContextImpl implements XMLContext {

	private static Log log = LogFactory.getLog(XMLContextImpl.class);

	private ArrayList<Boolean> indentStatus = new ArrayList<Boolean>();
	private int depth = 0;
	private boolean reserveSpace;
	private boolean format = false;
	private boolean compress = true;
	private final ParseContext context;

	protected XPathFactory xpathFactory;
	protected TransformerFactory transformerFactory;

	protected DocumentBuilder documentBuilder;

	public XMLContextImpl(ParseContext context) {
		this.context = context;
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

	public Document loadXML(URI uri) throws SAXException, IOException {
		return parse(uri);
	}

	private Document parse(URI uri) throws IOException, SAXException {
		InputStream in1 = ParseUtil.trimBOM(context.openInputStream(uri));
		in1.mark(1);
		if(in1.read() != '<'){
			return null;
		}
		in1.reset();
		String id = uri.toString();
		try {
			return documentBuilder.parse(in1, id);
		} catch (SAXParseException e) {
			InputStream in2 = ParseUtil.trimBOM(context.openInputStream(uri));
			try{
				//做一次容错处理
				log.warn("Invalid xml source:"+e.toString()+",try to fix it：");
				return new XMLFixerImpl().parse(documentBuilder,in2,id);
				//in2 = new SequenceInputStream(new ByteArrayInputStream(DEFAULT_STARTS),in2);
				//return documentBuilder.parse(in2, uri.toString());
			}catch(Exception ex){
				log.debug(ex);
			}finally{
				in2.close();
			}
			throw new SAXException("XML Parser Error:" + id + "("
					+ e.getLineNumber() + "," + e.getColumnNumber() + ")\r\n"
					+ e.getMessage());
		} finally {
			in1.close();
		}
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
				String url = prefixMap.get(prefix);
				return url == null ? prefix : url;
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

	public DocumentFragment selectNodes(Node currentNode, String xpath)
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

	public Node transform(URI parentURI, Node doc, Node xslt)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException,
			IOException {
		Source xsltSource; // create an instance of TransformerFactory
		
		if (xslt.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
			xslt = xslt.getFirstChild();
			while (xslt.getNodeType() != Node.ELEMENT_NODE) {
				xslt = xslt.getNextSibling();
			}
			DOMResult result = new DOMResult();
			Transformer transformer = createTransformer(null);
			transformer.transform(new DOMSource(xslt), result);
			xsltSource = new javax.xml.transform.dom.DOMSource(result.getNode());

		}else{
			xsltSource = new javax.xml.transform.dom.DOMSource(xslt);
			
		}
		Transformer transformer = createTransformer(xsltSource);
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
			String xpathFactoryClass = context
					.getFeatrue(XPathFactory.DEFAULT_PROPERTY_NAME);
			if (xpathFactoryClass != null) {
				try {
					try {
						xpathFactory = XPathFactory.newInstance(
								XPathFactory.DEFAULT_OBJECT_MODEL_URI,
								xpathFactoryClass, this.getClass()
										.getClassLoader());
					} catch (NoSuchMethodError e) {
						log.info("不好意思，我忘记了，我们JDK5没这个方法：<" + xpathFactoryClass
								+ ">");
						xpathFactory = (XPathFactory) Class.forName(
								xpathFactoryClass).newInstance();
						// 还有一堆校验，算了，饶了我吧：（
					}
				} catch (Exception e) {
					log.error(
							"自定义xpathFactory初始化失败<" + xpathFactoryClass + ">",
							e);
				}
			}
			if (xpathFactory == null) {
				xpathFactory = XPathFactory.newInstance();
			}
		}
		return xpathFactory.newXPath();
	}

	protected Transformer createTransformer(Source source)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError {
		if (transformerFactory == null) {
			String transformerFactoryClass = context
					.getFeatrue(TransformerFactory.class.getName());
			if (transformerFactoryClass != null) {
				try {
					transformerFactory = TransformerFactory.newInstance(
							transformerFactoryClass, this.getClass()
									.getClassLoader());
				} catch (Exception e) {
					log
							.error("创建xslt转换器失败<" + transformerFactoryClass
									+ ">", e);
				}
			}
			if (transformerFactory == null) {
				transformerFactory = TransformerFactory.newInstance();
			}
		}
		if(source == null){
			return transformerFactory.newTransformer();
		}else{
			return transformerFactory.newTransformer(source);
		}
	}

	public static DocumentFragment toDocumentFragment(Node node, NodeList nodes) {
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

	public boolean isFormat() {
		return format;
	}

	public void setFormat(boolean format) {
		this.format = format;
	}

	public boolean isCompress() {
		return compress;
	}

	public void setCompress(boolean compress) {
		this.compress = compress;
	}

	public boolean isReserveSpace() {
		return reserveSpace;
	}

	public void setReserveSpace(boolean keepSpace) {
		this.reserveSpace = keepSpace;
	}

	public int getDepth() {
		return depth;
	}

	public void beginIndent() {// boolean needClose) {
		int size = indentStatus.size();
		printIndent();
		depth++;
		switch (depth - size) {
		case 1:
			indentStatus.add(null);
		case 0:
			indentStatus.add(null);
			// case -1:
		default:
			indentStatus.set(depth - 1, true);
			indentStatus.set(depth, false);
		}
	}

	public void endIndent() {
		if (Boolean.TRUE.equals(indentStatus.get(depth))) {
			depth--;
			printIndent();
		} else {
			depth--;
		}

	}

	private void printIndent() {
		if (!this.isCompress() && !this.isReserveSpace() && this.isFormat()) {
			int i = context.mark();
			if (i > 0) {
				context.append("\r\n");
			}
			int depth = this.depth;
			if (depth > 0) {
				char[] data = new char[depth];
				while (depth-- > 0) {
					data[depth] = ' ';
				}
				context.append(new String(data));
			}

		}
	}

}
