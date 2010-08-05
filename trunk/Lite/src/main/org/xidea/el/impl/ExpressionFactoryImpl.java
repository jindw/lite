package org.xidea.el.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xidea.el.Invocable;
import org.xidea.el.OperationStrategy;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionToken;
import org.xidea.el.fn.ECMA262Impl;

public class ExpressionFactoryImpl extends ExpressionFactory {
	private static ExpressionFactoryImpl expressionFactory;
	private final OperationStrategy strategy;
	private Map<String, Integer> aliseMap = new HashMap<String, Integer>();
	private int inc = 1;

	public static ExpressionFactoryImpl getInstance() {
		if (expressionFactory == null) {
			expressionFactory = new ExpressionFactoryImpl();
			expressionFactory.aliseMap = Collections.emptyMap();
			expressionFactory.getImpl().customizable = false;
		}
		return expressionFactory;
	}

	public ExpressionFactoryImpl(OperationStrategy strategy) {
		this.strategy = strategy;
	}

	public OperationStrategy getStrategy() {
		return getImpl();
	}

	public ExpressionFactoryImpl() {
		OperationStrategyImpl strategy = new OperationStrategyImpl(true);
		this.strategy = strategy;
		ECMA262Impl.setup(this);
	}
	private OperationStrategyImpl getImpl(){
		if (strategy instanceof OperationStrategyImpl) {
			return (OperationStrategyImpl)strategy;
		}
		throw new UnsupportedOperationException();
	}
	public void addVar(String var, Object value) {
		getImpl().addVar(var, value);
	}

	public void addMethod(Class<? extends Object> clazz, String name,
			Invocable invocable) {
		getImpl().addMethod(clazz, name,invocable);
	}

	public void addOperator(int sampleToken, String name, Object impl) {
		if(impl == null){
			this.aliseMap.put(name, sampleToken);
		}else{
			if(impl instanceof Method){
				Method method = (Method)impl;
				if(Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())){
					impl = ReferenceImpl.createProxy(method);
				}
			}
			if(impl instanceof Invocable){
				sampleToken += (inc++ << ExpressionToken.POS_INC);
				this.aliseMap.put(name, sampleToken);
				getImpl().addVar(sampleToken, impl);
			}else{
				throw new IllegalArgumentException("操作符实现只支持public static 模式函数或者org.xidea.el.Invocable 对象");
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Object parse(String el) {
		ExpressionParser ep = new ExpressionParser(el);
		ep.setAliasMap(aliseMap);
		ExpressionToken tokens = ep.parseEL();
		tokens = ((TokenImpl) tokens).optimize(strategy, Collections.EMPTY_MAP);
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
		Expression ressult = OptimizeExpressionImpl.create(el, strategy);
		return ressult != null ? ressult : new ExpressionImpl(el, strategy);
	}
}
