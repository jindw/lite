package org.xidea.el;

import java.util.List;
import java.util.Map;

import org.xidea.el.impl.ExpressionImpl;

public interface Expression{
	/**
	 * 根据传入的变量上下文，执行表达式
	 * @see ExpressionImpl#evaluate(Map)
	 * @param context 变量表
	 * @return
	 */
	public Object evaluate(Object context);
	/**
	 * 获取表达式涉及到的变量集合
	 * @return
	 */
	public List<String> getVars();
	/**
	 * 返回表达式的源代码(JSON)
	 * @return
	 */
	public String toString();
}