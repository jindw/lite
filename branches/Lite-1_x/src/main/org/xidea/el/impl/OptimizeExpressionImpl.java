package org.xidea.el.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.xidea.el.Calculater;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionToken;
import org.xidea.el.ValueStack;

public class OptimizeExpressionImpl extends ExpressionImpl {

	protected Object name;

	public OptimizeExpressionImpl(ExpressionToken[] expression,
			Calculater calculater, Map<String, Object> globalMap, Object name) {
		super(null, expression, calculater, globalMap);
		this.name = name;
	}

	@Override
	public Object evaluate(Object context) {
		ValueStack valueStack;
		if (context == null) {
			valueStack = new ValueStackImpl(globalMap);
		} else if (context instanceof ValueStack) {
			valueStack = (ValueStack) context;
		} else {
			valueStack = new ValueStackImpl(globalMap, context);
		}
		return compute(valueStack);
	}

	protected Object compute(ValueStack valueStack) {
		return valueStack.get(name);
	}

	public static Expression create(final ExpressionToken[] el,
			Calculater calculater, Map<String, Object> globals) {
		if (el[0].getType() == ExpressionToken.VALUE_VAR) {
			ArrayList<Object> props = new ArrayList<Object>();
			for (int i = 1; i < el.length; i++) {
				if (el[i].getType() != ExpressionToken.OP_STATIC_GET_PROP) {
					return null;
				}
				props.add(el[i].getParam());
			}
			Object baseName = el[0].getParam();
			Collections.reverse(props);
			final Object[] properties = props.toArray();
			switch (properties.length) {
			case 0:
				return new OptimizeExpressionImpl(el, calculater, globals,
						baseName);
			case 1:
				return new PropertyExpression(el, calculater, globals,
						baseName,properties[0]);
			default:
				return new PropertiesExpression(el, calculater, globals,
						baseName,properties);
			}

		}
		return null;
	}

	static class PropertyExpression extends OptimizeExpressionImpl {
		private Object key;
		public PropertyExpression(ExpressionToken[] expression,
				Calculater calculater, Map<String, Object> globalMap,
				Object name, Object key) {
			super(expression, calculater, globalMap, name);
			this.key = key;
		}
		protected Object compute(ValueStack valueStack) {
			Object base = valueStack.get(name);
			return ReflectUtil.getValue(base, key);
		}
	}

	static class PropertiesExpression extends PropertyExpression {
		private Object[] keys;
		public PropertiesExpression(ExpressionToken[] expression,
				Calculater calculater, Map<String, Object> globalMap,
				Object name, Object[] keys) {
			super(expression, calculater, globalMap, name,null);
			this.keys = keys;
		}
		protected Object compute(ValueStack valueStack) {
			Object base = valueStack.get(name);
			int i = keys.length;
			while(i-->0){
				base = ReflectUtil.getValue(base, keys[i]);
			}
			return base;
		}
	}

}
