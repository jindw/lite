package org.xidea.el.impl;

import java.util.Map;

import org.xidea.el.ValueStack;

public class ValueStackImpl implements ValueStack {
	protected Object[] stack;

	public ValueStackImpl(Object... stack) {
		this.stack = stack;
	}

	public Object get(Object key) {
		int i = stack.length;
		while (i-- > 0) {
			Object context = stack[i];
			if (context instanceof Map<?, ?>) {
				Map<?, ?> contextMap = (Map<?, ?>) context;
				Object result = contextMap.get(key);
				if (result != null || contextMap.containsKey(key)) {
					return result;
				}
			} else if (context != null) {
				Object result = ReflectUtil.getValue(context, key);
				Class<?> clazz = context.getClass();
				if (result != null
						|| ReflectUtil.getPropertyType(clazz, key) != null) {
					return result;
				}
				if (key instanceof String) {
					return ReferenceImpl.getInvocable(clazz,
							(String) key, -1);
				}
			}
		}
		return fallback(key);
	}

	protected Object fallback(Object key) {
		return null;
	}

	public void put(Object key, Object value) {
		put(key, value, -1);
	}

	public void put(Object key, Object value, int level) {
		if (level < 0) {
			level = level + stack.length;
		}
		ReflectUtil.setValue(stack[level], key, value);
	}

}
//class ThisWrapper implements Invocable {
//	Invocable base;
//	Object thiz;
//	public Object invoke(Object thiz2, Object... args)
//			throws Exception {
//		return base.invoke(thiz, args);
//	}
//};
class RefrenceStackImpl extends ValueStackImpl {
	public RefrenceStackImpl(Object... context) {
		super(context);
	}

	public Object get(Object key) {
		int i = stack.length;
		while (i-- > 0) {
			Object context = stack[i];
			if (context instanceof Map<?,?>) {
				return new ReferenceImpl(context, key);
			} else if (ReflectUtil.getPropertyType(context.getClass(), key) != null) {
				return new ReferenceImpl(context, key);
			}
		}
		return null;
	}
}
