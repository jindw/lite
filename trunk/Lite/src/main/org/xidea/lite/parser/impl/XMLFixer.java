package org.xidea.lite.parser.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xidea.lite.parser.impl.dtd.DefaultEntityResolver;
import org.xml.sax.SAXException;

public class XMLFixer{
	private static final String ISO8859_1 = "ISO8859-1";
	private static final Log log = LogFactory.getLog(XMLFixer.class);
	private static final String html5doctype = "<!doctype html>";
	private static final String DEFAULT_STARTS = ("<!DOCTYPE html PUBLIC '"+DefaultEntityResolver.DEFAULT__HTML_DTD+"' '.'>");
	private final static Pattern encodingPattern = Pattern.compile("^(?:(\\s*<\\?xml[^>]+encoding=['\"])" +
			"([\\w\\-]+)" +
			"([\\s\\S]*)" +
			")?$");
	private final static Pattern rootPattern = Pattern.compile("<[\\w\\-]");
	//
	private final static Pattern replacePattern = Pattern.compile("<!--[\\s\\S]*-->|" +
			"<!\\[CDATA\\[[\\s\\S]*\\]\\]>|" +
			"&&|" +
			"&nbsp;|" +
			"<.|");
	private final static Pattern coreUsePattern = Pattern.compile("<c\\:[\\w\\-]+",Pattern.CASE_INSENSITIVE);
	private final static Pattern coreDecPattern = Pattern.compile("\\sxmlns:c\\s*=\\s*['\"]",Pattern.CASE_INSENSITIVE);
	public static InputStream create(byte[] data) throws IOException{
		String text = new String(data,ISO8859_1);
		String encoding = encodingPattern.matcher(text).replaceAll("$2");
		if(encoding.equals(text)){
			encoding = "UTF-8";
		}
		String[] ens = new String[]{encoding,"UTF-8","GB18030"};
		text = toString(data,ens);
		if(ens[0] != encoding){
			log.error("XML 编码错误");
			text = encodingPattern.matcher(text).replaceAll("$1"+ens[0]+"$3");
			encoding = ens[0];
		}
		
		if(text.startsWith(html5doctype)){
			text = html5doctype.toUpperCase()+text.substring(html5doctype.length());
		}
		text = replaceInValidChar(text);
		if( coreUsePattern.matcher(text).find()&& !coreDecPattern.matcher(text).find()){
			text = rootPattern.matcher(text).replaceFirst("$0 xmlns:c='"+ParseUtil.CORE_URI+"'");
		}
		return new ByteArrayInputStream(text.getBytes(encoding));
		
	}
	static String replaceInValidChar(String text) {
		Matcher matchs = replacePattern.matcher(text);
		int begin = 0;
		StringBuilder buf = new StringBuilder();
		while(matchs.find()){
			int start = matchs.start();
			buf.append(text.substring(begin,start));
			String token = matchs.group();
			if(token.length() == 2 && token.charAt(0) == '<'){
				int c = token.charAt(1);
				if(c != '/' ){
					if(c == '$' ||
							c != '!' && c != '?' && !Character.isJavaIdentifierStart(c)){
						token = "&lt;"+c;
					}
				}
			}else if(token.equals("&&")){
				token = "&amp;&amp;";
			}
			buf.append(token);
			begin = matchs.end();
		}
		buf.append(text.substring(begin));
		text = buf.toString();
		return text;
	}
	static String toString(byte[] data,String[] encodingList) throws UnsupportedEncodingException{
		for(String encoding : encodingList){
			String text = new String(data,encoding);
			if(Arrays.equals(data, text.getBytes(encoding))){
				encodingList[0] = encoding;
				return text;
			}
		}
		return null;
	}
	public Document parse(DocumentBuilder documentBuilder, InputStream in,String uri) throws IOException {

		String text = ParseUtil.loadText(in, ISO8859_1);
		byte[] data =text.getBytes(ISO8859_1);
		try {
			in = create(data);
			return documentBuilder.parse(in, uri);
		} catch (SAXException e) {
			text = DEFAULT_STARTS+text;
			data =text.getBytes(ISO8859_1);
			in = create(data) ;
			try {
				return documentBuilder.parse(in, uri);
			} catch (SAXException e2) {
				text = rootPattern.matcher(text).replaceFirst("<c:group xmlns:c='"+ParseUtil.CORE_URI+"'>$0")+"</c:group>";
				System.out.println(text);
				data =text.getBytes(ISO8859_1);
				in = create(data) ;
				try {
					return documentBuilder.parse(in, uri);
				} catch (SAXException e3) {
					log.error(e3);
				}
			}
		}
		return null;
	}

}
