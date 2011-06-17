var TAG_NAME = new RegExp("[\\w_](?:[\\w_\\-\\.\\:]*[\\w_\\-\\.])?");

	// key (= value)?
var ELEMENT_ATTR_END = new RegExp("(?:^|\\s+)("
					+ TAG_NAME.source
					+ ")(?:\\s*=\\s*('[^']*'|\"[^\"]*\"|\\w+|\\$\\{[^}]+\\}))?|\\s*\\/?>");
var XML_TEXT = new RegExp("&\\w+;|&#\\d+;|&#x[\\da-fA-F]+;|([&\"\'<])");

var LEAF_TAG = new RegExp(
			"^(?:meta|link|img|br|hr|input)$", 'i');

var defaultNSMap = {
	"xmlns:f": "http://firekylin.my.baidu.com/ns/2010",
	"xmlns:c": "http://www.xidea.org/lite/core",
	"": "http://www.w3.org/1999/xhtml"
};
var defaultEntryMap = {"&nbsp;": "&#160;","&copy;": "&#169;"};
var documentStart = "<c:group xmlns:c='http://www.xidea.org/lite/core'>";
var documentEnd = "</c:group>";

function normalizeXML(text,uri){
	var start = 0;
	var rootCount = 0;
	var textPosition = [];
	var tag = [];
	var result = [];

	function parse() {
		while (true) {
			var p = text.indexOf('<', start);
			if (p >= start) {
				start = appendTextTo(p);
				start = appendElement(p);
			} else {
				appendEnd(p);
				break;
			}
		}
		if (rootCount > 1) {
			result.push(documentEnd);
			var rtv = result.join('');
			return rtv.replace(/<[\w_]/, documentStart + "$0");
		}
		return result.join('');
	}

	function appendPos(start) {
		result.append(" c:__pos__=\'" + textPosition.getPosition(start) + "\'");
	}

	function appendElementStart() {
		var start = this.start;
		var len = result.length();
		var m = ELEMENT_ATTR_END.test(text.substring(start + 1));
		var p = 0;
		if (tag == null) {
			rootCount++;
		}
		this.tag = new Tag();
		while (m.find()) {
			if (p != m.start()) {
				break;
			}
			var v = m.group();
			this.start = start + 1 + m.end();
			if (v.endsWith(">")) {
				tag.checkTagNS();
				var closeTag = v.indexOf('/') >= 0;
				if (closeTag || isLeaf(tag.name)) {
					appendPos(start);
					if (!closeTag) {
						info("标签:" + tag.name + " 未关闭(已修复)");
					}
					result.append("/>");
					tag.remove();
				} else {
					appendPos(start);
					result.append('>');
					// for script
					if ("script".equalsIgnoreCase(tag.name)) {

					}
				}
				return true;
			} else {
				var name = m.group(1);
				var  value = m.groupCount() > 1 ? m.group(2) : null;

				tag.acceptNS(name, value);
				if (p == 0) {
					if (value != null) {
						error("attribute value without name:" + v);
					}
					// checkTag(name);
					result.append('<');
					result.append(name);
					tag.name = name;
				} else {
					result.append(v.substring(0, m.start(1) - m.start()));
					result.append(name);
					result.append('=');
					if (value == null) {
						info("属性:" + name + " 未赋值(已修复)");
						result.append('"');
						result.append(name);
						result.append('"');
					} else {
						var f = value.charAt(0);
						if (f == '"' || f == '\'') {
							result.append(f);
							var v1 = value.substring(1, value.length() - 1);
							var v2 = formatXMLValue(v1, name, f);
							result.append(v2);
							result.append(f);
						} else {
							info("属性:" + name + " 未使用\"'(已修复)");
							result.append('"');
							var v2 = formatXMLValue(value, name, '"');
							result.append(v2);
							result.append('"');
						}
					}

				}

			}
			p = m.end();
		}

		this.start = start;
		result.setLength(len);
		return false;
	}

	function appendElementEnd() {
		var content = sourceTo(">");
		var name = content.substring(2, content.length() - 1);
		if (isLeaf(name)) {
			return;
		}
		if (tag != null) {
			var lastName = tag.name;
			result.append("</");
			result.append(lastName);
			result.append(">");
			tag.remove();
			if (!lastName.equalsIgnoreCase(name)) {
				error("end tag(" + name + ") can not match the start("
						+ lastName + ")!");
			}
		} else {
			error("Missed Start Element!");
		}

	}

	function isLeaf(name) {
		return LEAF_TAG.matcher(name).find();
	}

	function appendElement() {
		var type = getOffset(1);
		if (type == '?') {
			appendInstruction();
		} else if (type == '!') {
			var type2 = getOffset(2);
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

	function appendDTD() {
		var start = this.start;
		var content = sourceTo(">");
		var p = content.indexOf("<!", 1);
		if (p > 0) {// nest
			this.start = start;
			content = sourceTo("]>");
		}
		if (content.startsWith("<!doctype")) {
			content = "<!DOCTYPE" + content.substring("<!doctype".length());
		}
		result.append(content);
	}

	function appendCDATA() {
		var content = sourceTo("]]>");
		result.append(content);
	}

	var CC = Pattern
			.compile("^(<!--\\[if.*?\\]>)([\\s\\S]*?)(<!\\[endif\\]-->)$");

	function appendComment() {
		// <!--[if lt IE 9]><![endif]-->
		// <!--[if expression]> HTML <![endif]-->
		// http://msdn.microsoft.com/en-us/library/ms537512%28v=vs.85%29.aspx
		var content = sourceTo("-->");
		var p = content.indexOf("--", 4);
		if (p != content.lastIndexOf("--")) {// <!--- --> error <!-- --->
			warn("注释中不能出现连续的--");
			content = "<!--"
					+ content.substring(4, content.length() - 2).replaceAll(
							"[\\-]", " -") + "->";
		}
		// <!--[if lt IE 9]><![endif]-->
		var m = CC.matcher(content);
		if (m.find()) {
			var c2 = m.group(2);
			appendIECComment(c2);
		} else {
			result.append(content);
		}
	}

	function appendIECComment(content) {
		content = this.formatXMLValue(
				"$!{" + JSONEncoder.encode(content) + "}", null, '\0');
		result.append(content);
	}

	function appendInstruction() {
		var content = sourceTo("?>");
		result.append(content);
	}

	function appendTextTo(p) {
		if (p > start) {
			var text = this.text.substring(start, p);
			var text2 = formatXMLValue(text, null, 0);

			result.append(text2);
			start = p;
		}
	}

	function sourceTo(endText) {
		var end = text.indexOf(endText, start);
		if (end > 0) {
			return text.substring(start, start = end + endText.length());
		} else {
			return null;
		}
	}

	function isElementStart(type) {
		return Character.isJavaIdentifierPart(type) && type != '$';
	}

	function appendEnd() {
		var end = text.substring(start);
		if (end.trim().length() > 0) {
			warn("异常文件内容:" + end);
		}
	}

	function error(msg) {
		log.error(position(msg));
	}

	function warn(msg) {
		log.warn(position(msg));
	}

	function info(msg) {
		log.info(position(msg));
	}

	/**
	 * "["'&<]"
	 * 
	 * @param value
	 * @return
	 */
	function formatXMLValue(value, attrName, qute) {
		var m = XML_TEXT.matcher(value);
		var hit = -1;
		if (m.find()) {
			var sb = new StringBuffer();
			do {
				var entity = m.group();
				if (entity.length() == 1) {
					var c = entity.charAt(0);
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
					var entity2 = defaultEntryMap.get(entity);
					if (entity2 != null) {
						entity = entity2;
					}
				}
				m.appendReplacement(sb, entity);
			} while (m.find());
			m.appendTail(sb);
			if (hit >= 0) {
				if (attrName == null) {
					var line = new TextPosition(value).getLineText(hit);
					info("XML未转义(已修复):" + line.trim());
				} else {
					var line = new TextPosition(value).getLineText(hit);
					info("属性:" + attrName + " 值未转义(已修复):" + line.trim());
				}
			}
			return sb.toString();
		}
		return value;
	}

	function position(msg) {
		var pos = textPosition.getPosition(start);
		var line = textPosition.getLineText(start);
		return msg + "\n" + uri + "@[" + pos + "]\tline-text:" + line.trim();
	}

	function getOffset(offset) {
		var p = start + offset;
		if (p < text.length()) {
			return text.charAt(p);
		}
		return 0;
	}
























	function Tag (){
		var name;
		var nsMap;
		var parentTag;

		function Tag() {
			this.parentTag = tag;
		}

		function remove() {
			tag = parentTag;
		}

		function acceptNS(name,  value) {
			//
			// int size = tags.size();
			// size > 0?tags.get(size-1):null

			if ("xmlns".equalsIgnoreCase(name) || name.indexOf(':') > 0) {
				if (nsMap == null) {
					nsMap = {};
				}
				nsMap.put(name, value);
			}
		}

		function checkTagNS() {
			if (parentTag == null) {// first Node
				if (nsMap == null) {
					nsMap = {};
				}
				for (var entry in defaultNSMap.entrySet()) {
					var key = entry.getKey();
					var value = entry.getValue();
					if (!nsMap.containsKey(key)) {
						nsMap.put(key, value);
						result.append(" ");
						result.append(key);
						result.append("='");
						result.append(value);
						result.append("'");
					}
				}
				// check missed
				nsMap = toNSDecMap(nsMap,defaultNSMap);
			} else {
				if (nsMap == null) {
					nsMap = parentTag.nsMap;
				} else {
					// check missed and init this
					nsMap = toNSDecMap(nsMap,parentTag.nsMap);
				}

			}
		}

		function toNSDecMap(attributeMap,parentNSMap) {
			var nnsMap = null;
			for (var key in attributeMap.keySet()) {
				var p = key.indexOf(':');
				var prefix = p > 0 ? key.substring(0, p) : key;
				if (prefix.equals("xmlns")) {// ns define
					if (!parentNSMap.containsKey(key)) {
						if (nnsMap == null) {
							nnsMap = {}//new HashMap<String, String>(parentNSMap);
						}
						nnsMap.put(key, attributeMap.get(key));
					}
				} else if (!prefix.equals("xml")) {
					var dec = "xmlns:" + prefix;
					if (!attributeMap.containsKey(dec) && !parentNSMap.containsKey(dec)) {
						error("unknow namespace prefix:\t" + key
								+ ";\tdefaultNSMap:\t" + defaultNSMap
								+ ";\tnsMap:\t" + attributeMap + ";\tparentMap:\t"
								+ parentNSMap);
					}
				}
			}
			if (nnsMap != null) {
				return nnsMap;
			}else{
				return parentNSMap;
			}
		}
	}

}