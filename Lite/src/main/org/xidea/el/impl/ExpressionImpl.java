package org.xidea.el.impl;

import java.util.Map;

import org.xidea.el.OperationStrategy;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionToken;
import org.xidea.el.Reference;
import org.xidea.el.ReferenceExpression;
import org.xidea.el.ValueStack;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.parser.ExpressionTokenizer;

public class ExpressionImpl implements Expression ,ReferenceExpression {
	protected final OperationStrategy calculater;
	protected final ExpressionToken expression;
	protected final String source;
	protected final Map<String, Object> globalMap;


	public ExpressionImpl(String el) {
		this(el, new ExpressionTokenizer(el).getResult(),
				ExpressionFactoryImpl.DEFAULT_CALCULATER,ExpressionFactoryImpl.DEFAULT_GLOBAL_MAP);
	}

	public ExpressionImpl(String source, ExpressionToken expression,
			OperationStrategy calculater, Map<String, Object> globalMap) {
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

		Object result = calculater.evaluate(expression,valueStack);
		if (result instanceof Reference) {
			return ((Reference) result).getValue();
		}
		return result;
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
		Object result = calculater.evaluate(expression,valueStack);
		if(result instanceof Reference){
			return (Reference)result;
		}else{
			return RefrenceStackImpl.wrapResult(result);
		}
	}

	@Override
	public String toString() {
		return source == null?JSONEncoder.encode(expression):source;
	}

}
