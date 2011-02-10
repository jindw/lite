package org.xidea.el.fn;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSRegExp {
	private static WeakHashMap<String, JSRegExp> CACHED = new WeakHashMap<String, JSRegExp>();
//	private String source;
	private Pattern pattern;
	private boolean globals;

	public JSRegExp(String regexp){
		int end = regexp.lastIndexOf('/');
		String source2 = regexp.substring(end+1);
//		this.source = regexp;
		this.globals = source2.indexOf('m')>=0;
		int flags = 0;
		if(source2.length()>0){
			if(source2.indexOf('i')>=0){
				flags |= Pattern.CASE_INSENSITIVE;
			}
			//g
			if(source2.indexOf('m')>=0){
				flags |= Pattern.MULTILINE;
			}
		}
		this.pattern = Pattern.compile(regexp.substring(1,end),flags);
	}
	public static String replace(String thiz,Object regexp, String replaceValue){
		JSRegExp exp = getRegExp(regexp);
		if(exp == null){
			return Pattern.compile(String.valueOf(regexp), Pattern.LITERAL).matcher(thiz)
					.replaceFirst(Matcher.quoteReplacement(replaceValue));
		}
		Matcher match = exp.pattern.matcher(thiz);
		if(exp.globals){
			return match.replaceAll(replaceValue);
		}else{
			return match.replaceFirst(replaceValue);
		}
	}
	public static boolean match(String thiz,Object arg0){
		JSRegExp exp = getRegExp(arg0);
		if(exp==null){
			return false;
		}
		Matcher match = exp.pattern.matcher(thiz);
		return match.find();
	}
	@SuppressWarnings("unchecked")
	private static JSRegExp getRegExp(Object arg0) {
		if(arg0 instanceof Map){
			Map<String, String> map = (Map<String, String>)arg0;
			if("RegExp".equals(map.get("class"))){
				String source = map.get("source");
				JSRegExp exp = CACHED.get(source);
				if(exp == null){
					CACHED.put(source, exp = new JSRegExp(source));
				}
				return exp;
			}
		}
		return null;
	}

}
