package org.xidea.el.fn;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.ReflectUtil;

/**
 * 模拟ECMA262行为，保持基本一至，但迫于简单原则，略有偷懒行为^_^
 * 
 * @author jindw
 * @see org.mozilla.javascript.NativeGlobal
 */
public abstract class ECMA262Impl {
	private final static Class<?>[] ARRAY_CLASSES = new Class[] { List.class,
			Object[].class, int[].class, float[].class, double[].class,
			long[].class, short[].class, byte[].class, char[].class };

	public static void setup(ExpressionFactoryImpl calculater) {
		setup(calculater, JSArray.class, ARRAY_CLASSES);
		setup(calculater, JSNumber.class, Number.class);
		setup(calculater, JSString.class, String.class);
		JSGlobal.setupVar(calculater);
	}
	private static void setup(ExpressionFactoryImpl calculater,
			Class<? extends JSObject> impl, Class<?>... forClass) {
		try {
			Method[] dms = impl.getMethods();
			for (Method method : dms) {
				if (method.getDeclaringClass() == impl) {
					JSObject inv = impl.newInstance();
					Class<?>[] params = method.getParameterTypes();
					inv.method = method;
					if(params.length !=2 || params[1] != Object[].class){
						inv.params = params;
					}
					try {
						method.setAccessible(true);
					} catch (Exception e) {
					}
					for (Class<?> type : forClass) {
						calculater.addMethod(type, method.getName(), inv);
					}
				}
			}
		} catch (Exception e) {
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
			float f = ((Number) value).floatValue();
			return  f != 0 && !Float.isNaN(f);
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
						return parseNumber(text, 10);
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
		type = ReflectUtil.toWrapper(type);
		
		if(Number.class.isAssignableFrom(type)){
			Number n = ToNumber(value);
			return ReflectUtil.toValue(n, type);
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
			return toString((Number)value,10);
		}
		return String.valueOf(value);
	}

	static String toString(Number thiz, int radix) {
		if(radix <= 0  || radix > Character.MAX_RADIX){
			radix = 10;
		}
		if(thiz instanceof Double || thiz instanceof Float){
			return floatToString(thiz.doubleValue(),radix);
		}
		return Long.toString(thiz.longValue(), radix);
	}
    private static String floatToString(double d, int base) {
        if (Double.isNaN(d)) {
            return "NaN";
        } else if (Double.isInfinite(d)) {
            return (d > 0.0) ? "Infinity" : "-Infinity";
        } else if (d == 0) {
            // ALERT: should it distinguish -0.0 from +0.0 ?
            return "0";
        }
        if (base == 10) {
            String result = Double.toString(d);
            if(result.endsWith(".0")){
            	result = result.substring(0,result.length()-2);
            }
            return result;
        } else {
            return Long.toString((long)d,base);
        }
    }
}
