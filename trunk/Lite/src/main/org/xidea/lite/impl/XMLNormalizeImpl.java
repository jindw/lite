package org.xidea.lite.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.XMLNormalize;

public class XMLNormalizeImpl implements XMLNormalize {
	private static final Log log = LogFactory.getLog(XMLNormalizeImpl.class);
	private static final Pattern LINE = Pattern.compile(".*(?:\\r\\n?|\\n)?");
	
	//key (= value)?
	private static final Pattern ELEMENT_ATTR_END = Pattern.compile("(?:^|\\s+)([\\w_](?:[\\w_\\-\\.\\:]*[\\w_\\-\\.])?)(?:\\s*=\\s*('[^']*'|\"[^\"]*\"|\\w+|\\$\\{[^}]+\\}))?|\\/?>");
	private static final Pattern XML_TEXT = Pattern.compile(
			"&\\w+;|&#\\d+;|&#x[\\da-fA-F]+;|([&\"\'<])");
	private static final Pattern LEAF_TAG = Pattern.compile("^(?:meta|link|img|br|hr|input)$",Pattern.CASE_INSENSITIVE);
	
	private Map<String, String> defaultNSMap = new HashMap<String, String>();
	private Map<String, String> defaultEntryMap = new HashMap<String, String>();
	private String documentStart = "<c:group xmlns:c='http://www.xidea.org/lite/core'>";
	private String documentEnd = "</c:group>";
	
	private int start;
	private int  rootCount;
	private String text;
	private ArrayList<Tag> tags;
	private StringBuilder result;
	private String uri;
	public XMLNormalizeImpl(){
		defaultEntryMap.put("&nbsp;", "&#160;");
		defaultEntryMap.put("&copy;", "&#169;");
		defaultNSMap.put("xmlns:c", "http://www.xidea.org/lite/core");
	}
	class Tag{
		String name;
		Map<String,String> nsMap;
		void add(String name,String value) {
			if(nsMap == null){
				nsMap = new HashMap<String, String>();
			}
			nsMap.put(name, value);
		}
		public void check(Tag parent) {
			if(parent!=null && parent.nsMap != null){
				if(nsMap != null){
					HashMap<String, String> map = new HashMap<String, String>(parent.nsMap);
					map.putAll(nsMap);
					nsMap = map;
				}else{//nsMap!=null
					nsMap=parent.nsMap;
				}
			}
			if(nsMap != null){
				for (Map.Entry<String, String> entry:nsMap.entrySet()) {
					final String key = entry.getKey();
					String prefix = key.substring(0,key.indexOf(':'));
					if(!prefix.equals("xmlns") && !prefix.equals("xml")){
						String dec = "xmlns:"+prefix;
						if(!nsMap.containsKey(dec)){
							if(defaultNSMap != null && defaultNSMap.containsKey(dec)){
								result.append(" ");
								result.append(dec);
								result.append("=\"");
								result.append(defaultNSMap.get(dec));
								result.append("\"");
							}else{
								error("unknow namespace prefix:\t"+key+";\tdefaultNSMap:\t"+defaultNSMap);
							}
						}
					}
				}
			}
		}
	}
	
	public String normalize(String text,String uri){
		this.uri = uri;
		this.text = text;
		this.start = 0;
		this.rootCount = 0;
		this.tags = new ArrayList<Tag>();
		result = new StringBuilder(text.length()*11/10);
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
			int vlen = v.length();
			if(vlen <=2 && v.endsWith(">") ){
				int size = tags.size();
				tag.check(size > 0?tags.get(size-1):null);
				if(v.charAt(0) == '/' || isLeaf(tag.name)){
					result.append("/>");
				}else{
					tags.add(tag);
					result.append('>');
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

	private void appendElementEnd() {
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
	private boolean isLeaf(String name) {
		return LEAF_TAG.matcher(name).find();
	}

	private void appendElement() {
		// TODO Auto-generated method stub
		//text.charAt(start)=='?'
		char type = getNext(1);
		if(type == '?'){
			appendInstruction();
		}else if(type == '!'){
			int type2 = getNext(2);
			if(type2 == '-'){
				appendCommon();
			}else if(type2 == '['){//<![CDATA[  
				appendCdata();
			}else{//<!DOCTYPE
				appendDTD();
			}
		}else if(type == '/'){
			appendElementEnd();
		}else if(isElementStart(type)){
			appendElementStart();
		}else{
			
		}
	}
	private void appendDTD() {
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
	private void appendCdata() {
		String content = sourceTo("]]>");
		result.append(content);
	}
	private void appendInstruction(){
		String content = sourceTo("?>");
		result.append(content);
	}

	protected void appendCommon() {
		String content = sourceTo("-->");
		int p = content.indexOf("--",4);
		if(p!= content.lastIndexOf("--")){//<!--- --> error <!-- --->
			warn("注释中不能出现连续的--");
			content = "<!--"+content .substring(4,content.length()-2).replaceAll("[\\-]", " -")+"->";
		}
		//<!--[if lt IE 9]><![endif]-->
		result.append(content);
	}
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
	@SuppressWarnings("unused")
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
            	String entry = m.group();
            	if(entry.length()==1){
            		int c = entry.charAt(0);
            		switch(c){
            		case '&':
            			entry = "&amp;";
            		case '<':
            			entry = "&lt;";
            		case '\'':
            		case '\"':
            			if(qute == c){
                			entry = "&#"+c+";";
            			}
            		}
            	}else{
            		String entry2 = defaultEntryMap.get(entry);
            		if(entry2 != null){
            			entry = entry2;
            		}
            	}
//            	if(entry.equals("&nbsp;")){
//            		entry = "&#160;";
//            	}else if(entry.equals("&copy;")){
//            		entry = "&#169;";
//            	}
                m.appendReplacement(sb, entry);
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
				return msg + "\n"+uri+"@[line:"+line+";col:"+(offset+1)+"]";
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

	public String addDefaultEntry(String entry, String value) {
		return defaultEntryMap.put(entry, value);
	}

	public String addDefaultNS(String namespace, String prefix) {
		return defaultNSMap.put("xmlns:"+prefix, namespace);
	}
	public void setDefaultRoot(String start,String end){
		this.documentStart = start;
		this.documentEnd = end;
	}

}
