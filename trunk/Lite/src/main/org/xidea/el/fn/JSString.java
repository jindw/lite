package org.xidea.el.fn;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xidea.el.Invocable;

/**
 * 15.5.3.2 String.fromCharCode([ char0[, char1 [,...]]]) 15.5.4.4
 * String.prototype.charAt(pos) 15.5.4.5 String.prototype.charCodeAt(pos)
 * 15.5.4.6 String.prototype.concat([ string1[, string2 [,...]]]) 15.5.4.7
 * String.prototype.indexOf(searchString, position) 15.5.4.8
 * String.prototype.lastIndexOf(searchString, position) 15.5.4.11
 * String.prototype.replace(searchValue, replaceValue) 15.5.4.13
 * String.prototype.slice(start, end) 15.5.4.14
 * String.prototype.split(separator, limit) 15.5.4.15
 * String.prototype.substring(start, end) 15.5.4.16
 * String.prototype.toLowerCase() 15.5.4.17 String.prototype.toLocaleLowerCase()
 * 15.5.4.18 String.prototype.toUpperCase() 15.5.4.19
 * String.prototype.toLocaleUpperCase() 15.5.4.1 String.fromCharCode([ char0[,
 * char1 [,...]]])
 * 
 * 15.5.4.2 String.prototype.toString() 15.5.4.3 String.prototype.valueOf()
 * 15.5.4.9 String.prototype.localeCompare(that) 15.5.4.10
 * String.prototype.match(regexp) 15.5.4.12 String.prototype.search(regexp)
 */
class JSString extends JSObject implements Invocable {
	// 15.5.4.1 String.fromCharCode([ char0[, char1 [,...]]])
	public String fromCharCode(String thiz, Object[] args) {
		char[] codes = new char[args.length];
		for (int i = codes.length - 1; i >= 0;) {
			codes[i] = (char) ECMA262Impl.ToNumber(args[i]).intValue();
		}
		return new String(codes);
	}

	// 15.5.4.4 String.prototype.charAt(pos)
	public char charAt(String thiz, int p) {
		return thiz.charAt(p);
	}

	// 15.5.4.5 String.prototype.charCodeAt(pos)
	public int charCodeAt(String thiz, int p) {
		return thiz.charAt(p);
	}

	// 15.5.4.6 String.prototype.concat([ string1[, string2 [,...]]])
	public String concat(String thiz, Object[] args) {
		StringBuilder buf = new StringBuilder(thiz);
		for (Object arg:args) {
			buf.append(ECMA262Impl.ToString(arg));
		}
		return buf.toString();
	}

	// 15.5.4.7 String.prototype.indexOf(searchString, position)
	public int indexOf(String thiz, String sub, int pos) {
		return thiz.indexOf(sub, pos);
	}

	// 15.5.4.8 String.prototype.lastIndexOf(searchString, position)
	public int lastIndexOf(String thiz, Object[] args) {
		if (args.length > 1) {
			return thiz.lastIndexOf(ECMA262Impl.ToString(args[0]), ECMA262Impl
					.ToNumber(args[0]).intValue());
		}
		return thiz.lastIndexOf(ECMA262Impl.ToString(args[0]));
	}

	// 15.5.4.11 String.prototype.replace(searchValue, replaceValue)
	public String replace(String thiz, Object[] args) {
		if(args.length>0){
			Object regexp = args[0];
			String replaceValue = JSObject.getStringArg(args, 1, "undefined");
			JSRegExp exp = JSRegExp.getRegExp(regexp);
			if (exp == null) {
				String s = String.valueOf(regexp);
				int p = thiz.indexOf(s);
				if (p >= 0) {
					return thiz.substring(0, p) + replaceValue
							+ thiz.substring(p + s.length());
				}
				return thiz;
			}
			Matcher match = exp.pattern.matcher(thiz);
			replaceValue = replaceValue.replaceAll("[\\\\]", "\\\\\\\\").replace("[\\$]{2}","\\\\$");
//			System.out.println(replaceValue);
			if (exp.globals) {
				return match.replaceAll(replaceValue);
			} else {
				return match.replaceFirst(replaceValue);
			}
			
		}
		return thiz;
	}
	public Object match(String thiz, Object[] args) {
		JSRegExp exp = JSRegExp.getRegExp(getArg(args, 0, null));
		if (exp == null) {
			return null;
		}
		Matcher match = exp.pattern.matcher(thiz);
		if (match.find()) {
			ArrayList<String> result = new ArrayList<String>();
			if (exp.globals) {
				do{
					result.add(match.group());
				}while(match.find());
			} else {
				int c = match.groupCount();
				for(int i=0;i<=c;i++){
					result.add(match.group(i));
				}
			}
			return result;
		}
		return null;
	}
	// 15.5.4.13 String.prototype.slice(start, end)
	public String slice(String thiz, Object[] args) {
		int size = thiz.length();
		int begin = JSArray.toSliceRange(JSObject.getIntArg(args, 0, 0),
				size);
		int end = JSArray.toSliceRange(JSObject.getIntArg(args, 1, size),
				size);
		if (begin < end) {
			return thiz.substring(begin, end);
		} else {
			return "";
		}
	}

	// 15.5.4.14 String.prototype.split(separator, limit)
	public Object[] split(String thiz, Object[] args) {
		Object separator = JSObject.getArg(args, 0, null);
		if (separator == null) {
			return new String[] { thiz };
		}
		final int limit = JSObject.getIntArg(args, 1, -1);
		JSRegExp exp = JSRegExp.getRegExp(separator);
		Pattern pattern;
		if(exp!=null){
			pattern = exp.pattern;
			///System.out.println(exp.pattern);
			//System.out.println(Arrays.asList(exp.pattern.split(thiz, limit)));
			return exp.pattern.split(thiz, limit);
		}else{
			pattern = Pattern.compile(Pattern.quote(String.valueOf(separator)));
		}
		ArrayList<String> rtv = new ArrayList<String>();
	    Matcher m = pattern.matcher(thiz);
		int index = 0;
	    int ms = 0;
	    int me = 0;
	    while(m.find()) {
	    	ms = m.start();
	    	me = m.end();
			if (limit <0 || rtv.size() < limit) {
				if(!(ms == me && index == ms)){
					String match = thiz.substring(index,ms);
					rtv.add(match);
					index = me;
				}
			}
		}
	    if (limit <0 || rtv.size() < limit ) {
			if(!(ms == me && thiz.length() == ms)){
	    		rtv.add(thiz.substring(index));
	    	}
	    }
		return rtv.toArray();
	}

	static int toSubstringRange(int pos, int size) {
		return Math.min(Math.max(pos, 0), size);
	};

	// 15.5.4.15 String.prototype.substring(start, end)
	public String substring(String thiz, Object[] args) {
		int size = thiz.length();
		int begin = toSubstringRange(JSObject.getIntArg(args, 0, 0), size);
		int end = toSubstringRange(JSObject.getIntArg(args, 1, size), size);
		return thiz.substring(Math.min(begin, end), Math.max(begin, end));
	}

	// 非标准
	public String substr(String thiz, Object[] args) {
		int size = thiz.length();
		int begin = toSubstringRange(JSObject.getIntArg(args, 0, 0), size);
		int len = JSObject.getIntArg(args, 1, size - begin);
		if (len > 0) {
			return thiz.substring(begin, Math.min(size, begin + len));
		}
		return "";
	}

	// 15.5.4.16 String.prototype.toLowerCase()

	public String toLowerCase(String thiz, Object[] args) {
		return thiz.toLowerCase();
	}

	// 15.5.4.17 String.prototype.toLocaleLowerCase()
	public String toLocaleLowerCase(String thiz, Object[] args) {
		return thiz.toLowerCase(Locale.getDefault());
	}

	// 15.5.4.18 String.prototype.toUpperCase()
	public String toUpperCase(String thiz, Object[] args) {
		return thiz.toUpperCase(Locale.getDefault());
	}

	// 15.5.4.19 String.prototype.toLocaleUpperCase()
	public String toLocaleUpperCase(String thiz, Object[] args) {
		return thiz.toUpperCase(Locale.getDefault());
	}

}
