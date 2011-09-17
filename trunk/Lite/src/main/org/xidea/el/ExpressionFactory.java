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
	public abstract Expression create(Object el);
	/**
	 * 将表达式解析成中间状态
	 * @param expression
	 * @return
	 */
	public abstract Object parse(String expression);
	/**
	 * 添加表达式引擎内置变量
	 * @param name
	 * @param value
	 */
	public abstract void addVar(String name, Object value);
	/**
	 * 从对象(Map/JavaBean)构造一个表达式上下文
	 * @param <T>
	 * @param context
	 * @return
	 */
	public abstract <T>T wrapAsContext(Object context);
}