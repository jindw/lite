/**
 * 
 */
package org.jside.webserver.action;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dawei.Jin
 * 
 */
public class URLMatcher implements Comparable<URLMatcher> {
	private final Pattern pattern;
	private int length;
	public static final URLMatcher createMatcher(String pattern) {
		return new URLMatcher(pattern);
	}

	private URLMatcher(String pattern) {
		length = pattern.length();
		Matcher matcher = Pattern.compile("/?\\*\\*+|\\*+|[^\\*\\\\/]+?|[\\\\/]").matcher(
				pattern);
		StringBuilder buf = new StringBuilder("^");
		while (matcher.find()) {
			String item = matcher.group();
			int length = item.length();
			char firstChar = item.charAt(0);
			if (firstChar == '*') {
				if (length > 1) {
					buf.append(".*");
				} else {
					buf.append("[^\\\\/]+");
				}
			} else if (length > 1 && item.charAt(1) == '*') {// *
				if (length > 2) {
					buf.append("/?.*");
				} else {
					buf.append("/?[^\\\\/]+");
				}
			} else if(length == 1 && firstChar == '/' || firstChar == '\\') {
				buf.append("[\\\\/]");
			}else{
				buf.append(Pattern.quote(item));
			}
		}
		buf.append("$");
		this.pattern = Pattern.compile(buf.toString());

	}

	public final boolean match(String url) {
		return pattern.matcher(url).find();
	}

	public int compareTo(URLMatcher o) {
		return o.length - this.length;
	}

}
