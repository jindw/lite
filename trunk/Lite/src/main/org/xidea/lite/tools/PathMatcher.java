/**
 * 
 */
package org.xidea.lite.tools;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dawei.Jin
 * 
 */
public abstract class PathMatcher {
	public static final PathMatcher createMatcher(String... pattern) {
		if (pattern == null) {
			return null;
		}
		PathMatcher[] rtv = new PathMatcher[pattern.length];
		for (int i = 0; i < rtv.length; i++) {
			rtv[i] = new SinglePathMatcher(pattern[i].replace('\\', '/'));
		}
		switch (pattern.length) {
		case 0:
			return null;
		case 1:
			return rtv[0];
		}
		return new MultiPathMatcher(rtv);
	}

	abstract boolean must(String path);

	abstract boolean maybe(String path);

	abstract boolean match(String path);
}

class SinglePathMatcher extends PathMatcher {

	private static final Pattern MATCH_PARTY = Pattern.compile("\\*+|"
			+ "[^\\*/]+?|" + "[/]");
	private final Pattern pattern;
	private final Pattern[] parties;
	private final Pattern must;
	/**
	 * /path/.* /*.jpg =>/path/(?: .* /(?:.*)? )?
	 */
	protected SinglePathMatcher(String pattern) {
		Matcher matcher = MATCH_PARTY.matcher(pattern);
		StringBuilder buf = new StringBuilder("^");
		boolean fixPart = true;
		ArrayList<Pattern> prefix = new ArrayList<Pattern>();
		while (matcher.find()) {
			String item = matcher.group();
			int length = item.length();
			char firstChar = item.charAt(0);
			if (firstChar == '*') {
				if (length > 1) {
					fixPart = false;
					buf.append(".*");// * 允许 0至多个非分割字符（\/）
				} else {
					buf.append("[^/]*");// * 允许 0至多个任意字符
				}
			} else if (firstChar == '/') {
				buf.append("[/]");
				if (fixPart) {
					prefix.add(Pattern.compile(buf.toString()));
				}
			} else {
				buf.append(Pattern.quote(item));
			}
		}
		buf.append("$");
		String ps = buf.toString();
		this.pattern = Pattern.compile(ps);
		this.must = ps.endsWith(".*$") ? this.pattern : null;
		this.parties = prefix.toArray(new Pattern[prefix.size()]);

	}

	public final boolean match(String url) {
		return pattern.matcher(url).find();
	}

	boolean maybe(String path) {
		if (parties.length > 0) {
			int p = path.lastIndexOf('/');
			int c = 0;
			while (p >= 0) {
				c++;
				if (c >= parties.length) {
					break;
				}
				p = path.lastIndexOf('/', p - 1);
			}
			if (c > 0) {
				return parties[c - 1].matcher(path).find();
			}
		}
		return true;

	}

	boolean must(String path) {
		return must != null && must.matcher(path).find();
	}

}

class MultiPathMatcher extends PathMatcher {

	private PathMatcher[] children;

	protected MultiPathMatcher(PathMatcher[] children) {
		this.children = children;

	}

	public boolean match(String path) {
		for (PathMatcher pm : children) {
			if (pm.match(path)) {
				return true;
			}
		}
		return false;
	}

	boolean maybe(String path) {
		for (PathMatcher pm : children) {
			if (pm.maybe(path)) {
				return true;
			}
		}
		return false;
	}

	boolean must(String path) {
		for (PathMatcher pm : children) {
			if (pm.must(path)) {
				return true;
			}
		}
		return false;
	}
}