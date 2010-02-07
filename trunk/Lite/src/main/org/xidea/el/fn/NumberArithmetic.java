package org.xidea.el.fn;

import org.xidea.el.impl.ReflectUtil;

/**
 * 数字加减乘除四则运算，主要处理类型混合运算，如：Integer + Double
 * 
 * @author liangfei0201@163.com
 * @author jindw
 */

public class NumberArithmetic {
	public final static boolean isNaN(Number n1) {
		if (n1 instanceof Double || n1 instanceof Float) {
			float f = n1.floatValue();
			return f != f;
		}
		return false;
	}

	public final static boolean isNI(Number n1) {
		return (n1 instanceof Double || n1 instanceof Float)
				&& n1.floatValue() == Float.NEGATIVE_INFINITY;
	}

	public final static boolean isPI(Number n1) {
		return (n1 instanceof Double || n1 instanceof Float)
				&& n1.floatValue() == Float.POSITIVE_INFINITY;
	}

	protected static boolean isType(Class<?> clazz, Number n1, Number n2) {
		return clazz.isInstance(n1) || clazz.isInstance(n2);
	}

	public static Number getValue(Class<? extends Object> type, Number value) {
		if (type == Long.class) {
			return value.longValue();
		} else if (type == Integer.class) {
			return value.intValue();
		} else if (type == Short.class) {
			return value.shortValue();
		} else if (type == Byte.class) {
			return value.byteValue();
		} else if (type == Double.class) {
			return value.doubleValue();
		} else if (type == Float.class) {
			return value.floatValue();
		} else {
			Class<? extends Object> clazz = ReflectUtil.toWrapper(type);
			if(clazz == type){
				return null;
			}else{
				return getValue(clazz, value);
			}
			
		}
	}

	/**
	 * 加法运算
	 * 
	 * @param n1
	 *            左参数
	 * @param n2
	 *            右参数
	 * @return 结果 0,1,-1,validReturn
	 */
	public int compare(Number n1, Number n2, int validReturn) {
		if (isNaN(n1) || isNaN(n2)) {
			return validReturn;
		} else if (isPI(n1)) {
			return isPI(n2) ? 0 : 1;
		} else if (isNI(n1)) {
			return isNI(n2) ? 0 : -1;
		} else if (isNI(n2)) {
			return 1;
		} else if (isPI(n2)) {
			return -1;
		}
		if (isType(Double.class, n1, n2)) {
		} else if (isType(Float.class, n1, n2)) {
		} else if (isType(Long.class, n1, n2)) {
		} else if (isType(Integer.class, n1, n2) || isType(Short.class, n1, n2)
				|| isType(Byte.class, n1, n2)) {
			int offset = n1.intValue() - n2.intValue();
			if (offset == 0) {
				return 0;
			} else {
				return offset > 0 ? 1 : -1;
			}
		}
		double offset = n1.doubleValue() - n2.doubleValue();
		if (offset == 0) {
			return 0;
		} else {
			return offset > 0 ? 1 : -1;
		}
	}

	/**
	 * 加法运算
	 * 
	 * @param n1
	 *            左参数
	 * @param n2
	 *            右参数
	 * @return 结果
	 */
	public Number add(Number n1, Number n2) {
		if (isType(Double.class, n1, n2)) {
		} else if (isType(Float.class, n1, n2)) {
			return n1.floatValue() + n2.floatValue();
		} else if (isType(Long.class, n1, n2)) {
			return n1.longValue() + n2.longValue();
		} else if (isType(Integer.class, n1, n2) || isType(Short.class, n1, n2)
				|| isType(Byte.class, n1, n2)) {
			return n1.intValue() + n2.intValue();
		}
		return n1.doubleValue() + n2.doubleValue();
	}

	/**
	 * 减法运算
	 * 
	 * @param n1
	 *            左参数
	 * @param n2
	 *            右参数
	 * @return 结果
	 */
	public Number subtract(Number n1, Number n2) {
		if (isType(Double.class, n1, n2)) {
		} else if (isType(Float.class, n1, n2)) {
			return n1.floatValue() - n2.floatValue();
		} else if (isType(Long.class, n1, n2)) {
			return n1.longValue() - n2.longValue();
		} else if (isType(Integer.class, n1, n2) || isType(Short.class, n1, n2)
				|| isType(Byte.class, n1, n2)) {
			return n1.intValue() - n2.intValue();
		}
		return n1.doubleValue() - n2.doubleValue();
	}

	/**
	 * 乘法运算
	 * 
	 * @param n1
	 *            左参数
	 * @param n2
	 *            右参数
	 * @return 结果
	 */
	public Number multiply(Number n1, Number n2) {
		if (isType(Double.class, n1, n2)) {
		} else if (isType(Float.class, n1, n2)) {
			return n1.floatValue() * n2.floatValue();
		} else if (isType(Long.class, n1, n2)) {
			return n1.longValue() * n2.longValue();
		} else if (isType(Integer.class, n1, n2) || isType(Short.class, n1, n2)
				|| isType(Byte.class, n1, n2)) {
			return n1.intValue() * n2.intValue();
		}
		return n1.doubleValue() * n2.doubleValue();
	}

	/**
	 * 除法运算
	 * 
	 * @param n1
	 *            左参数
	 * @param n2
	 *            右参数
	 * @return 结果
	 */
	public Number divide(Number n1, Number n2, boolean exact) {
		if (isType(Double.class, n1, n2)) {
		} else if (isType(Float.class, n1, n2)) {
			return n1.floatValue() / n2.floatValue();
		} else {
			if (isType(Long.class, n1, n2)) {
				long right = n2.longValue();
				if (right == 0) {
					return n1.floatValue() / 0;
				}
				if (!exact || n1.longValue() % right == 0) {
					return n1.longValue() / right;
				}
			} else if (isType(Integer.class, n1, n2)
					|| isType(Short.class, n1, n2)
					|| isType(Byte.class, n1, n2)) {
				int right = n2.intValue();
				if (right == 0) {
					return n1.floatValue() / 0;
				}
				if (!exact || n1.intValue() % right == 0) {
					return n1.intValue() / right;
				}
			}
		}
		return n1.doubleValue() / n2.doubleValue();
	}

	/**
	 * 求模运算
	 * 
	 * @param n1
	 *            左参数
	 * @param n2
	 *            右参数
	 * @return 结果
	 */
	public Number modulus(Number n1, Number n2) {
		if (isType(Double.class, n1, n2)) {
		} else if (isType(Float.class, n1, n2)) {
			return n1.floatValue() % n2.floatValue();
		} else if (isType(Long.class, n1, n2)) {
			return n1.longValue() % n2.longValue();
		} else if (isType(Integer.class, n1, n2) || isType(Short.class, n1, n2)
				|| isType(Byte.class, n1, n2)) {
			return n1.intValue() % n2.intValue();
		}
		return n1.doubleValue() % n2.doubleValue();
	}

}
