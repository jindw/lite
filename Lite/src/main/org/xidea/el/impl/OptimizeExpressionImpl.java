package org.xidea.el.impl;

import java.util.ArrayList;

import org.xidea.el.ExpressionFactory;
import org.xidea.el.OperationStrategy;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionToken;
import org.xidea.el.ValueStack;

public class OptimizeExpressionImpl extends ExpressionImpl {

	protected Object name;

	public OptimizeExpressionImpl(ExpressionFactory factory,ExpressionToken expression,
			OperationStrategy calculater, Object name) {
		super(null, expression, calculater);
		this.name = name;
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
		return calculater.getVar(valueStack,name);
	}

	public static Expression create(ExpressionFactory factory,final ExpressionToken el,
			OperationStrategy calculater) {
		if (el.getType() == ExpressionToken.VALUE_VAR) {
			return new OptimizeExpressionImpl(factory,el, calculater, 
					el.getParam());
		}else if (el.getType() == ExpressionToken.OP_GET_STATIC_PROP) {
					ArrayList<Object> props = new ArrayList<Object>();
			ExpressionToken current = el;
			Object baseName = null;
			while(true) {
				if(current.getType() == ExpressionToken.OP_GET_STATIC_PROP){
					props.add(current.getParam());
				}else{
					if(current.getType() == ExpressionToken.VALUE_VAR){
						baseName = current.getParam();
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
				return new PropertyExpression(factory,el, calculater, 
						baseName,properties[0]);
			default:
				return new PropertiesExpression(factory,el, calculater, 
						baseName,properties);
			}

		}
		return null;
	}

	static class PropertyExpression extends OptimizeExpressionImpl {
		private Object key;
		public PropertyExpression(ExpressionFactory factory,ExpressionToken expression,
				OperationStrategy calculater, 
				Object name, Object key) {
			super(factory,expression, calculater, name);
			this.key = key;
		}
		protected Object compute(ValueStack valueStack) {
			Object base = calculater.getVar(valueStack,name);
			return ReflectUtil.getValue(base, key);
		}
	}

	static class PropertiesExpression extends PropertyExpression {
		private Object[] keys;
		public PropertiesExpression(ExpressionFactory factory,ExpressionToken expression,
				OperationStrategy calculater, 
				Object name, Object[] keys) {
			super(factory,expression, calculater, name,null);
			this.keys = keys;
		}
		protected Object compute(ValueStack valueStack) {
			Object base = calculater.getVar(valueStack,name);
			int i = keys.length;
			while(i-->0){
				base = ReflectUtil.getValue(base, keys[i]);
			}
			return base;
		}
	}

}
