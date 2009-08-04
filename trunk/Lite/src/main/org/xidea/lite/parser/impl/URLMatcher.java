/**
 * 
 */
package org.xidea.lite.parser.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dawei.Jin
 * 
 */
public abstract class URLMatcher implements Comparable<URLMatcher> {
	public static final URLMatcher createMatcher(String pattern) {
		int i = pattern.indexOf('*');
		if (i < 0) {
			return new StaticMatcher(pattern);
		}
		int j = pattern.lastIndexOf('*');
		if (j == i + 1) {
			if (i == 0) {
				return new EndMatcher(pattern.substring(2));
			}
			if (j == pattern.length() - 1) {
				return new BeginMatcher(pattern.substring(0, i));
			}
			return new BothMatcher(pattern.substring(0, i), pattern
					.substring(j + 1));
		}
		return new PatternMatcher(pattern);
	}

	public static final URLMatcher createAndMatcher(URLMatcher... matchers) {
		return new AndMatcher(matchers);
	}

	public static final URLMatcher createOrMatcher(URLMatcher... matchers) {
		return new OrMatcher(matchers);
	}

	protected int length;

	public int length() {
		return length;
	}

	public abstract boolean match(String url);

	@Override
	public String toString() {
		return "length:" + length;
	}

	public int compareTo(URLMatcher o) {
		return o.length() - this.length;
	}


	static class AndMatcher extends URLMatcher {
		final URLMatcher[] matchers;

		public AndMatcher(URLMatcher... matchers) {
			for (int i = 0; i < matchers.length; i++) {
				URLMatcher matcher = matchers[i];
				length = Math.min(matcher.length(), length);
			}
			this.matchers = matchers;
		}

		public boolean match(String url) {
			for (int i = 0; i < matchers.length; i++) {
				URLMatcher matcher = matchers[i];
				if (!matcher.match(url)) {
					return false;
				}
			}
			return true;
		}
	}

	static final class OrMatcher extends AndMatcher {
		public OrMatcher(URLMatcher... matchers) {
			super(matchers);
		}
		public boolean match(String url) {
			for (int i = 0; i < matchers.length; i++) {
				if (matchers[i].match(url)) {
					return true;
				}
			}
			return false;
		}
	}

	static final class StaticMatcher extends URLMatcher {
		protected final String pattern;

		public StaticMatcher(String pattern) {
			this.pattern = pattern;
			this.length = Character.MAX_VALUE + pattern.length();
		}

		public final boolean match(String url) {
			return pattern.equals(url);
		}
	}

	static final class AllMatcher extends URLMatcher {
		public final boolean match(String url) {
			return true;
		}
	}

	static final class BeginMatcher extends URLMatcher {
		private final String begin;

		public BeginMatcher(String begin) {
			this.begin = begin;
			this.length = 2 + begin.length();
		}

		public final boolean match(String url) {
			return url.startsWith(begin);
		}
	}

	static final class EndMatcher extends URLMatcher {
		private final String end;

		public EndMatcher(String end) {
			this.end = end;
			this.length = 2 + end.length();
		}

		public final boolean match(String url) {
			return url.endsWith(end);
		}
	}

	static final class BothMatcher extends URLMatcher {
		private final String begin;
		private final String end;

		public BothMatcher(String begin, String end) {
			this.begin = begin;
			this.end = end;
			length = 2 + begin.length() + end.length();
		}

		public final boolean match(String url) {
			return url.startsWith(begin)
					&& url.endsWith(end);
		}
	}

	static final class PatternMatcher extends URLMatcher {
		private final Pattern pattern;

		public PatternMatcher(String pattern) {
			length = pattern.length();
			Matcher matcher = Pattern.compile("\\*\\*+/?|\\*+|[^\\*]+").matcher(pattern);
			StringBuilder buf = new StringBuilder("^");
			while (matcher.find()) {
				String item = matcher.group();
				if (item.charAt(0) == '*') {
					if (item.length() > 1) {
						buf.append(".*");
					} else {
						buf.append("[^\\\\\\/]+");
					}
				} else {
					buf.append(Pattern.quote(item));
				}
			}
			buf.append("$");
			this.pattern = Pattern.compile(buf.toString());

		}

		public final boolean match(String url) {
			return pattern.matcher(url).find();
		}
	}
}
