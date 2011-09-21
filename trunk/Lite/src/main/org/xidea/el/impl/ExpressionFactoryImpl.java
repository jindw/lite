package org.xidea.el.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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

public class ExpressionFactoryImpl implements ExpressionFactory {
	private static ExpressionFactoryImpl expressionFactory;
	private static ValueStackImpl EMPTY_VS = new ValueStackImpl(Collections
			.emptyMap());
	protected final OperationStrategy strategy;
	protected Map<String, Integer> aliseMap = new HashMap<String, Integer>();
	private int inc = 1;
	private boolean optimize = true;
	private static Map<String, Invocable> cachedInvocableMap = new HashMap<String, Invocable>();

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

	private OperationStrategyImpl getImpl() {
		if (strategy instanceof OperationStrategyImpl) {
			return (OperationStrategyImpl) strategy;
		}
		throw new UnsupportedOperationException();
	}

	public void addVar(String var, Object value) {
		getImpl().addVar(var, value);
	}

	public void addMethod(Class<? extends Object> clazz, String name,
			Invocable invocable) {
		getImpl().addMethod(clazz, name, invocable);
	}

	private Invocable toInvocable(Object impl) {
		if (impl instanceof Method) {
			Method method = (Method) impl;
			if (Modifier.isPublic(method.getModifiers())
					&& Modifier.isStatic(method.getModifiers())) {
				impl = createProxy(method);
			}
		}
		if (impl instanceof Invocable) {
			return (Invocable) impl;
		}
		throw new IllegalArgumentException(
				"支持public static 格式的函数或者org.xidea.el.Invocable 对象");
	}

	public void addOperator(int sampleToken, String name, Object impl) {
		if (impl == null) {
			this.aliseMap.put(name, sampleToken);
		} else {
			sampleToken += (inc++ << ExpressionToken.POS_INC);
			this.aliseMap.put(name, sampleToken);
			getImpl().addVar(sampleToken, toInvocable(impl));
		}
	}

	@SuppressWarnings("unchecked")
	public Object parse(String el) {
		ExpressionParser ep = new ExpressionParser(el);
		ep.setAliasMap(aliseMap);
		ExpressionToken tokens = ep.parseEL();
		if (optimize) {
			tokens = ((TokenImpl) tokens).optimize(strategy, Collections.EMPTY_MAP);
		}
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
			return createExpression(el);

		}
	}

	protected Expression createExpression(ExpressionToken el) {
		if (optimize) {
			Expression ressult = OptimizeExpressionImpl.create(el, this,
					strategy);
			if (ressult != null) {
				return ressult;
			}
		}
		return new ExpressionImpl(el, this, strategy);
	}

	public static Invocable createProxy(final Method... methods) {
		for (Method method : methods) {
			try {
				method.setAccessible(true);
			} catch (Exception e) {
			}
		}
		MethodInvocable inv = new MethodInvocable();
		inv.methods = methods;
		return inv;
	}

	static Invocable getInvocable(final Class<? extends Object> clazz,
			final String name, int length) {
		String key = clazz.getName() + '.' + length + name;
		Invocable result = cachedInvocableMap.get(key);
		if (result == null && !cachedInvocableMap.containsKey(key)) {
			ArrayList<Method> methods = new ArrayList<Method>();
			for (Method method : clazz.getMethods()) {
				if (method.getName().equals(name)
						&& (length < 0 || method.getParameterTypes().length == length)) {
					methods.add(method);
				}
			}
			if (methods.size() > 0) {
				result = createProxy(methods
						.toArray(new Method[methods.size()]));
				cachedInvocableMap.put(key, result);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T wrapAsContext(Object context) {
		Map<String, Object> valueStack;
		if (context instanceof Map) {
			valueStack = (Map<String, Object>) context;
		} else if (context == null) {
			valueStack = EMPTY_VS;
		} else {
			valueStack = new ValueStackImpl(context);
		}
		return (T) valueStack;
	}
}

class MethodInvocable implements Invocable {
	Method[] methods;

	public Object invoke(Object thiz, Object... args) throws Exception {
		nextMethod: for (Method method : methods) {
			Class<? extends Object> clazzs[] = method.getParameterTypes();
			if (clazzs.length == args.length) {
				for (int i = 0; i < clazzs.length; i++) {
					Class<? extends Object> type = ReflectUtil
							.toWrapper(clazzs[i]);
					Object value = args[i];
					value = ECMA262Impl.ToValue(value, type);
					args[i] = value;
					if (value != null) {
						if (!type.isInstance(value)) {
							continue nextMethod;
						}
					}
				}
			}
			return method.invoke(thiz, args);
		}
		return null;
	}
}
