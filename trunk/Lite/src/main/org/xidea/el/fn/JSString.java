package org.xidea.el.fn;

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
public class JSString extends JSObject implements Invocable {
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
		for (int i = args.length - 1; i >= 0;) {
			buf.append(ECMA262Impl.ToString(args[i]));
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
		if(args !=null &&  args.length>0){
			String replaceValue = String.valueOf(ECMA262Impl.getStringArg(args, 1, "undefined"));
			if (args[0] instanceof Pattern) {
				Pattern searchPattern = (Pattern) args[0];
				//int f = searchPattern.flags();
				Matcher match = searchPattern.matcher(thiz);
				return match.replaceAll(
						replaceValue);
			} else {
				return Pattern.compile(String.valueOf(args[0]), Pattern.LITERAL).matcher(thiz)
						.replaceFirst(Matcher.quoteReplacement(replaceValue));
			}
		}
		return thiz;
	}
	// 15.5.4.13 String.prototype.slice(start, end)
	public String slice(String thiz, Object[] args) {
		int size = thiz.length();
		int begin = JSArray.toSliceRange(ECMA262Impl.getIntArg(args, 0, 0),
				size);
		int end = JSArray.toSliceRange(ECMA262Impl.getIntArg(args, 1, size),
				size);
		if (begin < end) {
			return thiz.substring(begin, end);
		} else {
			return "";
		}
	}

	// 15.5.4.14 String.prototype.split(separator, limit)
	public String[] split(String thiz, Object[] args) {
		String separator = ECMA262Impl.getStringArg(args, 0, null);
		if (separator == null) {
			return new String[] { thiz };
		}
		int limit = ECMA262Impl.getIntArg(args, 0, -1);
		return thiz.split(Pattern.quote(separator), limit);
	}

	static int toSubstringRange(int pos, int size) {
		return Math.min(Math.max(pos, 0), size);
	};

	// 15.5.4.15 String.prototype.substring(start, end)
	public String substring(String thiz, Object[] args) {
		int size = thiz.length();
		int begin = toSubstringRange(ECMA262Impl.getIntArg(args, 0, 0), size);
		int end = toSubstringRange(ECMA262Impl.getIntArg(args, 1, size), size);
		return thiz.substring(Math.min(begin, end), Math.max(begin, end));
	}

	// 非标准
	public String substr(String thiz, Object[] args) {
		int size = thiz.length();
		int begin = toSubstringRange(ECMA262Impl.getIntArg(args, 0, 0), size);
		int len = ECMA262Impl.getIntArg(args, 1, size - begin);
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
