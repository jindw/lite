package org.xidea.el.impl.test;

import org.xidea.el.ExpressionToken;

public class NumberTest {
	private  static final Class<?>[] NUMBER_CLASS = { Byte.class, Short.class,
			Integer.class, Long.class, Float.class, Double.class };

	private static final int getNumberType(Number n1, Number n2) {
		Class<?> c1 = n1.getClass();
		Class<?> c2 = n2.getClass();
		int i = NUMBER_CLASS.length;
		while (i-- > 0) {
			Class<?> c = NUMBER_CLASS[i];
			if (c == c1 || c == c2) {
				return i;
			}
		}
		return NUMBER_CLASS.length - 1;// double default
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
	private final  static boolean compare(Number n1, Number n2, int type) {
		int i = 0;
		switch (getNumberType(n1, n2)) {
		case 0:// b
		case 1:// s
		case 2:// i
			i = n1.intValue() - n2.intValue();
			break;
		case 3:// l
			long l = n1.longValue() - n2.longValue();
			i =( l == 0 ? 0 : (l > 0 ? 1 : -1));
			break;
			// case 4://f
			// case 5://d
		default:
			double d1 = n1.doubleValue(),
			d2 = n2.doubleValue();
			if (Double.isNaN(d1) || Double.isNaN(d2)) {
				return false;
			}
			i= Double.compare(d1, d2);
		}
		
		if(i == 0){
			return type ==ExpressionToken.OP_EQ ||type == ExpressionToken.OP_GTEQ || type== ExpressionToken.OP_LTEQ;
		}else{
			return (i>0) ^ (ExpressionToken.OP_GT == type || type== ExpressionToken.OP_GTEQ);
		}
	}
	//56282582
	//55618763
	//10435469
	//11140775
	//17487686


	public static void main(String[] args) {
		Integer arg1 = new Object().hashCode();
		int c = 1000 *1000;
		long n1 = System.nanoTime();
		int result ;
		while(c-->0){

			if (Double.isNaN(arg1) || Double.isNaN(c)) {
			}else{
				result = Double.compare(arg1.doubleValue(), c);
			}
			//result = compare(arg1, c, ExpressionToken.OP_EQ)?1:0;
		}
		long n2 = System.nanoTime();
		System.out.println(n2-n1);
	}

}
