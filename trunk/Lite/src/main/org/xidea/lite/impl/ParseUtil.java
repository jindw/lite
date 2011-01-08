package org.xidea.lite.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.JSIText;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.impl.dtd.DefaultEntityResolver;
import org.xidea.lite.parse.ParseContext;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ParseUtil {

	private static Log log = LogFactory.getLog(ParseUtil.class);
	static final ThreadLocal<JSIRuntime> jsi = new ThreadLocal<JSIRuntime>();

	static XPathFactory xpathFactory;

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

	static JSIRuntime getJSIRuntime() {
		JSIRuntime rt = jsi.get();
		if (rt == null) {
			jsi.set(rt = RuntimeSupport.create());
		}
		return rt;
	}

	static String safeTrim(String text) {
		StringBuffer buf = new StringBuffer(text);
		int len = buf.length();
		int end = len;
		char s1 = ' ';
		while(end-->0){
			char c = buf.charAt(end);
			if(c == '\r' || c == '\n'){
				s1 = c;
			}else if(c != ' ' && c!='\t'){
				end++;
				if(end<len){
					buf.setCharAt(end, s1);
					end++;
				}else{
				}
				s1 = ' ';
				for(int j=0;j<end;j++){
					c = buf.charAt(j);
					if(c == '\r' || c == '\n'){
						s1 = c;
					}else if(c != ' ' && c!='\t'){
						j--;
						if(j>=0){
							buf.setCharAt(j, s1);
						}else{
							j++;
						}
						return buf.substring(j, end);
					}
				}
			}
		}
		return "";
	}
	// static Object eval(String source){
	// return getJSIRuntime().eval(source);
	// }

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
					charset = h.substring(h.indexOf('=', p) + 1,
							h.indexOf(',', p));
				}
				return new ByteArrayInputStream(URLDecoder
						.decode(data, charset).getBytes(charset));
				// charset=
			} else if ("classpath".equalsIgnoreCase(uri.getScheme())) {// classpath:///
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
				if(isFile(uri)){
					File f = new File(uri);
					if(f.exists()){
						return new FileInputStream(f);
					}else{
						return null;
					}
				}
				return uri.toURL().openStream();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Document parse(URI uri, ParseContext context)
			throws IOException, SAXException {
		InputStream in1 = ParseUtil.trimBOM(context,uri);
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
				System.out.println(JSIText.loadText(in2, "utf-8"));
				// return new XMLFixerImpl().parse(documentBuilder, in2, id);
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

	private static InputStream openStream(URI uri, ParseContext context)
			throws MalformedURLException, IOException {
		return context == null ? openStream(uri) : context.openStream(uri);
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

	public static URI createSourceURI(String path) {
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
								xpathFactoryClass,
								ParseUtil.class.getClassLoader());
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
	private static InputStream trimBOM(ParseContext context,URI uri) throws IOException{
		try{
			return trimBOM(openStream(uri, context));
		}catch(IOException e){
			log.warn(uri+"读取异常:",e);
			throw e;
		}catch(RuntimeException e){
			log.warn(uri+"读取异常:",e);
			throw e;
		}
	}

	private static InputStream trimBOM(InputStream in) throws IOException {
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

	static boolean isFile(URI uri){
		return "file".equals(uri.getScheme());
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

	public Iterator<?> getPrefixes(String namespaceURI) {
		throw new UnsupportedOperationException("xpath not use");
	}
}
