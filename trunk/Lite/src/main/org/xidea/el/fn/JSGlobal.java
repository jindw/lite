package org.xidea.el.fn;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xidea.el.Invocable;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

class JSGlobal implements Invocable {

	static String[] mathArgs = {
	// 0,6
			"abs", "acos", "asin", "atan", "ceil", "asin", "cos",
			// 6,10
			"exp", "floor", "log", "round",
			// 11,13
			"sin", "sqrt", "tan"
			// 14,15,16,17+18
			, "random", "min", "max", "pow", "atan2" };
	static void setupVar(ExpressionFactoryImpl calculater) {
		/*==== Math 0+ ====*/
		Map<String, Object> map = new HashMap<String, Object>();
		double LN10 = Math.log(10);
		double LN2 = Math.log(2);
		map.put("E", Math.E);
		map.put("PI", Math.PI);
		map.put("LN10", LN10);
		map.put("LN2", LN2);
		map.put("LOG2E", 1 / LN2);
		map.put("LOG10E", 1 / LN10);
		map.put("SQRT1_2", Math.sqrt(0.5));
		map.put("SQRT2", Math.sqrt(2));
		for (int i = 0; i < mathArgs.length; i++) {
			map.put(mathArgs[i], new JSGlobal(i));
		}
		calculater.addVar("Math", map);
		
		/*=====JSON = {"parse","stringify"};//100+ =====*/
		map = new HashMap<String, Object>();
		map.put("parse", new JSGlobal(100));
		map.put("stringify", new JSGlobal(101));
		calculater.addVar("JSON", Collections.unmodifiableMap(map));
		
		
		/*======isFinite,isNaN 200+======*/
		calculater.addVar("isFinite", new JSGlobal(200));
		calculater.addVar("isNaN", new JSGlobal(201));

		/*==========parseInt,parseFloat 300+=============*/
		calculater.addVar("parseInt", new JSGlobal(300));
		calculater.addVar("parseFloat", new JSGlobal(301));
		/*===========encode,decode uri 400+================*/
		calculater.addVar("encodeURI", new JSGlobal(400));
		calculater.addVar("decodeURI",new JSGlobal(401));
		calculater.addVar("encodeURIComponent", new JSGlobal(402));
		calculater.addVar("decodeURIComponent", new JSGlobal(403));

		/*==========Others ===========*/
		calculater.addVar("Infinity", Double.POSITIVE_INFINITY);
		calculater.addVar("NaN", Double.NaN);
	}

	final int type;
	JSGlobal(int type) {
		this.type = type;
	}

	public String toString(){
		switch (type) {
		//encodeURI,decodeURI,encodeURIConponent,decodeURIComponent
		case 400:return "encodeURI";
		case 401:return "decodeURI";
		case 402:return "encodeURIConponent";
		case 403:return "decodeURIComponent";
		//parseInt,parseFloat
		case 300:return "parseInt";
		case 301:return "parseFloat";
		case 200:return "isFinite";
		case 201:return "isNaN";
		case 100:return "JSON.parse";
		case 101:return "JSON.stringify";
		default:
			if(type>0 && type<mathArgs.length){
				return "Math."+mathArgs[type];
			}
			return "unknow method:"+type;
		}
	}
	public Object invoke(Object thiz, Object... args) throws Exception {

		switch (type) {
		//encodeURI,decodeURI,encodeURIConponent,decodeURIComponent
		case 400://encodeURI
		case 401://decodeURI
		case 402://encodeURIConponent
		case 403://decodeURIComponent
			String text = String.valueOf(JSObject.getArg(args, 0, "null"));
			final String charset = String.valueOf(JSObject.getArg(args, 1,
					"utf-8"));
			return edscode(0 == (type&1), type<402, text, charset);
		//parseInt,parseFloat
		case 300://parseInt
		case 301://parseFloat
			text = JSObject.getStringArg(args, 0, "").trim();
			if(type == 301){
				return parseFloat(text);
			}
			int radix = JSObject.getNumberArg(args, 1, -1).intValue();
			return parseInt(text.trim(),radix);
			
		// json
		case 100:
			return parse(JSObject.getStringArg(args, 0, null));
		case 101:
			return stringify(JSObject.getArg(args, 0, null));
		}
		//isFinite,isNaN,math
		switch (type) {
		// isFinite
		case 200:
			Number n1 = JSObject.getNumberArg(args, 0, null);
			if(n1 == null){
				return true;
			}
			return !Double.isNaN(n1.doubleValue())
					&& !Double.isInfinite(n1.doubleValue());
			// isNaN
		case 201:
			n1 = JSObject.getNumberArg(args, 0, null);
			if(n1 == null){
				return false;
			}
			return Double.isNaN(n1.doubleValue());
		}
		Number n1 = JSObject.getNumberArg(args, 0, Double.NaN);
		switch (type) {
		// math,
		
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
			return Math.pow(n1.doubleValue(), JSObject.getNumberArg(args, 1,
					Double.NaN).doubleValue());
		case 18:
			return atan2(n1.doubleValue(), JSObject.getNumberArg(args, 1,
					Double.NaN).doubleValue());
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

			// "exp", "floor", "log", "round",
		case 7:
			return exp(n1);
		case 8:
			return floor(n1);
		case 9:
			return log(n1);
		case 10:
			return round(n1);
			// "sin", "sqrt", "tan"
		case 11:
			return sin(n1);
		case 12:
			return sqrt(n1);
		case 13:
			return tan(n1);
		}
		return 0;
	}

	private final Object mimax(boolean max, Object... args) throws Exception {
		Number n1 = null;
		for (int i = 0; i < args.length; i++) {
			Number n2 = ECMA262Impl.ToNumber(JSObject.getArg(args, i,
					Double.NaN));
			double f2 = n2.floatValue();
			if (f2 == Float.NaN) {
				return n2;
			}
			if (max) {
				if (Float.POSITIVE_INFINITY == f2) {
					return n2;
				}
			} else {
				if (Float.NEGATIVE_INFINITY == f2) {
					return n2;
				}
			}
			if (i == 0) {
				n1 = n2;
			} else {
				if (f2 > n1.doubleValue()) {// n2>n1
					if (max) {
						n1 = n2;
					}
				} else {// n1>=n2
					if (!max) {// min
						n1 = n2;
					}
				}
			}
		}
		return n1;
	}

	// 15.8.2.1 abs(x)
	private final Object abs(Number x) {
		double d = x.doubleValue();
		if (d < 0) {
			return -d;
		}
		return x;
	}

	// 15.8.2.2 acos(x)
	private final Object acos(Number x) {
		return Math.acos(x.doubleValue());
	}

	// 15.8.2.3 asin(x);
	private final Object asin(Number x) {
		return Math.asin(x.doubleValue());
	}

	// 15.8.2.4 atan(x)
	private final Object atan(Number x) {
		return Math.atan(x.doubleValue());
	}

	// 15.8.2.5 atan2(x)
	private final Object atan2(Number x, Number y) {
		return Math.atan2(x.doubleValue(), y.doubleValue());
	}

	// 15.8.2.6 ceil(x)
	private final Object ceil(Number x) {
		return Math.ceil(x.doubleValue());
	}

	// 15.8.2.7 cos(x)
	private final Object cos(Number x) {
		return Math.cos(x.doubleValue());
	}

	// 15.8.2.8 exp(x)
	private final Object exp(Number x) {
		return Math.exp(x.doubleValue());
	}

	// 15.8.2.9 floor(x)
	private final Object floor(Number x) {
		return Math.floor(x.doubleValue());
	}

	// 15.8.2.10 log(x)
	private final Object log(Number x) {
		return Math.log(x.doubleValue());
	}

	// 15.8.2.15 round(x)
	private final Object round(Number x) {
		return Math.round(x.doubleValue());
	}

	// 15.8.2.16 sin(x)
	private final Object sin(Number x) {
		return Math.sin(x.doubleValue());
	}

	// 15.8.2.17 sqrt(x)
	private final Object sqrt(Number x) {
		return Math.sqrt(x.doubleValue());
	}

	// 15.8.2.18 tan(x)
	private final Object tan(Number x) {
		return Math.tan(x.doubleValue());
	}

	/* =========== JSON ============= */
	private final Object parse(Object value) {
		return JSONDecoder.decode(ECMA262Impl.ToPrimitive(value, String.class)
				.toString());
	}

	private final String stringify(Object value) {
		return JSONEncoder.encode(value);
	}

	// /*========== isFinite isNaN=============*/
	//
	// private final Object isFinite(float d) {
	// return !Float.isNaN(d) && !Float.isInfinite(d);
	// }
	// private final Object isNaN(float d) {
	// return Float.isNaN(d);
	// }
	/** ===================number parse ======================= */
	private static final Pattern INT_PARTTERN = Pattern
			.compile("^[\\+\\-]?(0x[0-9a-f]+" + "|0+[0-7]*"
					+ "|[1-9][0-9]*)",Pattern.CASE_INSENSITIVE);
	private static final Pattern FLOAT_PARTTERN = Pattern
			.compile("^[\\+\\-]?[0-9]*(?:\\.[0-9]+)?");

	// public Object invoke(Object thiz, Object... args) throws Exception {
	// String text = String.valueOf(JSObject.getArg(args, 0, null)).trim()
	// .toLowerCase();
	// int length = text.length();
	// if (length > 0) {
	// Number result = parseFloat ? parseFloat(text) : parseInt(text);
	// if (result != null) {
	// return result;
	// }
	// // return Integer.valueOf(text);
	// }
	// return Float.NaN;
	// }

	// ECMA 262 parseInt,parseFloat不支持E[+-]?\d+
	protected Number parseFloat(String text) {
		if (text.length() > 0) {
			Matcher matcher = FLOAT_PARTTERN.matcher(text);
			if (matcher.find()) {
				return Double.parseDouble(matcher.group(0));
			}
		}
		return Double.NaN;
	}

	protected Number parseInt(String text,int radix) {
		Matcher matcher = INT_PARTTERN.matcher(text);
		if (matcher.find()) {
			text = matcher.group(0);
			if (radix > 0) {
				return Long.parseLong(text, radix);
			}
			String n = matcher.group(1);
			if (n.charAt(0) == '0') {
				if(n.length() ==1){
					return 0;
				}else{
					int c = n.charAt(1);
					if (c == 'x' || c == 'X') {
						return Long.parseLong(text.charAt(0)+n.substring(2), 16);
					}
				}
				return Long.parseLong(text, 8);
			} else {
				return Long.parseLong(text, 10);
			}
		} else {
			return parseFloat(text).intValue();
		}
	}
	/*================URL Encoder====================*/
	private static final Pattern URL_ENCODE_SPLIT = Pattern.compile("[;/?:@&=+$,#]");//-_.!~*'()
	private static final Pattern URL_DECODE_SPLIT = Pattern.compile("\\+|%3B|%2F|%3F|%3A|%40|%26|%3D|%2B|%24|%2C|%23");

	protected Object edscode(boolean encode,boolean split,final String text, String charset)
			throws UnsupportedEncodingException {
		if (split) {
			Matcher matcher = (encode?URL_ENCODE_SPLIT:URL_DECODE_SPLIT).matcher(text);
			StringBuilder buf = new StringBuilder();
			int end = 0;
			while (matcher.find()) {
				int start = matcher.start();
				if (start >= end) {
					buf
							.append(processPart(encode,text.substring(end, start),
									charset));
				}
				buf.append(text.substring(start, end = matcher.end()));
			}
			buf.append(processPart(encode,text.substring(end), charset));
			return buf.toString();
		} else {
			return processPart(encode,text, charset);
		}
	}

	protected Object processPart(boolean encode,final String text, String charset)
			throws UnsupportedEncodingException {
		return encode ? URLEncoder.encode(text, charset) : URLDecoder.decode(
				text, charset);
	}

}