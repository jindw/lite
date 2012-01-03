package org.xidea.lite.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.impl.dtd.DefaultEntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ParseUtil {

	private static Log log = LogFactory.getLog(ParseUtil.class);
	static final ThreadLocal<JSIRuntime> jsi = new ThreadLocal<JSIRuntime>();

	static XPathFactory xpathFactory;

	static DocumentBuilder documentBuilder;
	final static String[] CHARSETS;
	public static final String CORE_URI = "http://www.xidea.org/lite/core";
	// c:__i="1,2|c:if|c:for|c:client|"
	public static final String CORE_INFO = "__i";
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
		LinkedHashSet<String> cs = new LinkedHashSet<String>();
		cs.add("UTF-8");
		cs.add(Charset.defaultCharset().name());
		cs.add("GBK");
		for (Charset c : Charset.availableCharsets().values()) {
			cs.add(c.name());
		}
		CHARSETS = cs.toArray(new String[cs.size()]);

	}

	public static JSIRuntime getJSIRuntime() {
		JSIRuntime rt = jsi.get();
		if (rt == null) {
			jsi.set(rt = RuntimeSupport.create());
			rt.eval("$import('org.xidea.jsidoc.util:JSON')");
			// ((RuntimeSupport)rt).setOptimizationLevel(-1);
		}
		return rt;
	}

	/**
	 * 删除多余的xml空格，如果有换行，尽量保留换行，方便阅读
	 * 
	 * @param text
	 * @return
	 */
	public static String safeTrim(String text) {
		StringBuffer buf = new StringBuffer(text);
		final int len = buf.length();
		if (len == 0) {
			return "";
		}
		int end = len;
		char s1 = ' ';
		while (end-- > 0) {
			char c = buf.charAt(end);
			if (c == '\r' || c == '\n') {
				s1 = c;
			} else if (c != ' ' && c != '\t') {
				end++;
				if (end < len) {
					buf.setCharAt(end, s1);
					end++;
				} else {
				}
				s1 = ' ';
				for (int j = 0; j < end; j++) {
					c = buf.charAt(j);
					if (c == '\r' || c == '\n') {
						s1 = c;
					} else if (c != ' ' && c != '\t') {
						j--;
						if (j >= 0) {
							buf.setCharAt(j, s1);
						} else {
							j++;
						}
						return buf.substring(j, end);
					}
				}
			}
		}
		return String.valueOf(s1);
	}

	private static Pattern forcePattern = Pattern
			.compile("^\\s+|\\s+$|(\\s)\\s+");

	static String forceTrim(String text) {
		return forcePattern.matcher(text).replaceAll("$1");
	}

	public static InputStream openStream(URI uri) throws IOException {
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
				if (isFile(uri)) {
					File f = new File(uri);
					if (f.exists()) {
						return new FileInputStream(f);
					} else {
						return null;
					}
				}
				return uri.toURL().openStream();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	//
	// public static Document parse(URI uri, ParseContext context) throws
	// SAXException, IOException {
	// String id = uri.toString();
	// String text = loadText(uri, context);
	// return loadXMLBySource(text, id);
	// }

	public static Document loadXML(String path) throws SAXException,
			IOException {
		if (path.startsWith("#")) {
			path = "<out xmlns='http://www.xidea.org/lite/core'><![CDATA["
					+ TXT_CDATA_END.matcher(
							TXT_HEADER.matcher(path).replaceAll(""))
							.replaceAll("]]]]><![CDATA[>") + "]]></out>";
		}
		if (path.startsWith("<")) {
			path = normalize(path, path);
			return loadXMLBySource(path, path);
		} else {
			String id = path;
			String text = path;
			try {
				URI uri = URI.create(path);
				text = loadTextAndClose(openStream(uri), null);
				id = uri.toString();
			} catch (Exception e) {
			}
			path = normalize(text, id);
			return loadXMLBySource(text, id);
		}

	}

	public static Document loadXMLBySource(String text, String id)
			throws IOException, SAXException {
		String ins = getXMLInstruction(text);
		InputSource in = new InputSource(new StringReader(text));
		in.setSystemId(id);
		in.setCharacterStream(new StringReader(text));
		try {
			Document xml = documentBuilder.parse(in);
			if (ins != null) {
				xml.insertBefore(xml.createProcessingInstruction("xml", ins),
						xml.getFirstChild());
			}
			return xml;
		} catch (SAXException e) {
			log.info("xml 解析失败,文件:" + id + "\n 内容:" + text);
			throw e;
		}

	}

	public static String normalize(String text, String id) throws IOException,
			SAXException {
		int start = text.indexOf('<');
		if (start < 0 || start > 0
				&& text.substring(0, start).trim().length() > 0) {
			return "<out xmlns='http://www.xidea.org/lite/core'><![CDATA["
					+ TXT_CDATA_END.matcher(
							TXT_HEADER.matcher(text).replaceAll(""))
							.replaceAll("]]]]><![CDATA[>") + "]]></out>";
		}
		text = new XMLNormalizeImpl().normalize(text, id);
		return text;
	}

	private static final String ORDERED_NODE_KEY = ParseUtil.class.getName()
			+ "#ORDERED_NODE_KEY";

	private static final UserDataHandler VOID_HANDLER = new UserDataHandler() {
		public void handle(short operation, String key, Object data, Node src,
				Node dst) {
		}
	};

	static List<Attr> getOrderedNSAttrList(Element el) {
		@SuppressWarnings("unchecked")
		ArrayList<Attr> list = (ArrayList<Attr>) el
				.getUserData(ORDERED_NODE_KEY);
		if (list == null) {
			list = new ArrayList<Attr>();
			final String info = el.getAttributeNS(ParseUtil.CORE_URI,
					ParseUtil.CORE_INFO);
			NamedNodeMap attrs = el.getAttributes();
			int len = attrs.getLength();
			for (int i = len - 1; i >= 0; i--) {
				Attr attr = (Attr) attrs.item(i);
				String ln = getLocalName(attr);
				String ns = attr.getNamespaceURI();
				if (ns != null && !(ParseUtil.CORE_INFO.equals(ln))
				// && ParseUtil.CORE_URI.equals(ns)
				) {
					list.add(attr);
				}
			}
			if (info.length() > 0) {
				Collections.sort(list, new Comparator<Attr>() {
					// + a positive integer as the first argument is less
					// than(in order )
					public int compare(Attr o1, Attr o2) {
						int p1 = info.indexOf('|' + o1.getNodeName() + '|');
						int p2 = info.indexOf('|' + o2.getNodeName() + '|');
						if (p1 == -1 || p2 == -1) {
							return p1 - p2;// -1,-1=>0,-1,1=>-,1,-1=>+
						}
						return p2 - p1;
					}
				});
			}
			el.setUserData(ORDERED_NODE_KEY, list, VOID_HANDLER);
		}
		return list;
	}

	// public static String getNodePosition(Node node) {
	// Element el = null;
	// switch(node.getNodeType()){
	// case Node.ELEMENT_NODE:
	// el = (Element)node;
	// break;
	// case Node.ATTRIBUTE_NODE:
	// el = ((Attr)node).getOwnerElement();
	// break;
	// case Node.DOCUMENT_NODE:
	// el = ((Document)node).getDocumentElement();
	// }
	//		
	// if (el != null) {
	// Document doc = el.getOwnerDocument();
	// String pos = el.getAttributeNS(ParseUtil.CORE_URI, ParseUtil.CORE_INFO);
	// if(pos.length()>0){
	// int p = pos.indexOf('|');
	// if(p>0){
	// pos = pos.substring(0,p);
	// }
	// pos = "@"+node.getNodeName()+"["+pos+"]";
	// }
	// if(doc!=null){
	// String path = doc.getDocumentURI();
	// return path+pos;
	// }else{
	// return pos;
	// }
	//			
	// }
	// return null;
	// }
	private static String getXMLInstruction(String text) throws IOException {
		String ins = null;
		if (text.startsWith("<?xml")) {
			int end = text.indexOf("?>");
			if (end > 0) {
				ins = text.substring(6, end);
			}
		}
		return ins;
	}

	public static String getLocalName(Node node) {
		String ln = node.getLocalName();
		if (ln == null) {
			ln = node.getNodeName();
			int p = ln.indexOf(':');
			if (p > 0) {
				ln = ln.substring(p + 1);
			}
		}
		return ln;
	}

	private static Pattern TXT_HEADER = Pattern.compile("^#.*[\r\n]+");
	private static Pattern TXT_CDATA_END = Pattern.compile("]]>");

	public static NodeList selectByXPath(Node currentNode, String xpath)
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

	public static String loadTextAndClose(InputStream in, String defaultCharset)
			throws IOException {
		return loadTextAndClose(in, null,false, defaultCharset);
	}

	public static String loadXMLSourceAndClose(InputStream in,
			String[] rtCharset) throws IOException {
		return loadTextAndClose(in, rtCharset, true,"UTF-8");
	}

	/**
	 * FE FF UTF-16, big-endian FF FE UTF-16, little-endian EF BB BF UTF-8
	 * 如果有BOM，取BOM的编码设置。 如果有<?xml 取XML 编码设置。 如果defaultCharset 有值，
	 * 优先采用defaultCharset
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String loadTextAndClose(InputStream in, String[] rtCharset,
			boolean isXML,String defaultCharset) throws IOException {
		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		InputStreamReader cin = getSourceReader(in,isXML);
		if (cin != null) {
			if (rtCharset != null) {
				rtCharset[0] = cin.getEncoding();
			}
			return loadTextAndClose(cin);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			write(in, out);
		} finally {
			in.close();
		}
		byte[] data = out.toByteArray();
		if (defaultCharset != null) {
			String t = getText(data, rtCharset, defaultCharset);
			if (t != null) {
				return t;
			}
		}
		return getText(data, rtCharset, CHARSETS);
	}

	private static String getText(byte[] data, String[] rtCharset,
			String... charsets) throws UnsupportedEncodingException {
		for (String c : charsets) {
			String t = new String(data, c);
			byte[] data2 = t.getBytes(c);
			if (Arrays.equals(data, data2)) {
				if (rtCharset != null) {
					rtCharset[0] = c;
				}
				return t;
			}
		}
		return null;
	}

	private static InputStreamReader getSourceReader(InputStream bin,boolean isXML)
			throws IOException {
		InputStreamReader cin = getBOMReader(bin);
		if (cin != null) {
			return cin;
		} else if(isXML){
			bin.mark(256);
			cin = new InputStreamReader(bin, "UTF-8");
			int c = 0;
			for (int i = 0; i < 128; i++) {
				c = cin.read();
				if (Character.isWhitespace(c)) {
				} else {
					break;
				}
			}
			if (c == '<') {
				String charset = "UTF-8";
				if (cin.read() == '?') {
					StringBuilder buf = new StringBuilder();
					int len = 32 - 2;
					while (len-- > 0) {
						c = cin.read();
						if (c == '>' || c < 0) {
							String line = buf.toString().replace('\'', '"');
							if (line.startsWith("xml")) {
								int p0 = line.indexOf("charset");
								if (p0 > 0) {
									int p1 = line.indexOf('"', p0);
									int p2 = line.indexOf('"', p1);
									if (p1 > 0 && p2 > 0) {
										charset = line.substring(p1 + 1, p2);
									}
								}
							}
							break;
						}
					}
				}
				bin.reset();
				return new InputStreamReader(bin, charset);
			}
		}
		bin.reset();
		return null;
	}

	private static InputStreamReader getBOMReader(InputStream bin)
			throws IOException {
		// //\ufeff %EF%BB%BF
		bin.mark(3);
		if (bin.read() == 0xEF && bin.read() == 0xBB && bin.read() == 0xBF) {
			// readUTF8;
			return new InputStreamReader(bin, "utf-8");
		}
		bin.reset();
		if (bin.read() == 0xFE && bin.read() == 0xFF) {
			return new InputStreamReader(bin, "UTF16BE");
		}
		bin.reset();
		if (bin.read() == 0xFF && bin.read() == 0xFE) {
			return new InputStreamReader(bin, "UTF16LE");
		}
		bin.reset();
		return null;
	}

	private static String loadTextAndClose(Reader in) throws IOException {
		try {
			StringBuilder out = new StringBuilder();
			int count;
			char[] cbuf = new char[1024];
			while ((count = in.read(cbuf)) > -1) {
				out.append(cbuf, 0, count);
			}
			if (out.length() > 0 && out.charAt(0) == '\ufeff') {
				return out.substring(1);
			}
			return out.toString();

		} finally {
			in.close();
		}
	}

	private static void write(InputStream in, OutputStream out)
			throws IOException {
		byte[] data = new byte[64];
		int i;
		while ((i = in.read(data)) >= 0) {
			out.write(data, 0, i);
		}
	}

	static boolean isFile(URI uri) {
		return "file".equals(uri.getScheme());
	}

	static final String replaceBigChar(String text, String encoding) {
		if (encoding == null || encoding.length() == 0) {
			encoding = "GBK";
		} else if (encoding.equalsIgnoreCase("utf-8")) {
			return text;
		}
		Charset charset = Charset.forName(encoding);
		CharsetEncoder encoder = charset.newEncoder();
		StringBuilder buf = new StringBuilder();
		for (char c : text.toCharArray()) {
			if (encoder.canEncode(c)) {
				buf.append(c);
			} else {
				buf.append("&#" + ((int) c) + ';');
			}
		}
		text = buf.toString();
		return text;
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
				prefixMap.put(ParseUtil.getLocalName(attr), value);
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
