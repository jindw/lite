package org.xidea.el;


public interface ExpressionFactory {
	public Expression createEL(Object el);
	public Object optimizeEL(String expression);
}