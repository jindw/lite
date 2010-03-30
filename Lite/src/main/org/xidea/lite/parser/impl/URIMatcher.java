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
public abstract class URIMatcher implements Comparable<URIMatcher> {
	public static final URIMatcher createMatcher(String pattern) {
		pattern = pattern.trim();
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

	public static final URIMatcher createAndMatcher(URIMatcher... matchers) {
		return new AndMatcher(matchers);
	}

	public static final URIMatcher createOrMatcher(URIMatcher... matchers) {
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

	public int compareTo(URIMatcher o) {
		return o.length() - this.length;
	}


	static class AndMatcher extends URIMatcher {
		final URIMatcher[] matchers;

		public AndMatcher(URIMatcher... matchers) {
			for (int i = 0; i < matchers.length; i++) {
				URIMatcher matcher = matchers[i];
				length = Math.min(matcher.length(), length);
			}
			this.matchers = matchers;
		}

		public boolean match(String url) {
			for (int i = 0; i < matchers.length; i++) {
				URIMatcher matcher = matchers[i];
				if (!matcher.match(url)) {
					return false;
				}
			}
			return true;
		}
	}

	static final class OrMatcher extends AndMatcher {
		public OrMatcher(URIMatcher... matchers) {
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

	static final class StaticMatcher extends URIMatcher {
		protected final String pattern;

		public StaticMatcher(String pattern) {
			this.pattern = pattern;
			this.length = Character.MAX_VALUE + pattern.length();
		}

		public final boolean match(String url) {
			return pattern.equals(url);
		}
	}

	static final class AllMatcher extends URIMatcher {
		public final boolean match(String url) {
			return true;
		}
	}

	static final class BeginMatcher extends URIMatcher {
		private final String begin;

		public BeginMatcher(String begin) {
			this.begin = begin;
			this.length = 2 + begin.length();
		}

		public final boolean match(String url) {
			return url.startsWith(begin);
		}
	}

	static final class EndMatcher extends URIMatcher {
		private final String end;

		public EndMatcher(String end) {
			this.end = end;
			this.length = 2 + end.length();
		}

		public final boolean match(String url) {
			return url.endsWith(end);
		}
	}

	static final class BothMatcher extends URIMatcher {
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

	static final class PatternMatcher extends URIMatcher {
		private final Pattern pattern;

		public PatternMatcher(String pattern) {
			length = pattern.length();
			Matcher matcher = Pattern.compile("/?\\*\\*+|\\*+|[^\\*]+?").matcher(pattern);
			StringBuilder buf = new StringBuilder("^");
			while (matcher.find()) {
				String item = matcher.group();
				if (item.charAt(0) == '*') {
					if (item.length() > 1) {
						buf.append(".*");
					} else {
						buf.append("[^\\\\/]+");
					}
				}else if(item.length()>1 && item.charAt(1) == '*'){//*
					if(item.length()>2){
						buf.append("/?.*");
					}else{
						buf.append("/?[^\\\\/]+");
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
