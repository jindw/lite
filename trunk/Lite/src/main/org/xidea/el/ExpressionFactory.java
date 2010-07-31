package org.xidea.el;

import org.xidea.el.impl.ExpressionFactoryImpl;

/**
 * @see ExpressionFactoryImpl
 */
public abstract class ExpressionFactory {
	public static ExpressionFactory getInstance() {
		return ExpressionFactoryImpl.getInstance();
	}
	/**
	 * 从中间代码或者直接的表达式文本解析成表达式对象
	 * @param el
	 * @return
	 */
	public abstract Expression create(Object el);
	/**
	 * 将表达式解析成中间状态
	 * @param expression
	 * @return
	 */
	public abstract Object parse(String expression);
	
//	public String stringify(Object el);
}