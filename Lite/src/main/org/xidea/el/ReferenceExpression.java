package org.xidea.el;

import java.util.Map;

public interface ReferenceExpression{
	/**
	 * 根据传入的变量上下文，执行表达式
	 * @see ExpressionImpl#prepare(Map)
	 * @param context 变量表
	 * @return
	 */
	public Reference prepare(Object context);
	/**
	 * 返回表达式的源代码
	 * @return
	 */
	public String toString();
}