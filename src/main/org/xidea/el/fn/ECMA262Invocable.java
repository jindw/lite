package org.xidea.el.fn;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.xidea.el.Invocable;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

public class ECMA262Invocable implements Invocable {

	final static Map<String, Object> MATH;

	final static Map<String, Object> JSON;


	static{
		//String[] jsonArgs = {"parse","stringify"};//100+
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("parse",new ECMA262Invocable(100));
		map.put("stringify",new ECMA262Invocable(101));
		JSON = Collections.unmodifiableMap(map);
		//math
		String[] mathArgs = {
				// 0,6
						"abs", "acos", "asin", "atan", "ceil", "asin", "cos",
						// 6,10
						"exp", "floor", "log", "round",
						// 11,13
						"sin", "sqrt", "tan"
						// 14,15,16,17+18
						, "random", "min", "max", "pow", "atan2" };
		map = new HashMap<String, Object>();
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
			map.put(mathArgs[i], new ECMA262Invocable(i));
		}
		MATH = Collections.unmodifiableMap(map);
		
	}

	final static Invocable isFinite = new ECMA262Invocable(200);
	final static Invocable isNaN = new ECMA262Invocable(201);
	final int type;

	ECMA262Invocable(int type) {
		this.type = type;
	}

	public Object invoke(Object thiz, Object... args) throws Exception {

		switch(type){
		//json
		case 100:return parse(JSObject.getStringArg(args, 0, null));
		case 101:return stringify(JSObject.getArg(args, 0, null));
		}
		//math,isFinite,isNaN
		Number n1 = JSObject.getNumberArg(args, 0, Double.NaN);
		switch (type) {
		//isFinite
		case 200:
			return !Double.isNaN(n1.doubleValue()) && !Double.isInfinite(n1.doubleValue());
		//isNaN
		case 201:
			return Double.isNaN(n1.doubleValue());
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
			return Math.pow(n1.doubleValue(),  JSObject.getNumberArg(args, 1, Double.NaN)
					.doubleValue());
		case 18:
			return atan2(n1.doubleValue(),  JSObject.getNumberArg(args, 1, Double.NaN)
					.doubleValue());
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
			Number n2 = ECMA262Impl.ToNumber( JSObject.getArg(args, i, Double.NaN));
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
	/*=========== JSON =============*/
	private final Object parse(Object value) {
		return JSONDecoder.decode(ECMA262Impl.ToPrimitive(value, String.class)
				.toString());
	}
	private final String stringify(Object value) {
		return JSONEncoder.encode(value);
	}
//	/*========== isFinite isNaN=============*/
//
//	private final Object isFinite(float d) {
//		return !Float.isNaN(d) && !Float.isInfinite(d);
//	}
//	private final Object isNaN(float d) {
//		return Float.isNaN(d);
//	}


}