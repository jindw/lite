package org.xidea.el;

import java.util.Collections;
import java.util.Map;

import org.xidea.el.operation.Calculater;
import org.xidea.el.operation.Invocable;
import org.xidea.el.operation.ReflectUtil;
import org.xidea.el.parser.ExpressionToken;

public class PrepareExpressionImpl extends ExpressionImpl {

	public PrepareExpressionImpl(String source, ExpressionToken[] expression,
			Calculater calculater, Map<String, Invocable> globalMap) {
		super(source, expression, calculater, globalMap);
	}
	public ExpressionResult prepare(Object context) {
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

	protected Object createVariable(Object context, String key) {
		if (context instanceof Map) {
			Map<?, ?> contextMap = (Map<?, ?>) context;
			if (contextMap.get(key) == null && !contextMap.containsKey(key)) {
				//readonly
				//return calculater.createRefrence(globalMap, key);
				return globalMap.get(key);
			}
		} else if (ReflectUtil.getType(context.getClass(), key) == null) {
			//readonly
			//return calculater.createRefrence(globalMap, key);
			return globalMap.get(key);
		}
		return calculater.createRefrence(context, key);
	}

	protected ExpressionResult wrapResult(final Object realValue) {
		return new ExpressionResult() {
			public Class<? extends Object> getType() {
				return realValue.getClass();
			}

			public Object getValue() {
				return realValue;
			}

			public Object setValue(Object value) {
				throw new UnsupportedOperationException();
			}
		};
	}
}
