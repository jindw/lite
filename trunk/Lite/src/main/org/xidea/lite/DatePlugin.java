package org.xidea.lite;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;
import org.xidea.el.ValueStack;

/**
 * 自定义函数和扩展函数（Invocable接口类）
 * 
 * @author jindw
 */
public class DatePlugin implements RuntimePlugin {
	private static Log log = LogFactory.getLog(DatePlugin.class);
	private static Pattern datePattern = Pattern
			.compile("([YMDhms])\\1*|(\\.s|TZD)|[\\s\\S]+?");
	private static java.util.WeakHashMap<String, String> CACHED_PATTERN = new WeakHashMap<String, String>();
	private Expression pattern;
	private Expression date;

	public void initialize(Template template, Object[] children) {
		this.pattern = (Expression) ((Object[]) children[0])[1];
		this.date = (Expression) ((Object[]) children[1])[1];
	}

	private static String replace(final String pattern) {
		String rtv = CACHED_PATTERN.get(pattern);
		if (rtv == null) {
			Matcher matcher = datePattern.matcher(pattern);
			StringBuffer buf = new StringBuffer();
			while (matcher.find()) {
				String a = matcher.group();
				String p = matcher.group(1);
//				String st = matcher.group(2);
//				System.out.println('!'+p+'/'+st+'/'+a);
				if (".s".equals(a)) {
					a = ".SSS";
				} else if ("TZD".equals(a)) {
					a = "Z";
				} else if (p != null) {
					int len = a.length();
					char first = p.charAt(0);
					if(first == 'Y'){
						if(len ==2){
							a = "yy";
						}else{
							if(len != 4 && len != 1){
								log.error("unknow pattern:"+a);
							}
							a = "yyyy";
						}
					}else{
						if(len >2){
							log.error("unknow pattern:"+a+";normalized to:"+p);
							a = p;
						}
					}
					switch (first) {
					case 'Y':
					case 'D':
						a = a.toLowerCase();
						break;
					case 'h':
						a = a.toUpperCase();
						break;
					//case 'M':
					//case 'm':
					//case 's':
					}
				} else {
					int len = buf.length();
					if(a.charAt(0) == '\''){
						a = '\''+a;
					}
					if(len > 0 && buf.charAt(len - 1) == '\''){
						buf.delete(len-1, len);
						a = a+'\'';
					}else{
						a = '\''+a + '\'';
					}
				}
					
				matcher.appendReplacement(buf, a);
			}
			rtv = buf.toString();
			CACHED_PATTERN.put(pattern, rtv);
		}
		return rtv;

	}

	private String format(String pattern, Date date) {
		pattern = replace(pattern);
		String value = new SimpleDateFormat(pattern).format(date);
		if(pattern.endsWith("Z")){
			int p = value.length()-2;
			value = value.substring(0,p)+':'+value.substring(p);
		}
		return value;
	}

	public void execute(ValueStack context, Writer out) throws IOException {
		Object pattern = this.pattern.evaluate(context);
		Object date = this.date.evaluate(context);
//		System.out.println(pattern);
//		System.out.println(date);
		if (date == null) {
			date = new Date();
		} else if (!(date instanceof Date)) {
			if(date instanceof Number){
				date = new Date(((Number) date).longValue());
			}else{
				date = new Date();
			}
		}
		out.write(format((String)pattern, (Date)date));
	}
//	public static void main(String[] args){
//		System.out.println(new DatePlugin().format("Y-M-D h:m:s", new Date(1000*9)));
//	}
}
