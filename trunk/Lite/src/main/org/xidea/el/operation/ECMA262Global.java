package org.xidea.el.operation;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模拟ECMA262行为，保持基本一至，但迫于简单原则，略有偷懒行为^_^
 * 
 * @author jindw
 * @see org.mozilla.javascript.NativeGlobal
 */
public class ECMA262Global {
	public static void appendTo(Map<String, Invocable> globalInvocableMap) {
		globalInvocableMap.put("encodeURI", new EncodeURI());
		globalInvocableMap.put("decodeURI", new DecodeURI());

		globalInvocableMap.put("encodeURIComponent", new EncodeURIComponent());
		globalInvocableMap.put("decodeURIComponent", new DecodeURIComponent());

		globalInvocableMap.put("isFinite", new IsFinite());
		globalInvocableMap.put("isNaN", new IsNaN());

		globalInvocableMap.put("parseFloat", new ParseFloat());
		globalInvocableMap.put("parseInt", new ParseInt());
	}

	public static class EncodeURIComponent implements Invocable {
		public Object invoke(Object thiz, Object... args) throws Exception {
			// 不完全等价于 ECMA 262标准，主要是' '->+|%20
			// 编码标准参照：application/x-www-form-urlencoded
			final String text = String.valueOf(getArg(args, 0, null));
			final String charset = String.valueOf(getArg(args, 1, "utf-8"));
			return process(text, charset);
		}

		protected Object process(final String text, String charset)
				throws UnsupportedEncodingException {
			return URLEncoder.encode(text, charset);
		}
	}

	public static class DecodeURIComponent extends EncodeURIComponent {
		@Override
		protected Object process(final String text, String charset)
				throws UnsupportedEncodingException {
			return URLDecoder.decode(text, charset);
		}
	}

	public static class EncodeURI extends EncodeURIComponent {
		private static final Pattern URL_SPLIT = Pattern.compile("[\\/\\:&\\?=]");
		@Override
		protected Object process(final String text, String charset)
				throws UnsupportedEncodingException {
			Matcher matcher = URL_SPLIT.matcher(text);
			StringBuilder buf = new StringBuilder();
			int end = 0;
			while (matcher.find()) {
				int start = matcher.start();
				if (start >= end) {
					buf
							.append(processPart(text.substring(end, start),
									charset));
				}
				buf.append(text.substring(start, end = matcher.end()));
			}
			buf.append(processPart(text.substring(end), charset));
			return buf.toString();
		}

		protected Object processPart(final String text, String charset)
				throws UnsupportedEncodingException {
			return URLEncoder.encode(text, charset);
		}
	}

	public static class DecodeURI extends EncodeURI {
		@Override
		protected Object processPart(final String text, String charset)
				throws UnsupportedEncodingException {
			return URLDecoder.decode(text, charset);
		}
	}

	public static class IsFinite implements Invocable {
		public Object invoke(Object thiz, Object... args) throws Exception {
			Object o = getArg(args, 0, Float.NaN);
			Number number = ECMA262Util.ToNumber(o);
			if (number instanceof Float || number instanceof Double) {
				float d = number.floatValue();
				return check(d);
			}
			return true;
		}

		protected Object check(float d) {
			return d == d && !Double.isInfinite(d);
		}
	}

	public static class IsNaN extends IsFinite {
		@Override
		protected Object check(float d) {
			return d != d;
		}
	}

	public static class ParseInt implements Invocable {
		private static final Pattern INT_PARTTERN = Pattern
				.compile("^0x[0-9a-f]+|^0+[0-7]*|^[0-9]+");

		public Object invoke(Object thiz, Object... args) throws Exception {
			String text = String.valueOf(getArg(args, 0, null)).trim()
					.toLowerCase();
			int length = text.length();
			if (length > 0) {
				Number result = parse(text);
				if (result != null) {
					return result;
				}
				// return Integer.valueOf(text);
			}
			return Float.NaN;
		}

		private Number parseInt(String text, int readio) {
			try {
				return new Integer(Integer.parseInt(text, readio));
			} catch (NumberFormatException e) {
				return new Long(Long.parseLong(text, readio));
			}
		}

		protected Number parse(String text) {
			Matcher matcher = INT_PARTTERN.matcher(text);
			if (matcher.find()) {
				text = matcher.group();
				if (text.startsWith("0x")) {
					return parseInt(text, 16);
				} else if (text.startsWith("0")) {
					return parseInt(text, 8);
				} else {
					return parseInt(text, 10);
				}
			} else {
				return Float.NaN;
			}
		}
	}

	public static class ParseFloat extends ParseInt {
		private static final Pattern FLOAT_PARTTERN = Pattern
				.compile("^[0-9]*\\.[0-9]+");

		// ECMA 262 parseInt,parseFloat不支持E[+-]?\d+
		@Override
		protected Number parse(String text) {
			Matcher matcher = FLOAT_PARTTERN.matcher(text);
			if (matcher.find()) {
				return Double.parseDouble(matcher.group());
			} else {
				return Float.NaN;
			}
		}
	}



	private static Object getArg(Object[] args, int index, Object defaultValue) {
		if (index >= 0 && index < args.length) {
			return args[index];
		} else {
			return defaultValue;
		}
	}
}
