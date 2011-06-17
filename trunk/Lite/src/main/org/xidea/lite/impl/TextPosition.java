package org.xidea.lite.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextPosition {
	protected static final Pattern LINE = Pattern.compile(".*(?:\\r\\n?|\\n)?");
	private String text;
	private int line;
	private int lineStart;//\r\n 算下一行
	public TextPosition(String text){
		this.text=text;
	}
	public synchronized String getPosition(int start){
		walk(start);
		return (line+1)+","+(start-lineStart+1);
	}
	public synchronized String getLineText(int start){
		int end = walk(start);
		if(end>=0){
			return text.substring(lineStart,end);
		}else{
			return text.substring(lineStart);
		}
	}
	private int walk(int start) {
		if(start<lineStart){
			lineStart = 0;
			line = 0;
		}
		Matcher m = LINE.matcher(text);
		if(m.find(lineStart)){
			do{
				int end = m.end();
				if(end>start){
					return end;
				}
				line++;
				lineStart=end;
			}while(m.find());
		}
		return -1;
	}
}
