package org.xidea.el;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xidea.el.operation.Calculater;
import org.xidea.el.operation.Invocable;
import org.xidea.el.operation.ReflectUtil;
import org.xidea.el.parser.ExpressionToken;
import org.xidea.el.parser.ExpressionTokenizer;

public class ExpressionImpl implements Expression {
	protected final Calculater calculater;
	protected final ExpressionToken[] expression;
	protected String source;

	private final Map<String, Invocable> globalMap;

	public ExpressionImpl(String el) {
		this(el, new ExpressionTokenizer(el).getTokens().getData(),
				ExpressionFactoryImpl.DEFAULT_CALCULATER,
				ExpressionFactoryImpl.DEFAULT_GLOBAL_MAP);
	}

	public ExpressionImpl(String source, ExpressionToken[] compiled,
			Calculater calculater, Map<String, Invocable> globalMap) {
		this.source = source;
		this.calculater = calculater;
		this.expression = compiled;
		this.globalMap = globalMap;
	}

	public Object evaluate(Object context) {
		if (context == null) {
			context = Collections.emptyMap();
		}
		ValueStack stack = new ValueStack();
		evaluate(stack, expression, context);
		return calculater.realValue(stack.pop());
	}

	public ExpressionResult evaluateAsResult(Object context) {
		if (context == null) {
			context = Collections.emptyMap();
		}
		ValueStack stack = new ValueStack();
		evaluate(stack, expression, context);
		Object result = stack.pop();
		if (result instanceof ExpressionResult) {
			return (ExpressionResult) result;
		}
		return wrapResult(calculater.realValue(result));
	}

	protected void evaluate(ValueStack stack, ExpressionToken[] tokens,
			Object context) {
		ExpressionToken item = null;
		int i = tokens.length;
		while (i-- > 0) {
			item = (ExpressionToken) tokens[i];
			int type = item.getType();
			if (type > 3) {
				Object arg2 = null;
				Object arg1 = null;
				int length = type & 3;
				if (length > 1) {
					arg2 = stack.pop();
					arg1 = stack.pop();
				} else {// if (length == 1) {
					arg1 = stack.pop();
				}
				Object result = calculater.compute(item, arg1, arg2);
				if (result instanceof ExpressionToken) {
					ExpressionToken lazyToken = (ExpressionToken) result;
					if (lazyToken.getType() == ExpressionToken.VALUE_LAZY) {
						evaluate(stack, (ExpressionToken[]) lazyToken
								.getParam(), context);
						return;
					}
				}
				stack.push(result);
			} else {
				stack.push(getTokenValue(context, item));
			}
		}
	}

	protected Object getTokenValue(Object context, ExpressionToken item) {
		switch (item.getType()) {
		case ExpressionToken.VALUE_NEW_LIST:
			return new ArrayList<Object>();
		case ExpressionToken.VALUE_NEW_MAP:
			return new LinkedHashMap<Object, Object>();
		case ExpressionToken.VALUE_VAR:
			String value = (String) item.getParam();
			if ("this".equals(value)) {
				return context;
			} else {
				return createVariable(context, value);
			}
		case ExpressionToken.VALUE_LAZY:
			return (item);
		case ExpressionToken.VALUE_CONSTANTS:
			return item.getParam();
		}
		throw new IllegalArgumentException();
	}

	protected Object createVariable(Object context, String key) {
		if (context instanceof Map) {
			java.util.HashMap<?, ?> contextMap = (java.util.HashMap<?, ?>) context;
			if (contextMap.get(key)==null && !contextMap.containsKey(key)) {
				return calculater.createRefrence(globalMap,key);
			}
		} else if (ReflectUtil.getType(context.getClass(), key) == null) {
			return calculater.createRefrence(globalMap,key);
		}
		return calculater.createRefrence(context,key);
	}

	protected ExpressionResult wrapResult(final Object realValue) {
		return new ExpressionResult() {
			public Class<? extends Object> getType() {
				return realValue.getClass();
			}

			public Object getValue() {
				return realValue;
			}

			public void setValue(Object value) {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public String toString() {
		return source;
	}
}
