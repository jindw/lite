package org.xidea.el.impl;

import java.util.Collections;
import java.util.List;

import org.xidea.el.OperationStrategy;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionToken;
import org.xidea.el.fn.ECMA262Impl;

public class ExpressionFactoryImpl implements ExpressionFactory {
	public static final OperationStrategy DEFAULT_CALCULATER;
	static {
		DEFAULT_CALCULATER = new OperationStrategyImpl();
		ECMA262Impl.setup((OperationStrategyImpl) DEFAULT_CALCULATER);
	}
	private static ExpressionFactoryImpl expressionFactory = new ExpressionFactoryImpl();

	public static ExpressionFactory getInstance() {
		return expressionFactory;
	}

	public OperationStrategy strategy = DEFAULT_CALCULATER;
	
	public ExpressionFactoryImpl(OperationStrategy strategy) {
		this.strategy = strategy;
	}

	public ExpressionFactoryImpl() {
	}


	public Object parse(String el) {
		@SuppressWarnings("unchecked")
		ExpressionToken tokens = new ExpressionTokenizer(el, Collections.EMPTY_MAP)
				.getResult();
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
		Expression ressult = OptimizeExpressionImpl.create(this, el,
				DEFAULT_CALCULATER);
		return ressult != null ? ressult : new ExpressionImpl(null, el,
				DEFAULT_CALCULATER);
	}
}
