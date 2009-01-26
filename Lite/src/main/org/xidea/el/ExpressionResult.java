package org.xidea.el;



public interface ExpressionResult{
	/**
	 * 根据传入的变量上下文,设置表达式对应的属性值
	 * @see ExpressionResult#setValue(Object)
	 * @param context 变量表
	 * @return
	 */
	public void setValue(Object value);
	public Object getValue();
	public Class<? extends Object> getType();
}