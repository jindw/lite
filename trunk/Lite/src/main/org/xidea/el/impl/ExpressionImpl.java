package org.xidea.el.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xidea.el.Calculater;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionToken;
import org.xidea.el.Reference;
import org.xidea.el.ReferenceExpression;
import org.xidea.el.ResultStack;
import org.xidea.el.ValueStack;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.parser.ExpressionTokenizer;

public class ExpressionImpl implements Expression ,ReferenceExpression {
	protected final Calculater calculater;
	protected final ExpressionToken[] expression;
	protected final String source;
	private final Map<String, Object> globalMap;


	public ExpressionImpl(String el) {
		this(el, new ExpressionTokenizer(el).getTokens().getData(),
				ExpressionFactoryImpl.DEFAULT_CALCULATER,ExpressionFactoryImpl.DEFAULT_GLOBAL_MAP);
	}

	public ExpressionImpl(String source, ExpressionToken[] expression,
			Calculater calculater, Map<String, Object> globalMap) {
		this.source = source;
		this.calculater = calculater;
		this.expression = expression;
		this.globalMap = globalMap;
	}

	public Object evaluate(Object context) {
		ValueStack valueStack;
		if (context == null) {
			valueStack = new ValueStackImpl(globalMap);
		}else if(context instanceof ValueStack){
			valueStack = (ValueStack)context;
		}else{
			valueStack = new ValueStackImpl(globalMap,context);
		}
		ResultStack stack = new ResultStackImpl();
		evaluate(stack, expression, valueStack);
		return realValue(stack.pop());
	}

	public Reference prepare(Object context) {

		ValueStack valueStack;
		if (context == null) {
			valueStack = new ValueStackImpl(globalMap);
		}else if(context instanceof ValueStack){
			valueStack = (ValueStack)context;
		}else{
			valueStack = new RefrenceStackImpl(globalMap,context);
		}
		ResultStack stack = new ResultStackImpl();
		evaluate(stack, expression, valueStack);
		Object result = stack.pop();
		if(result instanceof Reference){
			return (Reference)result;
		}else{
			return RefrenceStackImpl.wrapResult(result);
		}
	}
	protected void evaluate(ResultStack stack, ExpressionToken[] tokens,
			ValueStack context) {
		ExpressionToken item = null;
		int i = tokens.length;
		while (i-- > 0) {
			item = (ExpressionToken) tokens[i];
			int type = item.getType();
			if (type > 0) {
				Object result = calculater.compute(item, stack);
				if (result instanceof ExpressionToken) {
					ExpressionToken lazyToken = (ExpressionToken) result;
					if (lazyToken.getType() == ExpressionToken.VALUE_LAZY) {
						evaluate(stack, (ExpressionToken[]) lazyToken
								.getParam(), context);
						continue;
					}
				}
				stack.set(result);
			} else {
				stack.push(getValue(context, item));
			}
		}
	}

	protected Object getValue(ValueStack context, ExpressionToken item) {
		switch (item.getType()) {
		case ExpressionToken.VALUE_NEW_LIST:
			return new ArrayList<Object>();
		case ExpressionToken.VALUE_NEW_MAP:
			return new LinkedHashMap<Object, Object>();
		case ExpressionToken.VALUE_VAR:
			String value = (String) item.getParam();
			return context.get(value);
		case ExpressionToken.VALUE_LAZY:
			return (item);
		case ExpressionToken.VALUE_CONSTANTS:
			return item.getParam();
		}
		throw new IllegalArgumentException("unknow token:"+Integer.toBinaryString(item.getType()));
	}
	static Object realValue(Object result) {
		if (result instanceof Reference) {
			return ((Reference) result).getValue();
		}
		return result;
	}

	@Override
	public String toString() {
		return source == null?JSONEncoder.encode(expression):source;
	}

}
