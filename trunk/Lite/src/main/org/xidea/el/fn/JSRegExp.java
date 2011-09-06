package org.xidea.el.fn;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

public class JSRegExp {
	private static WeakHashMap<String, JSRegExp> CACHED = new WeakHashMap<String, JSRegExp>();
	// private String source;
	Pattern pattern;
	boolean globals;

	public JSRegExp(String regexp) {
		int end = regexp.lastIndexOf('/');
		String source2 = regexp.substring(end + 1);
		// this.source = regexp;
		this.globals = source2.indexOf('g') >= 0;
		int flags = 0;
		if (source2.length() > 0) {
			if (source2.indexOf('i') >= 0) {
				flags |= Pattern.CASE_INSENSITIVE;
			}
			// g
			if (source2.indexOf('m') >= 0) {
				flags |= Pattern.MULTILINE;
			}
		}
		this.pattern = Pattern.compile(regexp.substring(1, end), flags);
	}

	@SuppressWarnings("unchecked")
	static JSRegExp getRegExp(Object arg0) {
		if (arg0 instanceof Map) {
			Map<String, String> map = (Map<String, String>) arg0;
			String literal = map.get("literal");
			if (literal != null && "RegExp".equals(map.get("class"))) {
				JSRegExp exp = CACHED.get(literal);
				if (exp == null) {
					CACHED.put(literal, exp = new JSRegExp(literal));
				}
				return exp;
			}
		}
		return null;
	}


}
