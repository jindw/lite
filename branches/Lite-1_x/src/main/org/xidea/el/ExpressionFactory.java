package org.xidea.el;

import org.xidea.el.impl.ExpressionFactoryImpl;

/**
 * @see ExpressionFactoryImpl
 */
public interface ExpressionFactory {
	/**
	 * 从中间代码或者直接的表达式文本解析成表达式对象
	 * @param el
	 * @return
	 */
	public Expression create(Object el);
	/**
	 * 将表达式解析成中间状态
	 * @param expression
	 * @return
	 */
	public Object parse(String expression);
}