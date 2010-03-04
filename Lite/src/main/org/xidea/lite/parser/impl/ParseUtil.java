package org.xidea.lite.parser.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.parser.ParseContext;

class ParseUtil {

	private static Log log = LogFactory.getLog(CoreXMLNodeParser.class);
	private static final Pattern TEMPLATE_NAMESPACE_CORE = Pattern
			.compile("^http:\\/\\/www.xidea.org\\/ns\\/(?:template|lite)(?:\\/core)?\\/?$");

	static final String CORE_URI = "http://www.xidea.org/ns/lite/core/";
	static boolean isCoreNS(String prefix, String url) {
		return ("c".equals(prefix) && ("#".equals(url) || "#core".equals(url)))
				|| TEMPLATE_NAMESPACE_CORE.matcher(url).find();
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
			log.warn("输入的不是有效el，系统将字符串转换成el");
			value = JSONEncoder.encode(value);
		}
		return context.parseEL(value);
	}
	static String loadText(InputStream in,String charset) throws IOException {
		return loadText(new InputStreamReader(in,charset));
	}

	static InputStream trimBOM(InputStream in) throws IOException {
		in = new BufferedInputStream(in, 3);
		int trim = 0;
		in.mark(3);
		outer:for(int i = 0;i<3;i++){// bugfix \ufeff
			//Unicode(UTF-16)      FF-FE      31 00 32 00 33 00 34 00
			//Unicode big endian   FE-FF      00 31 00 32 00 33 00 34
			//UTF-8                 EF-BB-BF 31 32 33 34
			//ASCII                         31 32 33 34
			
			int c = in.read();
			switch(c){
			//UTF-16
			case 0xFF:
			case 0xFE:
				if(i == 1){
					trim = 2;
					break outer;
				}else if(i>1){
					break outer;
				}
			//UTF-8
			case 0xEF:
				if(i != 0){
					break outer;
				}
				break;
			case 0xBB:
				if(i != 1){
					break outer;
				}
				break;
			case 0xBF:
				if(i != 2){
					break outer;
				}else{
					trim=3;
				}
				break;
			default:
				break outer;
			}
		};
		in.reset();
		while(trim-->0){
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
