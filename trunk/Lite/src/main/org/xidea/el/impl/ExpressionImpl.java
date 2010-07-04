package org.xidea.el.impl;

import java.util.ArrayList;
import java.util.List;

import org.xidea.el.OperationStrategy;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionToken;
import org.xidea.el.Reference;
import org.xidea.el.ReferenceExpression;
import org.xidea.el.ValueStack;
import org.xidea.el.json.JSONEncoder;

public class ExpressionImpl implements Expression, ReferenceExpression {
	protected final OperationStrategy calculater;
	protected final ExpressionToken expression;
	protected final String source;

	public ExpressionImpl(String el) {
		this(el, (ExpressionToken) ExpressionFactoryImpl.getInstance()
				.parse(el), ExpressionFactoryImpl.DEFAULT_CALCULATER);
	}

	public ExpressionImpl(String source, ExpressionToken expression,
			OperationStrategy calculater) {
		this.source = source;
		this.calculater = calculater;
		this.expression = expression;
	}

	public Object evaluate(Object context) {
		ValueStack valueStack;
		if (context instanceof ValueStack) {
			valueStack = (ValueStack) context;
		} else if (context == null) {
			valueStack = new ValueStackImpl();
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
			valueStack = new ValueStackImpl();
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
		if (source == null) {
			// TODO:jsel stringify
			return JSONEncoder.encode(expression);
		} else {
			return source;
		}
	}

	public List<String> getVars() {
		ArrayList<String> list = new ArrayList<String>();
		append(list, expression);
		return list;
	}

	private void append(ArrayList<String> list, ExpressionToken el) {
		if (el != null) {
			int type = el.getType();
			if (type > 0) {
				append(list, el.getRight());
				append(list, el.getLeft());
			} else if (type == ExpressionToken.VALUE_CONSTANTS) {
				list.add((String) el.getParam());
			}
		}
	}

}
