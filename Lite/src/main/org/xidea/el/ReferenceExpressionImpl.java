package org.xidea.el;

import java.util.Collections;
import java.util.Map;

import org.xidea.el.operation.Calculater;
import org.xidea.el.operation.ProxyMap;
import org.xidea.el.operation.ReflectUtil;
import org.xidea.el.parser.ExpressionToken;

class ReferenceExpressionImpl extends ExpressionImpl {

	public ReferenceExpressionImpl(String source, ExpressionToken[] expression,
			Calculater calculater) {
		super(source, expression, calculater);
	}
	public Reference prepare(Object context) {
		if (context == null) {
			context = Collections.emptyMap();
		}
		ResultStack stack = new ResultStack();
		evaluate(stack, expression, context);
		Object result = stack.pop();
		if (result instanceof Reference) {
			return (Reference) result;
		}
		return wrapResult(calculater.realValue(result));
	}

	protected Object createVariable(Object context, String key) {
		if (context instanceof Map) {
			Map<?, ?> contextMap = (Map<?, ?>) context;
			Object value ;
			if(context instanceof ProxyMap){
				value = ((ProxyMap)context).getPropertyValue(key);
			}else{
				value = contextMap.get(key);
			}
			if(value instanceof Reference){
				return value;
			}
			if (value== null && !contextMap.containsKey(key)) {
				//readonly
				//return calculater.createRefrence(globalMap, key);
				Object object = globalMap.get(key);
				if(object != null){
					return object;
				}
			}
		} else if (ReflectUtil.getType(context.getClass(), key) == null) {
			return globalMap.get(key);
		}
		return calculater.createRefrence(context, key);
	}

	protected Reference wrapResult(final Object realValue) {
		return new Reference() {
			public Class<? extends Object> getType() {
				return realValue == null? null:realValue.getClass();
			}

			public Object getValue() {
				return realValue;
			}

			public Object setValue(Object value) {
				throw new UnsupportedOperationException();
			}
			public Reference next(Object key) {
				throw new UnsupportedOperationException();
			}

			public Object getBase() {
				return null;
			}

			public Object getName() {
				return null;
			}
		};
	}
}
