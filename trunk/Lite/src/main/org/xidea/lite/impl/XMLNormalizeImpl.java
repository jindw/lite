package org.xidea.lite.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.parse.XMLNormalize;

/**
 * @author jindawei
 * @see org.xidea.lite.parser.impl.xml.test.XMLNormalizeTest
 */
public class XMLNormalizeImpl implements XMLNormalize {
	private static final Log log = LogFactory.getLog(XMLNormalizeImpl.class);
	protected static final Pattern LINE = Pattern.compile(".*(?:\\r\\n?|\\n)?");
	protected static final String TAG_NAME = "[\\w_](?:[\\w_\\-\\.\\:]*[\\w_\\-\\.])?";
	
	//key (= value)?
	protected static final Pattern ELEMENT_ATTR_END = Pattern.compile("(?:^|\\s+)("+TAG_NAME+")(?:\\s*=\\s*('[^']*'|\"[^\"]*\"|\\w+|\\$\\{[^}]+\\}))?|\\s*\\/?>");
	protected static final Pattern XML_TEXT = Pattern.compile(
			"&\\w+;|&#\\d+;|&#x[\\da-fA-F]+;|([&\"\'<])");
	protected static final Pattern LEAF_TAG = Pattern.compile("^(?:meta|link|img|br|hr|input)$",Pattern.CASE_INSENSITIVE);
	
	protected Map<String, String> defaultNSMap = new HashMap<String, String>();
	protected Map<String, String> defaultEntryMap = new HashMap<String, String>();
	protected String documentStart = "<c:group xmlns:c='http://www.xidea.org/lite/core'>";
	protected String documentEnd = "</c:group>";
	
	protected int start;
	protected int  rootCount;
	protected String text;
	protected ArrayList<Tag> tags;
	protected StringBuilder result;
	protected String uri;
	public XMLNormalizeImpl(){
		defaultEntryMap.put("&nbsp;", "&#160;");
		defaultEntryMap.put("&copy;", "&#169;");
		defaultNSMap.put("xmlns:c", "http://www.xidea.org/lite/core");
	}
	protected class Tag{
		String name;
		Map<String,String> nsMap;
		void add(String name,String value) {
			if(nsMap == null){
				nsMap = new HashMap<String, String>();
			}
			nsMap.put(name, value);
		}
		public void checkTagNS(Tag parent) {
			Map<String, String> parentMap = parent==null?null: parent.nsMap;
			
			if(nsMap != null){
				for (String key:nsMap.keySet().toArray(new String[nsMap.size()])) {
					String prefix = key.substring(0,key.indexOf(':'));
					if(prefix.equals("xml")){
						nsMap.remove(key);
					}else if(prefix.equals("xmlns")){
					}else{
						nsMap.remove(key);
						String dec = "xmlns:"+prefix;
						if(!nsMap.containsKey(dec) && (parentMap==null || !parentMap.containsKey(dec))){
							if(defaultNSMap != null && defaultNSMap.containsKey(dec)){
								result.append(" ");
								result.append(dec);
								result.append("=\"");
								String value = defaultNSMap.get(dec);
								result.append(value);
								result.append("\"");
								nsMap.put(dec, value);
							}else{
								error("unknow namespace prefix:\t"+key+";\tdefaultNSMap:\t"+defaultNSMap+";\tnsMap:\t"+nsMap+";\tparentMap:\t"+parentMap);
							}
						}
					}
				}
			}
			if(parentMap != null){
				if(nsMap != null){
					parentMap = new HashMap<String, String>(parentMap);
					parentMap.putAll(nsMap);
					nsMap = parentMap;
				}else{
					nsMap=parentMap;
				}
			}
		}
	}
	
	public String normalize(String text,String uri){
		final String text0 = this.text;
		final String uri0 = this.uri;
		final int start0 = this.start;
		final StringBuilder result0 = this.result;
		try{
			this.uri = uri;
			this.text = text;
			this.start = 0;
			this.rootCount = 0;
			this.tags = new ArrayList<Tag>();
			this.result = new StringBuilder(text.length()*11/10);
			String result = parse();
			return result;
		}finally{
			this.text=text0;
			this.uri = uri0;
			this.start=start0;
			this.result=result0;
		}
		
	}

	private String parse() {
		while(true){
			int p = text.indexOf('<',start);
			if(p>=start){
				appendTextTo(p);
				appendElement();
			}else{
				appendEnd();
				break;
			}
		}
		if(this.rootCount>1){
			result.append(documentEnd);
			String rtv = result.toString();
			return rtv.replaceFirst("<[\\w_]", documentStart+"$0");
		}
		return result.toString();
	}

	private boolean appendElementStart() {
		final int start = this.start;
		final int len = result.length();
		final Matcher m = ELEMENT_ATTR_END.matcher(text.substring(start+1));
		int p = 0;
		final Tag tag = new Tag();
		if(tags.size() ==0){
			rootCount++;
		}
		while(m.find()){
			if(p!=m.start()){
				break;
			}
			String v = m.group();
			this.start = start + 1 + m.end();
			if(v.endsWith(">") ){
				int size = tags.size();
				tag.checkTagNS(size > 0?tags.get(size-1):null);
				if(v.indexOf('/')>=0 || isLeaf(tag.name)){
					result.append("/>");
				}else{
					tags.add(tag);
					result.append('>');
					//for script
					if("script".equalsIgnoreCase(tag.name)){
						
					}
				}
				return true;
			}else{
				String name = m.group(1);
				String value = m.groupCount()>1?m.group(2):null;
				if(name.indexOf(':')>0){
					tag.add(name,value);
				}
				if(p==0){
					if(value!=null){
						error("attribute value without name:"+v);
					}
					//checkTag(name);
					result.append('<');
					result.append(name);
					tag.name = name;
				}else{
					result.append(v.substring(0,m.start(1)-m.start()));
					result.append(name);
					result.append('=');
					if(value == null){
						result.append('"');
						result.append(name);
						result.append('"');
					}else{
						char f = value.charAt(0);
						if(f == '"' || f == '\''){
							result.append(f);
							result.append(formatXMLValue(value.substring(1,value.length()-1),f));
							result.append(f);
						}else{
							result.append('"');
							result.append(formatXMLValue(value,'"'));
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

	protected void appendElementEnd() {
		String content = sourceTo(">");
		String name = content.substring(2,content.length()-1);
		if(isLeaf(name)){
			return;
		}
		int len = tags.size();
		if(len>0){
			Tag last = tags.remove(len-1);
			String lastName = last.name;
			if(lastName.equals(name)){
				result.append(content);
				return;
			}else if(lastName.equalsIgnoreCase(name)){
				result.append("</");
				result.append(lastName);
				result.append(">");
				return;
			}else{
				error("end tag("+name+") can not match the start("+lastName+")!");
			}
		}else{
			error("Missed Start Element!");
		}
		
	}
	protected boolean isLeaf(String name) {
		return LEAF_TAG.matcher(name).find();
	}

	private void appendElement() {
		char type = getNext(1);
		if(type == '?'){
			appendInstruction();
		}else if(type == '!'){
			int type2 = getNext(2);
			if(type2 == '-'){
				appendComment();
			}else if(type2 == '['){//<![CDATA[  
				appendCDATA();
			}else{//<!DOCTYPE
				appendDTD();
			}
		}else if(type == '/'){
			appendElementEnd();
		}else if(isElementStart(type)){
			if(!appendElementStart()){
				start++;
				result.append("&lt;");
			}
		}else{
			start++;
			result.append("&lt;");
		}
	}
	protected void appendDTD() {
		int start = this.start;
		String content = sourceTo(">");
		int p = content.indexOf("<!",1);
		if(p>0){//nest
			this.start = start;
			content = sourceTo("]>");
		}
		if(content.startsWith("<!doctype")){
			content = "<!DOCTYPE"+content.substring("<!doctype".length());
		}
		result.append(content);
	}
	protected void appendCDATA() {
		String content = sourceTo("]]>");
		result.append(content);
	}
	protected void appendInstruction(){
		String content = sourceTo("?>");
		result.append(content);
	}

	protected void appendComment() {
		String content = sourceTo("-->");
		int p = content.indexOf("--",4);
		if(p!= content.lastIndexOf("--")){//<!--- --> error <!-- --->
			warn("注释中不能出现连续的--");
			content = "<!--"+content .substring(4,content.length()-2).replaceAll("[\\-]", " -")+"->";
		}
		//<!--[if lt IE 9]><![endif]-->
		result.append(content);
	}
//	protected void appendComment(String content){
//		result.append(content);
//	}
	protected void appendTextTo(int p) {
		if(p>start){
			String text = this.text.substring(start,p);
			text = formatXMLValue(text, (char)0);
			result.append(text);
			start = p;
		}
	}

	protected String sourceTo(String endText) {
		int end = text.indexOf(endText,start);
		if(end >0){
			return text.substring(start,start=end+endText.length());
		}else{
			return null;
		}
	}
	protected boolean isElementStart(char type) {
		return Character.isJavaIdentifierPart(type)&& type != '$';
	}
	protected void appendEnd() {
		String end = text.substring(start);
		if(end.trim().length()>0){
			warn("异常文件内容:"+end);
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
	 * "[^'&]"
	 * @param value
	 * @return
	 */
	protected String formatXMLValue(String value,char qute) {
		Matcher m = XML_TEXT.matcher(value);
        if (m.find()) {
            StringBuffer sb = new StringBuffer();
            do {
            	String entity = m.group();
            	if(entity.length()==1){
            		int c = entity.charAt(0);
            		switch(c){
            		case '&':
            			entity = "&amp;";
            			break;
            		case '<':
            			entity = "&lt;";
            			break;
            		case '\'':
            		case '\"':
            			if(qute == c){
                			entity = "&#"+c+";";
            			}
            		}
            	}else{
            		String entity2 = defaultEntryMap.get(entity);
            		if(entity2 != null){
            			entity = entity2;
            		}
            	}
//            	if(entry.equals("&nbsp;")){
//            		entry = "&#160;";
//            	}else if(entry.equals("&copy;")){
//            		entry = "&#169;";
//            	}
                m.appendReplacement(sb, entity);
            } while ( m.find());
            m.appendTail(sb);
            return sb.toString();
        }
        return value;
	}
	protected String position(String msg){
		Matcher m = LINE.matcher(text);
		int line = 0;
		while(m.find()){
			line++;
			int offset = start-m.start();
			if(offset>0 && start<=m.end()){
				return msg + "\n"+uri+"@[line:"+line+";col:"+(offset+1)+"]\nline-text:"+m.group();
			}
		}
		return msg+ "\n"+uri;//unhit
	}

	private char getNext(int offset){
		int p = start+offset;
		if(p<text.length()){
			return text.charAt(p);
		}
		return 0;
	}

	public String addDefaultEntity(String entry, String value) {
		return defaultEntryMap.put(entry, value);
	}

	public String addDefaultNS(String prefix,String namespace) {
		return defaultNSMap.put("xmlns:"+prefix, namespace);
	}
	public void setDefaultRoot(String elementTag){
		this.documentStart = elementTag.replaceAll("^\\s+|\\/?>(?:\\s*<\\/"+TAG_NAME+">)?\\s*$",">");
		this.documentEnd = this.documentStart.replaceFirst("^<("+TAG_NAME+")[\\s\\S]*$", "</$1>");
	}

}
