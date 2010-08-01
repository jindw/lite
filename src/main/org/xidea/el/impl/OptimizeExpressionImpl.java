package org.xidea.el.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xidea.el.OperationStrategy;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionToken;
import org.xidea.el.ValueStack;

public class OptimizeExpressionImpl extends ExpressionImpl {
	protected String name;

	public OptimizeExpressionImpl(ExpressionToken expression,
			OperationStrategy calculater, String name) {
		super(expression, calculater);
		this.name = name;
	}

	@Override
	public List<String> getVars() {
		return Arrays.asList(name);
	}

	@Override
	public Object evaluate(Object context) {
		ValueStack valueStack;
		if (context == null) {
			valueStack = new ValueStackImpl();
		} else if (context instanceof ValueStack) {
			valueStack = (ValueStack) context;
		} else {
			valueStack = new ValueStackImpl(context);
		}
		return compute(valueStack);
	}

	protected Object compute(ValueStack valueStack) {
		return strategy.getVar(valueStack,name);
	}
	public static Expression create(final ExpressionToken el,
			OperationStrategy calculater) {
		if (el.getType() == ExpressionToken.VALUE_VAR) {
			return new OptimizeExpressionImpl(el, calculater, 
					(String)el.getParam());
		}else if (el.getType() == TokenImpl.OP_GET_STATIC) {
					ArrayList<Object> props = new ArrayList<Object>();
			ExpressionToken current = el;
			String baseName = null;
			while(true) {
				if(current.getType() == TokenImpl.OP_GET_STATIC){
					props.add(current.getParam());
				}else{
					if(current.getType() == ExpressionToken.VALUE_VAR){
						baseName = (String)current.getParam();
						break;
					}else{
						return null;
					}
				}
				current = current.getLeft();
			}
			final Object[] properties = props.toArray();
			switch (properties.length) {
			case 1:
				return new PropertyExpression(el, calculater, 
						baseName,properties[0]);
			default:
				return new PropertiesExpression(el, calculater, 
						baseName,properties);
			}

		}
		return null;
	}

	static class PropertyExpression extends OptimizeExpressionImpl {
		private Object key;
		public PropertyExpression(ExpressionToken expression,
				OperationStrategy calculater, 
				String name, Object key) {
			super(expression, calculater, name);
			this.key = key;
		}
		protected Object compute(ValueStack valueStack) {
			Object base = strategy.getVar(valueStack,name);
			return ReflectUtil.getValue(base, key);
		}
	}

	static class PropertiesExpression extends PropertyExpression {
		private Object[] keys;
		public PropertiesExpression(ExpressionToken expression,
				OperationStrategy calculater, 
				String name, Object[] keys) {
			super(expression, calculater, name,null);
			this.keys = keys;
		}
		protected Object compute(ValueStack valueStack) {
			Object base = strategy.getVar(valueStack,name);
			int i = keys.length;
			while(i-->0){
				base = ReflectUtil.getValue(base, keys[i]);
			}
			return base;
		}
	}

}
