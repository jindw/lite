package org.xidea.el;

public interface Expression{
	/**
	 * 根据传入的变量上下文（map 或者javabean），执行表达式
	 * @param context 变量表
	 * @return
	 * @see ExpressionImpl#evaluate(varMap)
	 */
	public Object evaluate(Object context);
	/**
	 * 根据传入的变量上下文（键值数组），执行表达式
	 * @param keyValue 键值对（两个参数代表一个键值对）
	 * @return
	 */
	public Object evaluate(Object... keyValue);
}