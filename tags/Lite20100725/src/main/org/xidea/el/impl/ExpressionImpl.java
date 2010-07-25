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

public class ExpressionImpl implements Expression, ReferenceExpression, ExpressionInfo {
	protected final OperationStrategy calculater;
	protected final ExpressionToken expression;
	static ValueStack EMPTY_VS  =new ValueStack() {
		public void put(Object key, Object value) {
		}
		public Object get(Object key) {
			return null;
		}
	};

	public ExpressionImpl(String el) {
		this((ExpressionToken) ExpressionFactoryImpl.getInstance()
				.parse(el), ExpressionFactoryImpl.DEFAULT_CALCULATER);
	}

	public ExpressionImpl(ExpressionToken expression,
			OperationStrategy calculater) {
		this.calculater = calculater;
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
		Object result = calculater.evaluate(expression, valueStack);
		if (result instanceof Reference) {
			return ((Reference) result).getValue();
		}
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
		Object result = calculater.evaluate(expression, valueStack);
		if (result instanceof Reference) {
			return (Reference) result;
		} else {
			return RefrenceStackImpl.wrapResult(result);
		}
	}

	@Override
	public String toString() {
		return expression.toString();
	}

	public List<String> getVars() {
		ArrayList<String> list = new ArrayList<String>();
		appendVar(expression,list);
		return list;
	}

	static void appendVar(ExpressionToken el,List<String> list) {
		if (el != null) {
			int type = el.getType();
			if (type > 0) {
				appendVar(el.getRight(),list);
				appendVar(el.getLeft(),list);
			} else if (type == ExpressionToken.VALUE_VAR) {
				list.add((String) el.getParam());
			}
		}
	}

}
