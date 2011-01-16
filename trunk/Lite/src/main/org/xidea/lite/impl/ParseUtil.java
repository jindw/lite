package org.xidea.lite.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.jsi.JSIRuntime;
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

	/**
	 * 删除多余的xml空格，如果有换行，尽量保留换行，方便阅读
	 * @param text
	 * @return
	 */
	static String safeTrim(String text) {
		StringBuffer buf = new StringBuffer(text);
		final int len = buf.length();
		if(len == 0){
			return "";
		}
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
		return String.valueOf(s1);
	}

	private static InputStream openStream(URI uri, ParseContext context)
			throws IOException {
		return context == null ? openStream(uri) : context.openStream(uri);
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
	static String readTo(InputStream in,char...cs) throws IOException{
		StringBuilder buf = new StringBuilder();
		int c;
		int i=-cs.length;//-2
		read:while((c = in.read())>=0){//?>
			buf.append((char)c);
			i++;//-1
			if(i>=0){
				for (int j = 0; j <cs.length ; j++) {
					if(cs[j] != buf.charAt(i+j)){
						continue read;
					}
				}
				return buf.toString();
			}
		}
		return null;
	}
	private static Pattern XMLNS_CDEC = Pattern.compile("\\sxmlns\\:c\\b");
	private static Pattern XMLNS_CUSE = Pattern.compile("\\bc:\\w+\\b");
	private static Pattern XMLNS_FE = Pattern.compile("<[\\w\\-\\:]+");
	private static Pattern XMLNS_ENCODE = Pattern.compile("<\\?xml\\s*[^>]*?encoding\\s*=\\s*['\"]([\\w\\-]+)['\"]");

	public static Document parse(URI uri, ParseContext context)
			throws IOException, SAXException {
		String id = uri.toString();
		InputStream in1 = ParseUtil.trimBOM(context,uri,null);
		try{
			return loadXML(in1, id);
		} catch (SAXParseException e) {
			String source = loadText(uri, context);
			if(!XMLNS_CDEC.matcher(source).find() && XMLNS_CUSE.matcher(source).find()){
				log.warn("缺乏名称空间申明："+id);
				source = XMLNS_FE.matcher(source).replaceFirst("$0 xmlns:c='http://www.xidea.org/lite/core'");
			}
			try{
				Matcher m = XMLNS_ENCODE.matcher(source);
				String encoding = "UTF-8";
				if(m.find()){
					encoding =  m.group(1);
				}
				return loadXML(new ByteArrayInputStream(source.getBytes(encoding)), id);
			}catch (Exception ex) {
			}
			throw new SAXException("XML Parser Error:" + id + "("
						+ e.getLineNumber() + "," + e.getColumnNumber() + ")\r\n"
						+ e.getMessage());
		} finally {
			in1.close();
		}
	}

	private static Document loadXML(InputStream in, String id)
			throws IOException, SAXException {
		in.mark(1024);
		if (in.read() != '<') {
			return null;
		}
		String ins = getXMLInstruction(in);
		in.reset();
		Document xml = documentBuilder.parse(in, id);
		if(ins!=null){
			xml.insertBefore(xml.createProcessingInstruction("xml", ins),xml.getFirstChild());
		}
		return xml;
		
	}

	private static String getXMLInstruction(InputStream in) throws IOException {
		String ins = null;
		if(in.read() == '?'){
			in.reset();
			ins = readTo(in, '?','>');
			if(ins.startsWith("<?xml")){
				ins = ins.substring(6, ins.length()-2);
			}
		}
		return ins;
	}


	private static Pattern TXT_HEADER = Pattern.compile("^#.*[\r\n]+");
	private static Pattern TXT_CDATA_END = Pattern.compile("]]>");
	
	public static Document loadXML(String path, ParseContext context)
			throws SAXException, IOException {
		URI uri;
		if(path.startsWith("#")){
			path = "<out xmlns='http://www.xidea.org/lite/core'><![CDATA["+
				TXT_CDATA_END.matcher(TXT_HEADER.matcher(path).replaceAll("")).replaceAll("]]]]><![CDATA[>")+
				"]]></out>";
		}
		if (path.startsWith("<")) {
			uri = createSourceURI(path);
		} else if (context != null) {
			uri = context.createURI(path);
		} else {
			uri = URI.create(path);
		}
		return parse(uri, context);
	}

	private static URI createSourceURI(String path) {
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

	public static String loadText(URI uri, ParseContext parseContext) throws IOException {
		StringBuilder buf = new StringBuilder();
		InputStream in = trimBOM(parseContext,uri, buf);
		in.close();
		return buf.toString();
	}
	private static InputStream trimBOM(ParseContext context,URI uri,StringBuilder out) throws IOException{
		try{
			return trimBOM(openStream(uri, context),out);
		}catch(IOException e){
			log.warn(uri+"读取异常:",e);
			throw e;
		}catch(RuntimeException e){
			log.warn(uri+"读取异常:",e);
			throw e;
		}
	}

	private static InputStream trimBOM(InputStream in,StringBuilder out) throws IOException {
		in = new BufferedInputStream(in, 1024);
//		int trim = 0;
		in.mark(1024);
		String charset = null;
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
//					trim = 2;
					//{Big5=Big5, Big5-HKSCS=Big5-HKSCS, EUC-JP=EUC-JP, EUC-KR=EUC-KR, GB18030=GB18030, GB2312=GB2312, GBK=GBK, IBM-Thai=IBM-Thai, IBM00858=IBM00858, IBM01140=IBM01140, IBM01141=IBM01141, IBM01142=IBM01142, IBM01143=IBM01143, IBM01144=IBM01144, IBM01145=IBM01145, IBM01146=IBM01146, IBM01147=IBM01147, IBM01148=IBM01148, IBM01149=IBM01149, IBM037=IBM037, IBM1026=IBM1026, IBM1047=IBM1047, IBM273=IBM273, IBM277=IBM277, IBM278=IBM278, IBM280=IBM280, IBM284=IBM284, IBM285=IBM285, IBM297=IBM297, IBM420=IBM420, IBM424=IBM424, IBM437=IBM437, IBM500=IBM500, IBM775=IBM775, IBM850=IBM850, IBM852=IBM852, IBM855=IBM855, IBM857=IBM857, IBM860=IBM860, IBM861=IBM861, IBM862=IBM862, IBM863=IBM863, IBM864=IBM864, IBM865=IBM865, IBM866=IBM866, IBM868=IBM868, IBM869=IBM869, IBM870=IBM870, IBM871=IBM871, IBM918=IBM918, ISO-2022-CN=ISO-2022-CN, ISO-2022-JP=ISO-2022-JP, ISO-2022-JP-2=ISO-2022-JP-2, ISO-2022-KR=ISO-2022-KR, ISO-8859-1=ISO-8859-1, ISO-8859-13=ISO-8859-13, ISO-8859-15=ISO-8859-15, ISO-8859-2=ISO-8859-2, ISO-8859-3=ISO-8859-3, ISO-8859-4=ISO-8859-4, ISO-8859-5=ISO-8859-5, ISO-8859-6=ISO-8859-6, ISO-8859-7=ISO-8859-7, ISO-8859-8=ISO-8859-8, ISO-8859-9=ISO-8859-9, JIS_X0201=JIS_X0201, JIS_X0212-1990=JIS_X0212-1990, KOI8-R=KOI8-R, KOI8-U=KOI8-U, Shift_JIS=Shift_JIS, TIS-620=TIS-620, US-ASCII=US-ASCII, UTF-16=UTF-16, UTF-16BE=UTF-16BE, UTF-16LE=UTF-16LE, UTF-32=UTF-32, UTF-32BE=UTF-32BE, UTF-32LE=UTF-32LE, UTF-8=UTF-8, windows-1250=windows-1250, windows-1251=windows-1251, windows-1252=windows-1252, windows-1253=windows-1253, windows-1254=windows-1254, windows-1255=windows-1255, windows-1256=windows-1256, windows-1257=windows-1257, windows-1258=windows-1258, windows-31j=windows-31j, x-Big5-Solaris=x-Big5-Solaris, x-euc-jp-linux=x-euc-jp-linux, x-EUC-TW=x-EUC-TW, x-eucJP-Open=x-eucJP-Open, x-IBM1006=x-IBM1006, x-IBM1025=x-IBM1025, x-IBM1046=x-IBM1046, x-IBM1097=x-IBM1097, x-IBM1098=x-IBM1098, x-IBM1112=x-IBM1112, x-IBM1122=x-IBM1122, x-IBM1123=x-IBM1123, x-IBM1124=x-IBM1124, x-IBM1381=x-IBM1381, x-IBM1383=x-IBM1383, x-IBM33722=x-IBM33722, x-IBM737=x-IBM737, x-IBM834=x-IBM834, x-IBM856=x-IBM856, x-IBM874=x-IBM874, x-IBM875=x-IBM875, x-IBM921=x-IBM921, x-IBM922=x-IBM922, x-IBM930=x-IBM930, x-IBM933=x-IBM933, x-IBM935=x-IBM935, x-IBM937=x-IBM937, x-IBM939=x-IBM939, x-IBM942=x-IBM942, x-IBM942C=x-IBM942C, x-IBM943=x-IBM943, x-IBM943C=x-IBM943C, x-IBM948=x-IBM948, x-IBM949=x-IBM949, x-IBM949C=x-IBM949C, x-IBM950=x-IBM950, x-IBM964=x-IBM964, x-IBM970=x-IBM970, x-ISCII91=x-ISCII91, x-ISO-2022-CN-CNS=x-ISO-2022-CN-CNS, x-ISO-2022-CN-GB=x-ISO-2022-CN-GB, x-iso-8859-11=x-iso-8859-11, x-JIS0208=x-JIS0208, x-JISAutoDetect=x-JISAutoDetect, x-Johab=x-Johab, x-MacArabic=x-MacArabic, x-MacCentralEurope=x-MacCentralEurope, x-MacCroatian=x-MacCroatian, x-MacCyrillic=x-MacCyrillic, x-MacDingbat=x-MacDingbat, x-MacGreek=x-MacGreek, x-MacHebrew=x-MacHebrew, x-MacIceland=x-MacIceland, x-MacRoman=x-MacRoman, x-MacRomania=x-MacRomania, x-MacSymbol=x-MacSymbol, x-MacThai=x-MacThai, x-MacTurkish=x-MacTurkish, x-MacUkraine=x-MacUkraine, x-MS932_0213=x-MS932_0213, x-MS950-HKSCS=x-MS950-HKSCS, x-mswin-936=x-mswin-936, x-PCK=x-PCK, x-SJIS_0213=x-SJIS_0213, x-UTF-16LE-BOM=x-UTF-16LE-BOM, X-UTF-32BE-BOM=X-UTF-32BE-BOM, X-UTF-32LE-BOM=X-UTF-32LE-BOM, x-windows-50220=x-windows-50220, x-windows-50221=x-windows-50221, x-windows-874=x-windows-874, x-windows-949=x-windows-949, x-windows-950=x-windows-950, x-windows-iso2022jp=x-windows-iso2022jp}

					//UTF-16BE, UTF-16LE
					//FFFE :UTF-16LE
					charset = c == 0xFE?"UTF-16LE":"UTF-16BE";
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
				if (i == 2) {
					charset = "UTF-8";
//					trim = 3;
				}
				break outer;
			default:
				break outer;
			}
		}
		if(charset==null){
			in.reset();
		}
		if(out!=null){
			ByteArrayOutputStream out2 = new ByteArrayOutputStream();
			write(in, out2);
			if(charset==null){
				byte[] data = out2.toByteArray();
				String s = loadString(data,"UTF-8","GBK");
				if(s == null){
					Set<String> ks = Charset.availableCharsets().keySet();
					s = loadString(data, ks.toArray(new String[ks.size()]));
				}
				out.append(s);
			}else{
				out.append(out2.toString(charset));
			}
			return null;
		}
		return in;
	}
	private static String loadString(byte[] data,String...cs) throws UnsupportedEncodingException{
		for(String c:cs){
			String rtv = new String(data,c);
			byte[] data2 = rtv.getBytes(c);
			if(Arrays.equals(data, data2)){
				return rtv;
			}
		}
		return null;
	}

	private static void write(InputStream in, OutputStream out)
			throws IOException {
		byte[] data = new byte[64];
		int i ;
		while((i = in.read(data))>=0){
			out.write(data,0,i);
		}
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
