package org.xidea.el.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ValueStackImpl implements Map<String, Object> {
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
						|| ReflectUtil.getPropertyClass(clazz, key) != null) {
					return result;
				}
				if (key instanceof String) {
					return ExpressionFactoryImpl.getInvocable(clazz,
							(String) key, -1);
				}
			}
		}
		return fallback(key);
	}

	protected Object fallback(Object key) {
		return null;
	}

	public Object put(String key, Object value) {
		put(key, value, -1);
		return null;
	}

	public void put(Object key, Object value, int level) {
		if (level < 0) {
			level = level + stack.length;
		}
		ReflectUtil.setValue(stack[level], key, value);
	}

	public void clear() {
	}

	public boolean containsKey(Object key) {
		return false;
	}

	public boolean containsValue(Object value) {
		return false;
	}

	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return null;
	}

	public boolean isEmpty() {
		return false;
	}

	public Set<String> keySet() {
		return null;
	}

	public void putAll(Map<? extends String, ? extends Object> m) {
	}

	public Object remove(Object key) {
		return null;
	}

	public int size() {
		return 0;
	}

	public Collection<Object> values() {
		return null;
	}

}
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
			} else if (ReflectUtil.getPropertyClass(context.getClass(), key) != null) {
				return new ReferenceImpl(context, key);
			}
		}
		return null;
	}
}
