package org.xidea.el;

import java.util.List;

public interface ExpressionInfo{
	/**
	 * 返回表达式涉及到的变量名集合
	 * @return
	 */
	public List<String> getVars();
	/**
	 * 返回表达式的源代码(JSON)
	 * @return
	 */
	public String toString();
}