package org.xidea.el.impl;


/**
 * 数字加减乘除四则运算，主要处理类型混合运算，如：Integer + Double
 * 
 * @author liangfei0201@163.com
 * @author jindw
 */

class NumberArithmetic {

	private static final Class<?>[] NUMBER_CLASS = {Byte.class,Short.class,Integer.class,Long.class,Float.class,Double.class};
	private static final int getNumberType(Number n1, Number n2) {
		Class<?> c1 = n1.getClass();
		Class<?> c2 = n2.getClass();
		int i = NUMBER_CLASS.length;
		while(i-->0){
			Class<?> c = NUMBER_CLASS[i];
			if(c == c1 || c== c2){
				return i;
			}
		}
		return NUMBER_CLASS.length-1;//double default
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
		switch( getNumberType(n1,n2)){
		case 0://b
		case 1://s
		case 2://i
			int i = n1.intValue() - n2.intValue();
			return i == 0?0:(i>0?1:-1);
		case 3://l
			long l = n1.longValue() - n2.longValue();
			return l == 0?0:(l>0?1:-1);
		//case 4://f
		//case 5://d
		default:
			double d1 = n1.doubleValue(),d2= n2.doubleValue();
			if(Double.isNaN(d1) || Double.isNaN(d2)){
				return validReturn;
			}
			return Double.compare(d1,d2);
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
		switch( getNumberType(n1,n2)){
		case 0://b
		case 1://s
		case 2://i
			return n1.intValue() + n2.intValue();
		case 3://l
			return n1.longValue() + n2.longValue();
		case 4://f
			return n1.floatValue() + n2.floatValue();
		//case 5://d
		default:
			return n1.doubleValue() + n2.doubleValue();
		}
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
		switch( getNumberType(n1,n2)){
		case 0://b
		case 1://s
		case 2://i
			return n1.intValue() - n2.intValue();
		case 3://l
			return n1.longValue() - n2.longValue();
		case 4://f
			return n1.floatValue() - n2.floatValue();
		//case 5://d
		default:
			return n1.doubleValue() - n2.doubleValue();
		}
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
		switch( getNumberType(n1,n2)){
		case 0://b
		case 1://s
		case 2://i
			return n1.intValue() * n2.intValue();
		case 3://l
			return n1.longValue() * n2.longValue();
		case 4://f
			return n1.floatValue() * n2.floatValue();
		//case 5://d
		default:
			return n1.doubleValue() * n2.doubleValue();
		}
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
		switch( getNumberType(n1,n2)){
		case 0://b
		case 1://s
		case 2://i
			return n1.doubleValue() / n2.doubleValue();
		case 3://l
			return n1.doubleValue() / n2.doubleValue();
		case 4://f
			return n1.floatValue() / n2.floatValue();
		//case 5://d
		default:
			return n1.doubleValue() / n2.doubleValue();
		}
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
		switch( getNumberType(n1,n2)){
		case 0://b
		case 1://s
		case 2://i
			return n1.intValue() % n2.intValue();
		case 3://l
			return n1.longValue() % n2.longValue();
		case 4://f
			return n1.floatValue() % n2.floatValue();
		//case 5://d
		default:
			return n1.doubleValue() % n2.doubleValue();
		}
	}

}
