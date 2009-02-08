package org.xidea.el;

/**
 * @see ExpressionFactoryImpl
 */
public interface ExpressionFactory {
	public Expression createEL(Object el);
	public Object optimizeEL(String expression);
}