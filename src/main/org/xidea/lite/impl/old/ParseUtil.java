package org.xidea.lite.impl.old;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
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
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.impl.dtd.DefaultEntityResolver;
import org.xidea.lite.parse.ParseContext;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ParseUtil {

	private static Log log = LogFactory.getLog(CoreXMLNodeParser.class);
	private static final Pattern TEMPLATE_NAMESPACE_CORE = Pattern
			.compile("^http:\\/\\/www.xidea.org\\/ns\\/(?:template|lite)(?:\\/core)?\\/?$");

	static final String CORE_URI = "http://www.xidea.org/ns/lite/core";

	static boolean isCoreNS(String prefix, String url) {
		return ("c".equals(prefix) && ("#".equals(url) || "#core".equals(url)))
				|| url != null && TEMPLATE_NAMESPACE_CORE.matcher(url).find();
	}

	static XPathFactory xpathFactory;
	static TransformerFactory transformerFactory;

	static DocumentBuilder documentBuilder;
	static {
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

	public static InputStream openStream(URI uri) {
		try {
			if ("data".equalsIgnoreCase(uri.getScheme())) {
				String data = uri.getRawSchemeSpecificPart();
				int p = data.indexOf(',') + 1;
				String h = data.substring(0, p).toLowerCase();
				String charset = "UTF-8";
				data = data.substring(p);
				p = h.indexOf("charset=");
				if (p > 0) {
					charset = h.substring(h.indexOf('=', p) + 1, h.indexOf(',',
							p));
				}
				return new ByteArrayInputStream(URLDecoder
						.decode(data, charset).getBytes(charset));
				// charset=
			} else if ("classpath".equalsIgnoreCase(uri.getScheme())) {//classpath:///
				ClassLoader cl = ParseUtil.class.getClassLoader();
				uri = uri.normalize();
				String path = uri.getPath();
				path = path.substring(1);
				InputStream in = cl.getResourceAsStream(path);
				if (in == null) {
					ClassLoader cl2 = Thread.currentThread()
							.getContextClassLoader();
					if (cl2 != null) {
						in = cl2.getResourceAsStream(path);
					}
				}
				return in;
			} else {
				return uri.toURL().openStream();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Document parse(URI uri, ParseContext context) throws IOException,
			SAXException {
		InputStream in1 = ParseUtil.trimBOM(openStream(uri, context));
		in1.mark(1);
		if (in1.read() != '<') {
			return null;
		}
		in1.reset();
		String id = uri.toString();
		try {
			return documentBuilder.parse(in1, id);
		} catch (SAXParseException e) {
			InputStream in2 = ParseUtil.trimBOM(openStream(uri, context));
			try {
				// 做一次容错处理
				log.warn("Invalid xml source:" + e.toString()
						+ ",try to fix it：");
				return new XMLFixerImpl().parse(documentBuilder, in2, id);
				// in2 = new SequenceInputStream(new
				// ByteArrayInputStream(DEFAULT_STARTS),in2);
				// return documentBuilder.parse(in2, uri.toString());
			} catch (Exception ex) {
				log.debug(ex);
			} finally {
				in2.close();
			}
			throw new SAXException("XML Parser Error:" + id + "("
					+ e.getLineNumber() + "," + e.getColumnNumber() + ")\r\n"
					+ e.getMessage());
		} finally {
			in1.close();
		}
	}

	private static InputStream openStream(URI uri, ParseContext context) throws MalformedURLException, IOException {
		return context == null?openStream(uri):context.openStream(uri);
	}

	public static Document loadXML(String path, ParseContext context)
			throws SAXException, IOException {
		URI uri;
		if (path.startsWith("<")) {
			uri = createSourceURI(path);
		} else if (context != null) {
			uri = context.createURI(path);
		} else {
			uri = URI.create(path);
		}
		return parse(uri, context);
	}

	public static URI createSourceURI(String path){
		try {
			return URI.create("data:text/xml;charset=utf-8,"
					+ URLEncoder.encode(path, "UTF-8").replace("+", "%20"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static NodeList selectNodes(Node currentNode, String xpath)
			throws XPathExpressionException {
		Document doc;
		if (currentNode instanceof Document) {
			doc = (Document) currentNode;
		} else {
			doc = currentNode.getOwnerDocument();
		}
		XPath xpathEvaluator = createXPath(null);
		xpathEvaluator.setNamespaceContext(new NamespaceContextImpl(doc));
		NodeList nodes = (NodeList) xpathEvaluator.evaluate(xpath, currentNode,
				XPathConstants.NODESET);

		return nodes;
	}

	public static Node transform(Node doc, Node xslt)
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
			Transformer transformer = createTransformer(null, null);
			transformer.transform(new DOMSource(xslt), result);
			xsltSource = new javax.xml.transform.dom.DOMSource(result.getNode());

		} else {
			xsltSource = new javax.xml.transform.dom.DOMSource(xslt);

		}
		Transformer transformer = createTransformer(xsltSource, null);
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

	static DocumentFragment toFragment(Node node, NodeList nodes) {
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

	private static XPath createXPath(String xpathFactoryClass) {
		if (xpathFactory == null) {
			if (xpathFactoryClass != null) {
				try {
					try {
						xpathFactory = XPathFactory.newInstance(
								XPathFactory.DEFAULT_OBJECT_MODEL_URI,
								xpathFactoryClass, ParseUtil.class
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

	static Transformer createTransformer(Source source,
			String transformerFactoryClass)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError {
		if (transformerFactory == null) {
			if (transformerFactoryClass != null) {
				try {
					transformerFactory = TransformerFactory.newInstance(
							transformerFactoryClass, ParseUtil.class
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
		if (source == null) {
			return transformerFactory.newTransformer();
		} else {
			return transformerFactory.newTransformer(source);
		}
	}

	/**
	 * 如果属性不存在，返回null
	 * 
	 * @param context
	 * @param el
	 * @param key
	 * @return
	 */
	static Object getAttributeEL(ParseContext context, Element el,
			String... key) {
		String value = getAttributeOrNull(el, key);
		return toEL(context, value);

	}

	static String getAttributeOrNull(Element el, String... keys) {
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			if (el.hasAttribute(key)) {
				if (i > 0) {
					log.warn("元素：" + el.getTagName() + "属性：'" + key
							+ "' 不被推荐；请使用是:'" + keys[0] + "'代替");
				}
				return el.getAttribute(key);
			}
			if (key.equals("#text") && el.hasChildNodes()) {
				return el.getTextContent();
			}
		}
		return null;
	}

	static void parseChild(Node child, ParseContext context) {
		while (child != null) {
			context.parse(child);
			child = child.getNextSibling();
		}
	}

	/**
	 * 如果value == null,返回null
	 * 
	 * @param context
	 * @param value
	 * @return
	 */
	static Object toEL(ParseContext context, String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		if (value.startsWith("${") && value.endsWith("}")) {
			value = value.substring(2, value.length() - 1);
		} else {
			log.warn("输入的不是有效el，系统将字符串转换成el" + value);
			value = JSONEncoder.encode(value);
		}
		return context.parseEL(value);
	}

	static String loadText(InputStream in, String charset) throws IOException {
		return loadText(new InputStreamReader(in, charset));
	}

	static InputStream trimBOM(InputStream in) throws IOException {
		in = new BufferedInputStream(in, 3);
		int trim = 0;
		in.mark(3);
		outer: for (int i = 0; i < 3; i++) {// bugfix \ufeff
			// Unicode(UTF-16) FF-FE 31 00 32 00 33 00 34 00
			// Unicode big endian FE-FF 00 31 00 32 00 33 00 34
			// UTF-8 EF-BB-BF 31 32 33 34
			// ASCII 31 32 33 34

			int c = in.read();
			switch (c) {
			// UTF-16
			case 0xFF:
			case 0xFE:
				if (i == 1) {
					trim = 2;
					break outer;
				} else if (i > 1) {
					break outer;
				}
				// UTF-8
			case 0xEF:
				if (i != 0) {
					break outer;
				}
				break;
			case 0xBB:
				if (i != 1) {
					break outer;
				}
				break;
			case 0xBF:
				if (i != 2) {
					break outer;
				} else {
					trim = 3;
				}
				break;
			default:
				break outer;
			}
		}
		;
		in.reset();
		while (trim-- > 0) {
			in.read();
		}
		return in;
	}

	static String loadText(Reader reader) throws IOException {
		StringWriter out = new StringWriter();
		int count;
		char[] cbuf = new char[1024];
		while ((count = reader.read(cbuf)) > -1) {
			out.write(cbuf, 0, count);
		}
		return out.toString();
	}
}

class NamespaceContextImpl implements NamespaceContext {
	final HashMap<String, String> prefixMap = new HashMap<String, String>();

	protected NamespaceContextImpl(Document doc) {
		// nekohtml bug,not use doc.getDocumentElement()
		Node node = doc.getFirstChild();
		while (!(node instanceof Element)) {
			node = node.getNextSibling();
		}
		NamedNodeMap attributes = node.getAttributes();
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
	}

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
}
