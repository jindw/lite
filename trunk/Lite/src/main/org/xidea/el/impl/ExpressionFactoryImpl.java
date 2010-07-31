package org.xidea.el.impl;

import java.util.Collections;
import java.util.List;

import org.xidea.el.Invocable;
import org.xidea.el.OperationStrategy;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionToken;
import org.xidea.el.fn.ECMA262Impl;

public class ExpressionFactoryImpl extends ExpressionFactory {

	final OperationStrategy strategy;
	static ExpressionFactoryImpl expressionFactory;
	public static ExpressionFactoryImpl getInstance() {
		if(expressionFactory == null){
			expressionFactory = new ExpressionFactoryImpl();
		}
		return expressionFactory;
	}
	public ExpressionFactoryImpl(OperationStrategy strategy) {
		this.strategy = strategy;
	}

	public ExpressionFactoryImpl() {
		OperationStrategyImpl strategy = new OperationStrategyImpl();
		ECMA262Impl.setup(strategy);
		this.strategy = strategy;
	}

	public void addVar(String var, Object value) {
		((OperationStrategyImpl)this.strategy).addVar(var, value);
	}
	public void addMethod(Class<? extends Object> clazz, String name, Invocable invocable) {
		((OperationStrategyImpl)this.strategy).addMethod(clazz, name, invocable);
	}
	public void addOperator(int type,Invocable impl){
		
	}
	public void addOperator(int type,int higher,Invocable impl){
		
	}
	public Object parse(String el) {
		@SuppressWarnings("unchecked")
		ExpressionToken tokens = new ExpressionTokenizer(el, Collections.EMPTY_MAP)
				.getResult();

		tokens = ((TokenImpl)tokens).optimize(strategy);
		return tokens;
	}



	@SuppressWarnings("unchecked")
	public Expression create(Object elo) {
		if (elo instanceof String) {
			return create(parse((String) elo));
		} else {
			ExpressionToken el;
			if (elo instanceof ExpressionToken) {
				el = (ExpressionToken) elo;
			} else {
				el = TokenImpl.toToken((List<Object>) elo);
			}
			return getOptimizedExpression(el);

		}
	}

	private Expression getOptimizedExpression(ExpressionToken el) {
		Expression ressult = OptimizeExpressionImpl.create(el,
				strategy);
		return ressult != null ? ressult : new ExpressionImpl(el,
				strategy);
	}
}
