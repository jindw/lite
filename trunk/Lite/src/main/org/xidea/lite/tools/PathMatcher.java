/**
 * 
 */
package org.xidea.lite.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dawei.Jin
 * 
 */
public class PathMatcher {
	private static final Pattern MATCH_PARTY = Pattern.compile("\\*+|"
			+ "[^\\*/]+?|" + "[/]");
	private final Pattern pattern;
	private final Pattern start;
	private final Pattern must;

	public static final PathMatcher createMatcher(String... pattern) {
		if(pattern == null){
			return null;
		}
		PathMatcher[] rtv = new PathMatcher[pattern.length];
		for (int i = 0; i < rtv.length; i++) {
			rtv[i] = new PathMatcher(pattern[i].replace('\\', '/'));
		}
		switch(pattern.length){
		case 0:
			return null;
		case 1:
			return rtv[0];
		}
		return new PathMatcher(rtv);
	}

	private PathMatcher(PathMatcher[] matchs) {
		StringBuilder pattern = new StringBuilder();
		StringBuilder start = new StringBuilder();
		StringBuilder must = new StringBuilder();
		boolean startNotNull = true;
		for (PathMatcher match : matchs) {
			appendPattern(pattern, match.pattern);
			if(startNotNull){
				if(match.start == null){
					startNotNull = false;
				}else{
					appendPattern(start, match.start);
				}
			}
			appendPattern(must, match.must);
		}
		this.pattern = Pattern.compile(pattern.toString());
		this.start = startNotNull && start.length()>0?Pattern.compile(start.toString()):null;
		this.must = must.length()>0?Pattern.compile(must.toString()):null;

	}

	private void appendPattern(StringBuilder buf, Pattern match) {
		if (match != null) {
			if (buf.length() > 0) {
				buf.append('|');
			}
			buf.append(match.pattern());
		}
	}

	private PathMatcher(String pattern) {
		Matcher matcher = MATCH_PARTY.matcher(pattern);
		StringBuilder buf = new StringBuilder("^");
		int patternCount = 0;
		String prefix = null;
		while (matcher.find()) {
			String item = matcher.group();
			int length = item.length();
			char firstChar = item.charAt(0);
			if (firstChar == '*') {
				patternCount++;
				if (buf.length() > 1) {
					prefix = buf.toString();
				}
				if (length > 1) {
					buf.append(".*");// * 允许 0至多个非分割字符（\/）
				} else {
					buf.append("[^/]*");// * 允许 0至多个任意字符
				}
			} else if (length == 1 && firstChar == '/') {
				buf.append("[/]");
			} else {
				buf.append(Pattern.quote(item));
			}
		}
		buf.append("$");
		String ps = buf.toString();
		this.pattern = Pattern.compile(ps);
		this.must = patternCount == 1 && ps.endsWith(".*$")?this.pattern:null;
		this.start = prefix == null ? null : Pattern.compile(prefix);

	}

	public final boolean match(String url) {
		return pattern.matcher(url).find();
	}

	boolean maybe(String path) {
		return start == null || start.matcher(path).find();
	}

	boolean must(String path) {
		return must!=null && must.matcher(path).find();
	}

}
