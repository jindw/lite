package org.xidea.el.operation;

import java.util.Date;




/**
 * @author jindw
 * @see org.mozilla.javascript.NativeGlobal
 */
public abstract class ECMA262Util {

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

	public static Number parseNumber(String text, int radix) {
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

}
