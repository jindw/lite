package org.xidea.el.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionInfo;
import org.xidea.el.OperationStrategy;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionToken;
import org.xidea.el.Reference;
import org.xidea.el.ReferenceExpression;

public class ExpressionImpl implements Expression, ReferenceExpression,
		ExpressionInfo {
	protected final OperationStrategy strategy;
	protected final ExpressionToken expression;
	protected ExpressionFactory factory;

	public ExpressionImpl(String el) {
		ExpressionFactoryImpl efi = ExpressionFactoryImpl.getInstance();
		this.factory = efi;
		this.expression = (ExpressionToken) efi.parse(el);
		this.strategy = efi.getStrategy();
	}

	public ExpressionImpl(ExpressionToken expression, ExpressionFactory factory, OperationStrategy strategy) {
		this.factory = factory;
		this.strategy = strategy;
		this.expression = expression;
	}

	public Object evaluate(Object context) {
		Map<String, Object> contextMap = factory.wrapAsContext(context);
		Object result = strategy.evaluate(expression,contextMap);
		return result;
	}

	public Object evaluate(Object... context) {
		if (context == null || context.length == 0) {
			return evaluate((Object) null);
		} else if (context.length == 1) {
			return evaluate(context[0]);
		} else if ((context.length & 1) == 1) {
			throw new IllegalArgumentException("参数必须是偶数个数");
		}
		HashMap<Object, Object> map = new HashMap<Object, Object>();
		for (int i = 0; i < context.length; i++) {
			map.put(context[i], context[++i]);
		}
		// System.out.println(JSONEncoder.encode(map));
		return evaluate(map);
	}

	public Reference prepare(Object context) {
		Map<String, Object> valueStack = new RefrenceStackImpl(context);
		Object result = this.prepare(expression, valueStack);
		if (result instanceof Reference) {
			return (Reference) result;
		} else {
			return null;
		}
	}

	protected Object prepare(ExpressionToken item, Map<String, Object> vs) {
		int type = item.getType();
		Object arg2;
		if (type == TokenImpl.OP_GET_STATIC) {
			arg2 = item.getParam();
		} else if (type == ExpressionToken.OP_GET) {
			arg2 = strategy.evaluate(item.getRight(), vs);
		} else {
			return strategy.evaluate(item, vs);
		}
		Object arg1 = prepare(item.getLeft(), vs);
		if (arg1 instanceof Reference) {
			return ((Reference) arg1).next(arg2);
		} else {
			return new ReferenceImpl(arg1, arg2);
		}
	}

	@Override
	public String toString() {
		return expression.toString();
	}

	public List<String> getVars() {
		ArrayList<String> list = new ArrayList<String>();
		appendVar(expression, list);
		return list;
	}

	private static void appendVar(ExpressionToken el, List<String> list) {
		if (el != null) {
			int type = el.getType();
			if (type > 0) {
				appendVar(el.getRight(), list);
				appendVar(el.getLeft(), list);
			} else if (type == ExpressionToken.VALUE_VAR) {
				list.add((String) el.getParam());
			}
		}
	}

}
