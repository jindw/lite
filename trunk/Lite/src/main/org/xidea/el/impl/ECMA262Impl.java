package org.xidea.el.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xidea.el.Invocable;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.json.JSONTokenizer;

/**
 * 模拟ECMA262行为，保持基本一至，但迫于简单原则，略有偷懒行为^_^
 * 
 * @author jindw
 * @see org.mozilla.javascript.NativeGlobal
 */
public abstract class ECMA262Impl {
	public static void appendTo(Map<String, Object> globalInvocableMap) {
		globalInvocableMap.put("encodeURI", new EncodeURI());
		globalInvocableMap.put("decodeURI", new DecodeURI());

		globalInvocableMap.put("encodeURIComponent", new EncodeURIComponent());
		globalInvocableMap.put("decodeURIComponent", new DecodeURIComponent());

		globalInvocableMap.put("isFinite", new IsFinite());
		globalInvocableMap.put("isNaN", new IsNaN());

		globalInvocableMap.put("parseFloat", new ParseFloat());
		globalInvocableMap.put("parseInt", new ParseInt());
		globalInvocableMap.put("Math", math);
		globalInvocableMap.put("Infinity", Double.POSITIVE_INFINITY);
		globalInvocableMap.put("NaN", Double.NaN);
		globalInvocableMap.put("JSON", new JSON());
	}

	private static NumberArithmetic na = new NumberArithmetic();

	private static Map<String, Object> math;
	static {
		double LN10 = Math.log(10);
		double LN2 = Math.log(2);
		math = new HashMap<String, Object>();
		math.put("E", Math.E);
		math.put("PI", Math.PI);
		math.put("LN10", LN10);
		math.put("LN2", LN2);
		math.put("LOG2E", 1 / LN2);
		math.put("LOG10E", 1 / LN10);
		math.put("SQRT1_2", Math.sqrt(0.5));
		math.put("SQRT2", Math.sqrt(2));
		//15.8.2.11 max([ value1 [, value2 [,...]]])
		math.put("min", new MaxMin(false));
		//15.8.2.12 min([ value1 [, value2 [,...]]])
		math.put("max", new MaxMin(true));
		//15.8.2.14 random()
		math.put("random", new Invocable() {
			// 15.8.2.14 random()
			public Object invoke(Object thizz, Object... args) throws Exception {
				return Math.random();
			}
		});
		//15.8.2.1 abs(x)
		math.put("abs", new DoubleInvocable(){
			@Override
			Object compute(Number x) {
				if(na.compare(0,x, 2) == 1){
					x = na.subtract(0, x);
				}
				return x;
			}
		});
		//15.8.2.2 acos(x) 
		math.put("acos", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.acos(x.doubleValue());
			}
		});
		//15.8.2.3 asin(x)
		math.put("asin", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.asin(x.doubleValue());
			}
		});
		//15.8.2.4 atan(x)
		math.put("atan", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.atan(x.doubleValue());
			}
		});
		//15.8.2.6 ceil(x)
		math.put("ceil", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.ceil(x.doubleValue());
			}
		});
		//15.8.2.7 cos(x)
		math.put("cos", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.cos(x.doubleValue());
			}
		});
		//15.8.2.8 exp(x)
		math.put("exp", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.exp(x.doubleValue());
			}
		});
		//15.8.2.9 floor(x)
		math.put("floor", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.floor(x.doubleValue());
			}
		});
		//15.8.2.10 log(x)
		math.put("log", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.log(x.doubleValue());
			}
		});
		//15.8.2.13 pow(x, y)
		math.put("pow", new Double2Invocable(){
			Object compute(Number x,Number y) {
				return Math.pow(x.doubleValue(),y.doubleValue());
			}
		});
		//15.8.2.15 round(x)
		math.put("round", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.round(x.doubleValue());
			}
		});
		//15.8.2.16 sin(x)
		math.put("sin", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.sin(x.doubleValue());
			}
		});
		//15.8.2.17 sqrt(x)
		math.put("sqrt", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.sqrt(x.doubleValue());
			}
		});
		//15.8.2.18 tan(x)
		math.put("tan", new DoubleInvocable(){
			Object compute(Number x) {
				return Math.tan(x.doubleValue());
			}
		});
		math = Collections.unmodifiableMap(math);
	}
	private abstract static class DoubleInvocable implements Invocable {
		public Object invoke(Object thiz, Object... args) throws Exception {
			Number n1 = ToNumber(getArg(args, 0, Double.NaN));
			return this.compute(n1);
		}
		abstract Object compute(Number x) ;
	}
	private abstract static class Double2Invocable implements Invocable {
		public Object invoke(Object thiz, Object... args) throws Exception {
			Number n1 = ToNumber(getArg(args, 0, Double.NaN));
			Number n2 = ToNumber(getArg(args, 1, Double.NaN));
			return this.compute(n1,n2);
		}
		abstract Object compute(Number x,Number y) ;
	}

	private static class MaxMin implements Invocable {
		private boolean max;

		public MaxMin(boolean max) {
			this.max = max;
		}

		public Object invoke(Object thiz, Object... args) throws Exception {
			Number n1 = null;
			for (int i = 0; i < args.length; i++) {
				Number n2 = ToNumber(getArg(args, i, Float.NaN));
				if (NumberArithmetic.isNaN(n2)) {
					return n2;
				}
				if (max) {
					if (NumberArithmetic.isPI(n2)) {
						return n2;
					}
				} else {
					if (NumberArithmetic.isNI(n2)) {
						return n2;
					}
				}
				if (i == 0) {
					n1 = n2;
				} else {
					switch (na.compare(n1, n2, 8)) {
					case 1://n1>n2
						if (!max) {// min
							n1 = n2;
						}
						break;
					case -1://n2>n1
						if (max) {
							n1 = n2;
						}
					}
				}
			}
			return n1;
		}
	}

	public static class JSON {
		public static final Object stringify(Object value) {
			return encode(value);
		}

		public static final Object encode(Object value) {
			return JSONEncoder.encode(value);
		}

		public static final Object parse(Object value) {
			return decode(value);
		}

		public static final Object decode(Object value) {
			return new JSONTokenizer(ToPrimitive(value, String.class)
					.toString()).parse();
		}

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
		private static final Pattern URL_SPLIT = Pattern
				.compile("[\\/\\:&\\?=]");

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
			Number number = ToNumber(o);
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

	private static Number parseNumber(String text, int radix) {
		try {
			return Integer.parseInt(text, radix);
		} catch (Exception e) {
			return Long.parseLong(text, radix);
		}
	}

	/**
	 * @param value
	 * @see <a
	 *      href="http://www.ecma-international.org/publications/standards/Ecma-262.htm">Ecma262</a>
	 * @return
	 */
	public static boolean ToBoolean(Object value) {
		if (value == null) {
			return false;
		} else if (value instanceof Number) {
			if (value instanceof Float || value instanceof Double) {
				return ((Number) value).floatValue() != 0;
			} else if (value instanceof Long) {
				return ((Number) value).longValue() != 0;
			} else {
				return ((Number) value).intValue() != 0;
			}
		} else if (value instanceof String) {
			return ((String) value).length() > 0;
		} else if (value instanceof Boolean) {
			return (Boolean) value;
		} else {
			return true;
		}
	}

	/**
	 * @param arg1
	 * @param force
	 * @see <a
	 *      href="http://www.ecma-international.org/publications/standards/Ecma-262.htm">Ecma262</a>
	 * @return
	 */
	public static Number ToNumber(Object value) {
		value = ToPrimitive(value, String.class);
		if (value == null) {
			return 0;
		} else if (value instanceof Boolean) {
			return ((Boolean) value) ? 1 : 0;
		} else if (value instanceof Number) {
			return (Number) value;
		} else {
			String text = (String) value;
			try {
				if (text.indexOf('.') >= 0) {
					return Float.parseFloat(text);
				}
				if (text.startsWith("0x")) {
					return parseNumber(text.substring(2), 16);
				} else if (text.startsWith("0")) {
					return parseNumber(text.substring(1), 8);
				} else {
					return parseNumber(text, 10);
				}
			} catch (NumberFormatException ex) {
				return Double.NaN;
			}
		}
	}

	/**
	 * 
	 * @param <T>
	 * @param value
	 * @param expectedType
	 * @see <a
	 *      href="http://www.ecma-international.org/publications/standards/Ecma-262.htm">Ecma262</a>
	 * @return <null|Number|Boolean|String>
	 */
	@SuppressWarnings("unchecked")
	public static Object ToPrimitive(Object value, Class<?> expectedType) {
		boolean toString;
		if (expectedType == Number.class) {
			toString = false;
		} else if (expectedType == String.class) {
			toString = true;
		} else if (expectedType == null) {
			toString = !(value instanceof Date);
		} else {
			throw new IllegalArgumentException(
					"expectedType 只能是 Number或者String");
		}
		if (value == null) {
			return null;
		} else if (value instanceof Boolean) {
			return value;
		} else if (value instanceof Number) {
			return value;
		} else if (value instanceof String) {
			return value;
		}

		if (toString) {
			return String.valueOf(value);
		} else {
			if (value instanceof Date) {
				return new Long(((Date) value).getTime());
			} else {
				return String.valueOf(value);
			}
		}
	}
}
