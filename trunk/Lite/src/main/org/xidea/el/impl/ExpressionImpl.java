package org.xidea.el.impl;

import java.util.ArrayList;
import java.util.List;

import org.xidea.el.ExpressionInfo;
import org.xidea.el.OperationStrategy;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionToken;
import org.xidea.el.Reference;
import org.xidea.el.ReferenceExpression;
import org.xidea.el.ValueStack;

public class ExpressionImpl implements Expression, ReferenceExpression,
		ExpressionInfo {
	protected final OperationStrategy strategy;
	protected final ExpressionToken expression;
	static ValueStack EMPTY_VS = new ValueStack() {
		public void put(Object key, Object value) {
		}

		public Object get(Object key) {
			return null;
		}
	};

	public ExpressionImpl(String el) {
		ExpressionFactoryImpl efi = ExpressionFactoryImpl.getInstance();
		this.expression = (ExpressionToken) efi.parse(el);
		this.strategy = efi.strategy;
	}

	public ExpressionImpl(ExpressionToken expression,
			OperationStrategy strategy) {
		this.strategy = strategy;
		this.expression = expression;
	}

	public Object evaluate(Object context) {
		ValueStack valueStack;
		if (context instanceof ValueStack) {
			valueStack = (ValueStack) context;
		} else if (context == null) {
			valueStack = EMPTY_VS;
		} else {
			valueStack = new ValueStackImpl(context);
		}
		Object result = strategy.evaluate(expression, valueStack);
		return result;
	}

	public Reference prepare(Object context) {
		ValueStack valueStack;
		if (context == null) {
			valueStack = EMPTY_VS;
		} else if (context instanceof ValueStack) {
			valueStack = (ValueStack) context;
		} else {
			valueStack = new RefrenceStackImpl(context);
		}
		Object result = this.prepare(expression, valueStack);
		if (result instanceof Reference) {
			return (Reference) result;
		} else {
			return RefrenceStackImpl.wrapResult(result);
		}
	}

	protected Object prepare(ExpressionToken item, ValueStack vs) {
		int type = item.getType();
		Object arg2 ;
		if (type == TokenImpl.OP_GET_STATIC_PROP) {
			arg2 = item.getParam();
		} else if (type == ExpressionToken.OP_GET) {
			arg2 = strategy.evaluate(item.getRight(), vs);
		}else{
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

	static void appendVar(ExpressionToken el, List<String> list) {
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
