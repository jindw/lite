package org.xidea.lite.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONEncoder;

/**
 * @author jindawei
 * @see org.xidea.lite.parser.impl.xml.test.XMLNormalizeTest
 */
public class XMLNormalizeImpl {
	private static final Log log = LogFactory.getLog(XMLNormalizeImpl.class);
	protected static final String TAG_NAME = "[a-zA-Z_](?:[\\w_\\-\\.\\:]*[\\w_\\-\\.])?";

	private static final String NS_CORE = "http://www.xidea.org/lite/core";
	private static final Pattern OLD_NS_CORE = Pattern
			.compile("^(?:http://www.xidea.org/ns/(?:lite|template)/core/?|http://firekylin.my.baidu.com/ns/2010)$");
	// key (= value)?
	protected static final Pattern ELEMENT_ATTR_END = Pattern
			.compile("(?:^|\\s+|\\b)("
					+ TAG_NAME
					+ ")(?:\\s*=\\s*('[^']*'|\"[^\"]*\"|\\w+|\\$\\{[^}]+\\}))?|\\s*\\/?>");
	protected static final Pattern XML_TEXT = Pattern
			.compile("&\\w+;|&#\\d+;|&#x[\\da-fA-F]+;|([&\"\'<])");
	protected static final Set<String> DEFAULT_LEAF_SET ;
		
	public final static Map<String, String> DEFAULT_ENTRY_MAP;
	public final static Map<String, String> DEFAULT_NS_MAP;
	static {
		Map<String, String> map = new HashMap<String, String>();
		map.put("&copy;", "&#169;");
		map.put("&nbsp;", "&#160;");
		DEFAULT_ENTRY_MAP = Collections.unmodifiableMap(map);
		map = new HashMap<String, String>();
		map.put("xmlns:c", NS_CORE);
		map.put("xmlns", "http://www.w3.org/1999/xhtml");
		DEFAULT_NS_MAP = Collections.unmodifiableMap(map);
		Set<String> set = new HashSet<String>();
		set.add("meta");
		set.add("link");
		set.add("img");
		set.add("br");
		set.add("hr");
		set.add("input");
		DEFAULT_LEAF_SET = Collections.unmodifiableSet(set);
	}

	protected Map<String, String> defaultNSMap = DEFAULT_NS_MAP;
	protected Map<String, String> defaultEntryMap = DEFAULT_ENTRY_MAP;
	protected Set<String> defaultLeafSet;
	protected String documentStart = "<c:group xmlns:c='http://www.xidea.org/lite/core'>";
	protected String documentEnd = "</c:group>";

	protected int start;
	protected int rootCount;
	protected String text;
	private TextPosition textPosition;
	protected TagAttr tag;
	protected StringBuilder result;
	protected String uri;

	public XMLNormalizeImpl(Map<String, String> defaultNSMap,
			Map<String, String> defaultEntryMap) {
		this.defaultNSMap = defaultNSMap;
		this.defaultEntryMap = defaultEntryMap;
		defaultLeafSet = new HashSet<String>(DEFAULT_LEAF_SET);
	}

	public XMLNormalizeImpl() {
		defaultLeafSet = new HashSet<String>(DEFAULT_LEAF_SET);
	}

	protected class TagAttr {
		int start =  XMLNormalizeImpl.this.start;
		String name;
		Map<String, String> nsMap;
		TagAttr parentTag = tag;
		ArrayList<String> attrs = new ArrayList<String>();
	}

	private void addAttr(String space, String name, String value,char qute) {
		if(tag.name == null){
			error("tagName is required");
		}
		int prefixIndex = name.indexOf(':');
		if (name.equals("XMLNS")) {// format html xmlns
			name = "xmlns";
		} else if (prefixIndex == 5) {
			if (name.startsWith("XMLNS")) {// format html xmlns
				name = "xmlns" + name.substring(5);
			}
			if (name.startsWith("xmlns:") && OLD_NS_CORE.matcher(value).find()) {
				value = NS_CORE;
			}
		}
		if (prefixIndex > 0 || name.equals("xmlns")) {
			if (tag.nsMap == null) {
				tag.nsMap = new HashMap<String, String>();
			}
			tag.nsMap.put(name, value);
		}
		if (tag.attrs.contains(name)) {
			error("attribute " + name + "is existed!!");
			return;
		}
		tag.attrs.add(name);
		result.append(space);
		result.append(name);
		result.append('=');
		result.append(qute);
		result.append(value);
		result.append(qute);
	}

	private void compliteAttr() {
		if (tag.parentTag == null) {// first Node
			if (tag.nsMap == null) {
				tag.nsMap = new HashMap<String, String>();
			}
			for (Map.Entry<String, String> entry : defaultNSMap.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				if (!tag.nsMap.containsKey(key)) {
					int p = key.indexOf(':');
					if(p<0 || key.equals("xmlns:c")|| text.matches("[<\\s]"+key.substring(p+1)+':')){
						tag.nsMap.put(key, value);
						result.append(" ");
						result.append(key);
						result.append("='");
						result.append(value);
						result.append("'");
					}
				}
			}
			// check missed
			tag.nsMap = toNSDecMap(tag.nsMap, defaultNSMap);
		} else {
			if (tag.nsMap == null) {
				tag.nsMap = tag.parentTag.nsMap;
			} else {
				// check missed and init this
				tag.nsMap = toNSDecMap(tag.nsMap, tag.parentTag.nsMap);
			}
		}
		if (tag.nsMap.containsKey("xmlns:c")) {
			result.append(" c:");
			result.append(ParseUtil.CORE_INFO);
			result.append("='");
			result.append(textPosition.getPosition(start));
			if (tag.attrs.size() > 0) {
				result.append('|');
				for (String a : tag.attrs) {
					if (a.indexOf(':') > 0 || a.equals("xmlns")) {
						result.append(a);
						result.append('|');
					}
				}
			}

			result.append("'");
		}
	}

	private Map<String, String> toNSDecMap(Map<String, String> attributeMap,
			Map<String, String> parentNSMap) {
		HashMap<String, String> nnsMap = null;
		for (String key : attributeMap.keySet()) {
			int p = key.indexOf(':');
			String prefix = p > 0 ? key.substring(0, p) : key;
			if (prefix.equals("xmlns")) {// ns define
				if (!parentNSMap.containsKey(key)) {
					if (nnsMap == null) {
						nnsMap = new HashMap<String, String>(parentNSMap);
					}
					nnsMap.put(key, attributeMap.get(key));
				}
			} else if (!prefix.equals("xml")) {
				String dec = "xmlns:" + prefix;
				if (!attributeMap.containsKey(dec)
						&& !parentNSMap.containsKey(dec)) {
					error("unknow namespace prefix:\t" + key
							+ ";\tdefaultNSMap:\t" + defaultNSMap
							+ ";\tnsMap:\t" + attributeMap + ";\tparentMap:\t"
							+ parentNSMap);
				}
			}
		}
		if (nnsMap != null) {
			return nnsMap;
		} else {
			return parentNSMap;
		}
	}

	public String normalize(String text, String uri) {
		this.uri = uri;
		this.text = text;
		this.textPosition = new TextPosition(text);
		this.start = 0;
		this.rootCount = 0;
		this.result = new StringBuilder(text.length() * 11 / 10);
		String result = parse();
		return result.trim();

	}

	private String parse() {
		while (true) {
			int p = text.indexOf('<', start);
			if (p >= start) {
				appendTextTo(p);
				appendElement();
			} else {
				appendEnd();
				break;
			}
		}
		if (this.rootCount > 1) {
			result.append(documentEnd);
			String rtv = result.toString();
			return rtv.replaceFirst("<[\\w_]", documentStart + "$0");
		}
		return result.toString();
	}

	private boolean appendElementStart() {
		final int start = this.start;
		final int len = result.length();
		final Matcher m = ELEMENT_ATTR_END.matcher(text.substring(start + 1));
		int p = 0;
		if (tag == null) {
			rootCount++;
		}
		this.tag = new TagAttr();
		while (m.find()) {
			if (p != m.start()) {
				break;
			}
			String v = m.group();
			this.start = start + 1 + m.end();
			if (v.endsWith(">")) {
				compliteAttr();
				boolean closeTag = v.indexOf('/') >= 0;
				if (closeTag || isLeaf(tag.name)) {
					if (!closeTag) {
						info("标签:" + tag.name + " 未关闭(已修复)");
					}
					result.append("/>");
					tag = tag.parentTag;
				} else {
					result.append('>');
					// for script
					if ("script".equalsIgnoreCase(tag.name)) {

					}
				}
				return true;
			} else {
				final String name = m.group(1);
				String value = m.groupCount() > 1 ? m.group(2) : null;
				if (p == 0) {// process tagName
					if (value != null) {
						error("attribute value without name:" + v);
					}
					// checkTag(name);
					result.append('<');
					result.append(name);
					tag.name = name;
				} else {
					// append space
					String space = v.substring(0, m.start(1) - m.start());

					char qute = '"';
					if (value == null) {
						info("属性:" + name + " 未赋值(已修复)");
						value = name;
					} else {
						char f = value.charAt(0);
						if (f == '"' || f == '\'') {
							qute = f;
							String v1 = value.substring(1, value.length() - 1);
							value = formatXMLValue(v1, name, qute);
						} else {
							info("属性:" + name + " 未使用\"'(已修复)");
							value = formatXMLValue(value, name, qute);
						}
					}
					addAttr(space, name, value,qute);

				}

			}
			p = m.end();
		}

		this.start = start;
		result.setLength(len);
		return false;
	}

	protected void appendElementEnd() {
		String content = sourceTo(">");
		String name = content.substring(2, content.length() - 1);
		if (isLeaf(name)) {
			return;
		}
		if (tag != null) {
			String lastName = tag.name;
			result.append("</");
			result.append(lastName);
			result.append(">");
			tag = tag.parentTag;
			if (!lastName.equalsIgnoreCase(name)) {
				error("end tag(" + name + ") can not match the start("
						+ lastName + ")!");
			}
		} else {
			error("Missed Start Element!");
		}

	}

	protected boolean isLeaf(String name) {
		name = name.toLowerCase();
		if(defaultLeafSet.contains(name)){
			if(this.text.indexOf("</"+name+'>',this.start)>0){
				defaultLeafSet.remove(name);
				return false;
			}
			return true;
		}
		return false;
	}

	private void appendElement() {
		char type = getOffset(1);
		if (type == '?') {
			appendInstruction();
		} else if (type == '!') {
			int type2 = getOffset(2);
			if (type2 == '-') {
				appendComment();
			} else if (type2 == '[') {// <![CDATA[
				appendCDATA();
			} else {// <!DOCTYPE
				appendDTD();
			}
		} else if (type == '/') {
			appendElementEnd();
		} else if (isElementStart(type)) {
			if (!appendElementStart()) {
				start++;
				result.append("&lt;");
			}
		} else {
			start++;
			result.append("&lt;");
		}
	}

	protected void appendDTD() {
		int start = this.start;
		String content = sourceTo(">");
		int p = content.indexOf("<!", 1);
		if (p > 0) {// nest
			this.start = start;
			content = sourceTo("]>");
		}
		if (content.startsWith("<!doctype")) {
			content = "<!DOCTYPE" + content.substring("<!doctype".length());
		}
		result.append(content);
	}

	protected void appendCDATA() {
		String content = sourceTo("]]>");
		result.append(content);
	}

	static Pattern CC = Pattern
			.compile("^(<!--\\[if.*?\\]>)([\\s\\S]*?)(<!\\[endif\\]-->)$");

	protected void appendComment() {
		// <!--[if lt IE 9]><![endif]-->
		// <!--[if expression]> HTML <![endif]-->
		// http://msdn.microsoft.com/en-us/library/ms537512%28v=vs.85%29.aspx
		String content = sourceTo("-->");
		int p = content.indexOf("--", 4);
		if (p != content.lastIndexOf("--")) {// <!--- --> error <!-- --->
			warn("注释中不能出现连续的--");
			content = "<!--"
					+ content.substring(4, content.length() - 2).replaceAll(
							"[\\-]", " -") + "->";
		}
		// <!--[if lt IE 9]><![endif]-->
		Matcher m = CC.matcher(content);
		if (m.find()) {
			String c2 = m.group(2);
			appendIECComment(c2);
		} else {
			result.append(content);
		}
	}

	protected void appendIECComment(String content) {
		content = this.formatXMLValue(
				"$!{" + JSONEncoder.encode(content) + "}", null, '\0');
		result.append(content);
	}

	protected void appendInstruction() {
		String content = sourceTo("?>");
		result.append(content);
	}

	protected void appendTextTo(int p) {
		if (p > start) {
			String text = this.text.substring(start, p);
			String text2 = formatXMLValue(text, null, (char) 0);

			result.append(text2);
			start = p;
		}
	}

	protected String sourceTo(String endText) {
		int end = text.indexOf(endText, start);
		if (end > 0) {
			return text.substring(start, start = end + endText.length());
		} else {
			return null;
		}
	}

	protected boolean isElementStart(char type) {
		return Character.isJavaIdentifierPart(type) && type != '$';
	}

	protected void appendEnd() {
		String end = text.substring(start);
		if (end.trim().length() > 0) {
			warn("异常文件内容:" + end);
		}
	}

	protected void error(String msg) {
		log.error(position(msg));
	}

	protected void warn(String msg) {
		log.warn(position(msg));
	}

	protected void info(String msg) {
		log.info(position(msg));
	}

	/**
	 * "["'&<]"
	 * 
	 * @param value
	 * @return
	 */
	protected String formatXMLValue(String value, String attrName, char qute) {
		Matcher m = XML_TEXT.matcher(value);
		int hit = -1;
		if (m.find()) {
			StringBuffer sb = new StringBuffer();
			do {
				String entity = m.group();
				if (entity.length() == 1) {
					int c = entity.charAt(0);
					switch (c) {
					case '&':
						if (hit < 0) {
							hit = m.start();
						}
						entity = "&amp;";
						break;
					case '<':
						if (hit < 0) {
							hit = m.start();
						}
						entity = "&lt;";
						break;
					case '\'':
					case '\"':
						if (qute == c) {
							if (hit < 0) {
								hit = m.start();
							}
							entity = "&#" + c + ";";
						}
						break;
					default:

					}
				} else {
					String entity2 = defaultEntryMap.get(entity);
					if (entity2 != null) {
						entity = entity2;
					}
				}
				m.appendReplacement(sb, entity);
			} while (m.find());
			m.appendTail(sb);
			if (hit >= 0) {
				if (attrName == null) {
					String line = new TextPosition(value).getLineText(hit);
					info("XML未转义(已修复):" + line.trim());
				} else {
					String line = new TextPosition(value).getLineText(hit);
					info("属性:" + attrName + " 值未转义(已修复):" + line.trim());
				}
			}
			return sb.toString();
		}
		return value;
	}

	protected String position(String msg) {
		String pos = textPosition.getPosition(start);
		String line = textPosition.getLineText(start);
		return msg + "\n" + uri + "@[" + pos + "]\tline-text:" + line.trim();
	}

	private char getOffset(int offset) {
		int p = start + offset;
		if (p < text.length()) {
			return text.charAt(p);
		}
		return 0;
	}

	// public String addDefaultEntity(String entry, String value) {
	// return defaultEntryMap.put(entry, value);
	// }
	//
	// public String addDefaultNS(String prefix, String namespace) {
	// return defaultNSMap.put(prefix.length() > 0 ? "xmlns:" + prefix
	// : "xmlns", namespace);
	// }

	public void setDefaultRoot(String elementTag) {
		this.documentStart = elementTag.replaceAll("^\\s+|\\/?>(?:\\s*<\\/"
				+ TAG_NAME + ">)?\\s*$", ">");
		this.documentEnd = this.documentStart.replaceFirst("^<(" + TAG_NAME
				+ ")[\\s\\S]*$", "</$1>");
	}

}
