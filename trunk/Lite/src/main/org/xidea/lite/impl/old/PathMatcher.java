/**
 * 
 */
package org.xidea.lite.impl.old;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Dawei.Jin
 * 
 */
public class PathMatcher implements Comparable<PathMatcher> {
	private static final Pattern MATCH_PARTY = Pattern.compile(
			"\\*+|" +
			"[^\\*\\\\/]+?|" +
			"[\\\\/]+");
	private final Pattern pattern;
	private int length;

	public PathMatcher(String pattern) {
		length = pattern.length();
		Matcher matcher = MATCH_PARTY.matcher(
				pattern);
		StringBuilder buf = new StringBuilder("^");
		while (matcher.find()) {
			String item = matcher.group();
			int length = item.length();
			char firstChar = item.charAt(0);
			if (firstChar == '*') {
				if (length > 1) {
					buf.append(".*");//* 允许 0至多个非分割字符（\/）
				} else {
					buf.append("[^\\\\/]*");//* 允许 0至多个任意字符
				}
			} else if(firstChar == '/' || firstChar == '\\') {
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

	public int compareTo(PathMatcher o) {
		return o.length - this.length;
	}

}
