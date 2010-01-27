package org.xidea.el.fn;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xidea.el.Invocable;
import org.xidea.el.impl.OperationStrategyImpl;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.json.JSONTokenizer;

/**
 * 模拟ECMA262行为，保持基本一至，但迫于简单原则，略有偷懒行为^_^
 * 
 * @author jindw
 * @see org.mozilla.javascript.NativeGlobal
 */
public abstract class ECMA262Impl {
	@SuppressWarnings("unchecked")
	private final static Class[] ARRAY_CLASSES = new Class[] { List.class,
			Object[].class, int[].class, float[].class, double[].class,
			long[].class, short[].class, byte[].class, char[].class };

	public static void setup(OperationStrategyImpl calculater) {
		setup(calculater, JSArray.class, ARRAY_CLASSES);
		setup(calculater, JSNumber.class, Number.class);
		setup(calculater, JSString.class, String.class);
	}
	@SuppressWarnings("unchecked")
	public static void setup(OperationStrategyImpl calculater,
			Class<? extends JSObject> impl, Class... forClass) {
		try {
			Method[] dms = impl.getMethods();
			for (Method method : dms) {
				if (method.getDeclaringClass() == impl) {
					JSObject inv = impl.newInstance();
					Class<?>[] params = method.getParameterTypes();
					inv.method = method;
					inv.params = params;
					inv.directly = params.length ==2 && params[1] == Object[].class;
					try {
						method.setAccessible(true);
					} catch (Exception e) {
					}
					for (Class type : forClass) {
						calculater.addMethod(type, method.getName(), inv);
					}
				}
			}
		} catch (Exception e) {
		}

	}
	public static void setup(Map<String, Object> globalMap) {
		globalMap.put("encodeURI", new URI(true));
		globalMap.put("decodeURI", new URI(false));

		globalMap.put("encodeURIComponent", new URIComponent(true));
		globalMap.put("decodeURIComponent", new URIComponent(false));

		globalMap.put("isFinite", new IsFiniteNaN(true));
		globalMap.put("isNaN", new IsFiniteNaN(false));

		globalMap.put("parseFloat", new ParseNumber(true));
		globalMap.put("parseInt", new ParseNumber(false));
		globalMap.put("Math", math);
		globalMap.put("Infinity", Double.POSITIVE_INFINITY);
		globalMap.put("NaN", Double.NaN);
		globalMap.put("JSON", new JSON());
	}


	private static NumberArithmetic na = new NumberArithmetic();

	private static Map<String, Object> math = MathInvocable.create();

	private static class MathInvocable implements Invocable {
		private static String[] args = {
		// 0,6
			"abs", "acos", "asin", "atan", "ceil", "asin", "cos",
				// 6,10
				"exp", "floor", "log", "round",
				// 11,13
				"sin", "sqrt", "tan"
				// 14,15,16,17+18
				, "random", "min", "max", "pow" ,"atan2"};
		final int type;

		MathInvocable(int type) {
			this.type = type;
		}

		static Map<String, Object> create() {
			math = new HashMap<String, Object>();
			double LN10 = Math.log(10);
			double LN2 = Math.log(2);
			math.put("E", Math.E);
			math.put("PI", Math.PI);
			math.put("LN10", LN10);
			math.put("LN2", LN2);
			math.put("LOG2E", 1 / LN2);
			math.put("LOG10E", 1 / LN10);
			math.put("SQRT1_2", Math.sqrt(0.5));
			math.put("SQRT2", Math.sqrt(2));
			for (int i = 0; i < args.length; i++) {
				math.put(args[i], new MathInvocable(i));
			}
			return Collections.unmodifiableMap(math);
		}

		public Object invoke(Object thiz, Object... args) throws Exception {

			Number n1 = getNumberArg(args, 0, Double.NaN);
			switch (type) {
			case 14:
				// 15.8.2.14 random()
				return Math.random();
			case 15:
				// 15.8.2.12 min([ value1 [, value2 [,...]]])
				return mimax(false, args);// min
			case 16:
				// 15.8.2.11 max([ value1 [, value2 [,...]]])
				return mimax(true, args);// max
			case 17:
				// 15.8.2.13 pow(x, y)
				return Math.pow(n1.doubleValue(), getNumberArg(args, 1, Double.NaN).doubleValue());
			case 18:
				return atan2(n1.doubleValue(), getNumberArg(args, 1, Double.NaN).doubleValue());
			case 0:
				return abs(n1);
			case 1:
				return acos(n1);
			case 2:
				return asin(n1);
			case 3:
				return atan(n1);
			case 4:
				return ceil(n1);
			case 5:
				return asin(n1);
			case 6:
				return cos(n1);
				
			//"exp", "floor", "log", "round",
			case 7:
				return exp(n1);
			case 8:
				return floor(n1);
			case 9:
				return log(n1);
			case 10:
				return round(n1);
			//"sin", "sqrt", "tan"
			case 11:
				return sin(n1);
			case 12:
				return sqrt(n1);
			case 13:
				return tan(n1);
			}
			return 0;
		}

		public Object mimax(boolean max, Object... args) throws Exception {
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
					case 1:// n1>n2
						if (!max) {// min
							n1 = n2;
						}
						break;
					case -1:// n2>n1
						if (max) {
							n1 = n2;
						}
					}
				}
			}
			return n1;
		}

		// 15.8.2.1 abs(x)
		public Object abs(Number x) {
			if (na.compare(0, x, 2) == 1) {
				x = na.subtract(0, x);
			}
			return x;
		}

		// 15.8.2.2 acos(x)
		public Object acos(Number x) {
			return Math.acos(x.doubleValue());
		}

		// 15.8.2.3 asin(x);
		public Object asin(Number x) {
			return Math.asin(x.doubleValue());
		}

		// 15.8.2.4 atan(x)
		public Object atan(Number x) {
			return Math.atan(x.doubleValue());
		}
		// 15.8.2.5 atan2(x)
		public Object atan2(Number x,Number y) {
			return Math.atan2(x.doubleValue(),y.doubleValue());
		}

		// 15.8.2.6 ceil(x)
		public Object ceil(Number x) {
			return Math.ceil(x.doubleValue());
		}

		// 15.8.2.7 cos(x)
		public Object cos(Number x) {
			return Math.cos(x.doubleValue());
		}

		// 15.8.2.8 exp(x)
		public Object exp(Number x) {
			return Math.exp(x.doubleValue());
		}

		// 15.8.2.9 floor(x)
		public Object floor(Number x) {
			return Math.floor(x.doubleValue());
		}

		// 15.8.2.10 log(x)
		public Object log(Number x) {
			return Math.log(x.doubleValue());
		}

		// 15.8.2.15 round(x)
		public Object round(Number x) {
			return Math.round(x.doubleValue());
		}

		// 15.8.2.16 sin(x)
		public Object sin(Number x) {
			return Math.sin(x.doubleValue());
		}

		// 15.8.2.17 sqrt(x)
		public Object sqrt(Number x) {
			return Math.sqrt(x.doubleValue());
		}

		// 15.8.2.18 tan(x)
		public Object tan(Number x) {
			return Math.tan(x.doubleValue());
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

	public static class URIComponent implements Invocable {
		final boolean encode;

		URIComponent(boolean encode) {
			this.encode = encode;
		}

		public Object invoke(Object thiz, Object... args) throws Exception {
			// 不完全等价于 ECMA 262标准，主要是' '->+|%20
			// 编码标准参照：application/x-www-form-urlencoded
			final String text = String.valueOf(getArg(args, 0, null));
			final String charset = String.valueOf(getArg(args, 1, "utf-8"));
			return process(text, charset);
		}

		protected Object process(final String text, String charset)
				throws UnsupportedEncodingException {
			return encode ? URLEncoder.encode(text, charset) : URLDecoder
					.decode(text, charset);
		}
	}

	public static class URI extends URIComponent {
		URI(boolean encode) {
			super(encode);
		}

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
			return super.process(text, charset);
		}
	}

	public static class IsFiniteNaN implements Invocable {
		private boolean isFinite;

		IsFiniteNaN(boolean isFinite) {
			this.isFinite = isFinite;
		}

		public Object invoke(Object thiz, Object... args) throws Exception {
			Object o = getArg(args, 0, Float.NaN);
			Number number = ToNumber(o);
			float d = number.floatValue();
			return check(d);
		}

		protected Object check(float d) {
			if (isFinite) {
				return d == d && !Double.isInfinite(d);
			} else {
				return d != d;
			}
		}
	}

	public static class ParseNumber implements Invocable {
		private static final Pattern INT_PARTTERN = Pattern
			.compile("^[\\+\\-]?(0x[0-9a-fA-F]+" +
					"|0+[0-7]*" +
					"|[1-9][0-9]*)");
		private static final Pattern FLOAT_PARTTERN = Pattern
			.compile("^[\\+\\-]?[0-9]*(?:\\.[0-9]+)?");

		private boolean parseFloat;

		ParseNumber(boolean parseFloat) {
			this.parseFloat = parseFloat;
		}

		public Object invoke(Object thiz, Object... args) throws Exception {
			String text = String.valueOf(getArg(args, 0, null)).trim()
					.toLowerCase();
			int length = text.length();
			if (length > 0) {
				Number result = parseFloat ? parseFloat(text) : parseInt(text);
				if (result != null) {
					return result;
				}
				// return Integer.valueOf(text);
			}
			return Float.NaN;
		}

		// ECMA 262 parseInt,parseFloat不支持E[+-]?\d+
		protected Number parseFloat(String text) {
			Matcher matcher = FLOAT_PARTTERN.matcher(text);
			if (matcher.find()) {
				return Double.parseDouble(matcher.group(0));
			} else {
				return Float.NaN;
			}
		}

		protected Number parseInt(String text) {
			Matcher matcher = INT_PARTTERN.matcher(text);
			if (matcher.find()) {
				text = matcher.group(0);
				String n = matcher.group(1);
				if (n.startsWith("0x")) {
					return Long.parseLong(text.replaceFirst("0x", ""), 16);
				} else if (n.startsWith("0")) {
					return parseInt(text, 8);
				} else {
					return parseInt(text, 10);
				}
			} else {
				return parseFloat(text).intValue();
			}
		}

		private Number parseInt(String text, int readio) {
			try {
				return new Integer(Integer.parseInt(text, readio));
			} catch (NumberFormatException e) {
				return new Long(Long.parseLong(text, readio));
			}
		}

	}

	protected static Object getArg(Object[] args, int index, Object defaultValue) {
		if (index >= 0 && index < args.length) {
			return args[index];
		} else {
			return defaultValue;
		}
	}

	static String getStringArg(Object[] args, int index,
			String defaultValue) {
		Object value = getArg(args, index, null);
		if(value == null){
			return null;
		}
		return ToString(value);
	}

	static Number getNumberArg(Object[] args, int index,
			Number defaultValue) {
		Object value = getArg(args, index, defaultValue);
		return ToNumber(value);
	}

	static int getIntArg(Object[] args, int index,
			Integer defaultValue) {
		Number value = getNumberArg(args, index, defaultValue);
		return value.intValue();
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
				float f = ((Number) value).floatValue();
				return  f != 0 && f==f;
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
				if(text.length()>1 ){
					char c1 = text.charAt(0);
					char c2 = text.charAt(1);
					if(c1 == '+' || c1 == '-'){
						c1=c2;
						if(text.length()>2){
							c2 = text.charAt(2);
						}
					}
					if (c1 == '0') {
						if (c2== 'x' || c2 == 'X') {
							return parseNumber(text.substring(2), 16);
						}
						return parseNumber(text, 8);
					}else if (text.indexOf('E' )>0|| text.indexOf('e')>0) {
						return Float.parseFloat(text);
					}
				}
				return parseNumber(text, 10);
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
	public static Object ToValue(Object value, Class<?> type) {
		if(type == String.class){
			return value == null?null:value.toString();
		}else if(type == Character.class){
			if(value == null){
				return (char)0;
			}
			value = ToPrimitive(value, String.class);
			if(value instanceof Number){
				return (char)((Number)value).intValue();
			}
			String text = (String)value;
			if(text.length()>0){
				return text.charAt(0);
			}else{
				return 0;
			}
		}
		type = NumberArithmetic.toWrapper(type);
		
		if(Number.class.isAssignableFrom(type)){
			Number n = ToNumber(value);
			return NumberArithmetic.getValue(type, n);
		}
		
		//Boolean
		if(type == Boolean.class){
			return ToBoolean(ToPrimitive(value,type));
		}
		return value;
	}
	public static String ToString(Object value) {
		value = ToPrimitive(value, String.class);
		if(value instanceof Number){
			if(((Number)value).doubleValue() == 0){
				return "0";
			}
		}
		return String.valueOf(value);
	}
}
