package org.xidea.lite.parser.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xidea.lite.parser.impl.dtd.DefaultEntityResolver;
import org.xml.sax.SAXException;

public class XMLFixerImpl {
	private static final Log log = LogFactory.getLog(XMLFixerImpl.class);
	//private static final String DOCTYPE_HTML_4_01 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";
	private static final String ISO8859_1 = "ISO8859-1";
	private static final Pattern html5doctype = Pattern.compile(
			"^\\s*(<\\!doctype\\s+html\\s*>)", Pattern.CASE_INSENSITIVE);
	
	private static final Pattern htmlSingle = Pattern
			.compile(
					"(<(?:link|input|meta|img|br|hr)\\b(?:'[^']*'|\"[^\"]*\"|[^'\">])*?)/?>\\s*(?:</[\\w]+>)?",
					Pattern.CASE_INSENSITIVE);
	private static final String DEFAULT_STARTS = ("<!DOCTYPE html PUBLIC '"
			+ DefaultEntityResolver.DEFAULT__HTML_DTD + "' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>");
	private final static Pattern encodingPattern = Pattern
			.compile("^(?:(\\s*<\\?xml[^>]+encoding=['\"])" + "([\\w\\-]+)"
					+ "([\\s\\S]*)" + ")?$");
	private final static Pattern rootPattern = Pattern.compile("<[\\w\\-]+");
	// &#[integer]; 或者 &#x[hex];
	private final static Pattern replacePattern = Pattern
			.compile("<!--[\\s\\S]*-->|" + "<!\\[CDATA\\[[\\s\\S]*\\]\\]>|"
					+ "<." + "&&|" + "&\\w+;|" + "&#\\d+;|"
					+ "&#x[\\da-fA-F]+;|" + "&");
	private final static Pattern coreUsePattern = Pattern.compile(
			"<c\\:[\\w\\-]+", Pattern.CASE_INSENSITIVE);
	private final static Pattern coreDecPattern = Pattern.compile(
			"\\sxmlns:c\\s*=\\s*['\"]", Pattern.CASE_INSENSITIVE);

	public static InputStream create(byte[] data) throws IOException {
		String text = new String(data, ISO8859_1);
		String encoding = encodingPattern.matcher(text).replaceAll("$2");
		if (encoding.equals(text)) {
			encoding = "UTF-8";
			text = toString(data, new String[] { "UTF-8", "GB18030" });
		} else {
			String[] ens = new String[] { encoding, "UTF-8", "GB18030" };
			text = toString(data, ens);
			if (ens[0] != encoding) {
				log.error("XML 编码错误");
				text = encodingPattern.matcher(text).replaceAll(
						"$1" + ens[0] + "$3");
				encoding = ens[0];
			}
		}
		Matcher matcher = html5doctype.matcher(text);
		if(matcher.find()){
			String key = matcher.group(1);
			String replacement = "<!DOCTYPE html PUBLIC '"
				+ DefaultEntityResolver.DEFAULT__HTML_DTD + "' '"+URLEncoder.encode(key,"UTF-8")+"'>";
			text = html5doctype.matcher(text).replaceFirst(replacement);
			
		}
		text = replaceInValidChar(text);
		if (coreUsePattern.matcher(text).find()
				&& !coreDecPattern.matcher(text).find()) {
			text = rootPattern.matcher(text).replaceFirst(
					"$0 xmlns:c='" + ParseUtil.CORE_URI + "'");
		}
		text = text.trim();
		// xml 都剔除至 <开头
		if (text.indexOf("<!") != 0) {
			text = DEFAULT_STARTS + text;
		}
		return new ByteArrayInputStream(text.getBytes(encoding));

	}

	static String replaceInValidChar(String text) {
		Matcher matchs = replacePattern.matcher(text);
		int begin = 0;
		StringBuilder buf = new StringBuilder();
		while (matchs.find()) {
			int start = matchs.start();
			buf.append(text.substring(begin, start));
			String token = matchs.group();
			if (token.length() == 2 && token.charAt(0) == '<') {
				int c = token.charAt(1);
				if (c != '/') {
					if (c == '$' || c != '!' && c != '?'
							&& !Character.isJavaIdentifierStart(c)) {
						token = "&lt;" + c;
					}
				}
			} else if (token.equals("&&")) {
				token = "&amp;&amp;";
			} else if (token.equals("&nbsp;")) {
				token = "&#160;";
			} else if (token.equals("&")) {
				token = "&amp;";
			}
			buf.append(token);
			begin = matchs.end();
		}
		buf.append(text.substring(begin));
		text = buf.toString();
		return text;
	}

	static String toString(byte[] data, String[] encodingList)
			throws UnsupportedEncodingException {
		for (String encoding : encodingList) {
			String text = new String(data, encoding);
			if (Arrays.equals(data, text.getBytes(encoding))) {
				encodingList[0] = encoding;
				return text;
			}
		}
		return null;
	}

	public Document parse(DocumentBuilder documentBuilder, InputStream in,
			String uri) throws IOException {
		log.warn("开始XML错误修复");
		String text = ParseUtil.loadText(in, ISO8859_1);
		Document doc= null;
		try {
			doc= doParse(documentBuilder, text, uri);
		} catch (NullPointerException e) {
			log.error("JDK1.6.0.14的灵异问题。。。。");
		}finally{
			if(doc ==null){
				log.error("XML错误修复失败");
			}else{
				log.warn("成功修复XML错误语法");
			}
		}
		return doc;
	}

	private Document doParse(DocumentBuilder documentBuilder, String text,
			String uri) throws IOException, UnsupportedEncodingException {
		byte[] data = text.getBytes(ISO8859_1);
		try {
			log.warn("修复特殊字符");
			return documentBuilder.parse(create(data), uri);
		} catch (SAXException e) {
			log.warn("修复XML匹对（link|input|meta|img|br|hr）");
			text = htmlSingle.matcher(text).replaceAll("$1/>");
			data = text.getBytes(ISO8859_1);
			try {
				return documentBuilder.parse(create(data), uri);
			} catch (SAXException e2) {
				text = rootPattern.matcher(text).replaceFirst(
						"<c:group xmlns:c='" + ParseUtil.CORE_URI + "'>$0")
						+ "</c:group>";
				data = text.getBytes(ISO8859_1);
				try {
					return documentBuilder.parse(create(data), uri);
				} catch (SAXException e3) {
					log.error(e3);
				}
			}
		}
		return null;
	}

	public static void main(String[] args) throws Exception {

		Document doc = new XMLFixerImpl().parse(
				new XMLContextImpl(null).documentBuilder,
				new ByteArrayInputStream("<br><br>".getBytes()), "/uri");
		System.out.println(doc.toString());
	}

}
