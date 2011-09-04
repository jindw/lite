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

	/**
	 * @param path
	 *            path必须以/ 开头，且以/结尾 测试某个目录是否所有子文件都能匹配该模式。
	 */
	public abstract boolean must(String path);

	/**
	 * @param path
	 *            path必须以/ 开头，且以/结尾 测试某个目录是否可能包含能匹配该模式的子文件。
	 */
	public abstract boolean maybe(String path);

	/**
	 * @param path
	 *            path必须以/ 开头 测试某个路径能否匹配该模式。
	 */
	public abstract boolean match(String path);
}

class SinglePathMatcher extends PathMatcher {

	private static final Pattern MATCH_PARTY = Pattern.compile("\\*+|"
			+ "[^\\*/]+?|" + "[/]");
	private final Pattern pattern;
	private final Pattern[] parties;
	private final Pattern endParty;
	private final Pattern must;

	/**
	 * /path/.* /*.jpg =>/path/(?: .* /(?:.*)? )?
	 */
	protected SinglePathMatcher(String pattern) {
		Matcher matcher = MATCH_PARTY.matcher(pattern);
		StringBuilder buf = new StringBuilder("^");
		boolean fixPart = true;
		ArrayList<Pattern> prefix = new ArrayList<Pattern>();
		Pattern endParty = null;
		while (matcher.find()) {
			String item = matcher.group();
			int length = item.length();
			char firstChar = item.charAt(0);
			if (firstChar == '*') {
				if (length > 1) {
					if (fixPart) {
						fixPart = false;
						endParty = Pattern.compile(buf.toString() + ".*$");
					}
					buf.append(".*");// * 允许 0至多个非分割字符（\/）
				} else {
					buf.append("[^/]*");// * 允许 0至多个任意字符
				}
			} else if (firstChar == '/') {
				buf.append("[/]");
				if (fixPart) {
					prefix.add(Pattern.compile(buf.toString() + '$'));
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
		this.endParty = endParty;

	}

	public final boolean match(String url) {
		return pattern.matcher(url).find();
	}

	public boolean maybe(String path) {
		checkDir(path);
		if (parties.length > 0) {
			int p = path.lastIndexOf('/');
			int c = 0;
			while (p >= 0) {
				c++;
				if (c >= parties.length) {
					if (endParty != null) {
						return endParty.matcher(path).find();
					} else {
						break;
					}
				}
				p = path.lastIndexOf('/', p - 1);
			}
			if (c > 0) {
				return parties[c - 1].matcher(path).find();
			}
		}
		return true;

	}

	public boolean must(String path) {
		checkDir(path);
		return must != null && must.matcher(path).find();

	}

	private void checkDir(String path) {
		int len = path.length();
		if (len == 0 || path.charAt(0) != '/' || path.charAt(len - 1) != '/') {
			throw new IllegalArgumentException(
					"path must begin with and end with '/'!");
		}
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

	public boolean maybe(String path) {
		for (PathMatcher pm : children) {
			if (pm.maybe(path)) {
				return true;
			}
		}
		return false;
	}

	public boolean must(String path) {
		for (PathMatcher pm : children) {
			if (pm.must(path)) {
				return true;
			}
		}
		return false;
	}
}