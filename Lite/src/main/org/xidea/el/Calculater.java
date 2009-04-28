package org.xidea.el;


import org.xidea.el.parser.ExpressionToken;



/**
 * 做2值之间的计算。
 * 三元运算符，需要转化为二元表示
 * 一元何零元运算符，null自动补全
 * @author jindw
 */
public interface Calculater {

	/**
	 * @param context 运算变量表
	 * @param op 操作符对象
	 * @param arg1 参数1
	 * @param arg2 参数2
	 * @return 运算结果
	 */
	public Object compute(ExpressionToken op,ResultStack stack) ;
}
