package org.xidea.el.operation;

import java.util.Map;

public class PropertyValue {
	private Object base;
	private Object name;
	private Class<? extends Object> type;

	public PropertyValue(Object base, Object name) {
		this.base = base;
		this.name = name;
	}

	public void next(Object key) {
		if (base != null) {
			Object context2 = getValue();
			if (context2 == null) {
				type = getNextType(base.getClass(), name);
			} else {
				base = context2;
			}
		} else if (type != null) {
			type = getNextType(type, name);
		}
		name = key;
	}

	private Class<? extends Object> getNextType(Class<? extends Object> type2,
			Object name2) {
		return null;
	}

	public Object getBase() {
		return base;
	}

	public Object getValue() {
		return ReflectUtil.getValue(base, name);
	}

	public Invocable getInvocable(Map<String, Map<String, Invocable>> methodMap,Object[] args) {
		return createInvocable(methodMap,base,name.toString(),args);
	}

	static Invocable createInvocable(
			Map<String, Map<String, Invocable>> methodMap,
			final Object thisObject, final String name,Object[] args) {
		Map<String, Invocable> invocableMap = methodMap.get(name);
		Invocable invocable = null;
		if (invocableMap != null) {
			invocable = findInvocable(invocableMap, thisObject.getClass());
		}
		if (invocable == null) {
			for (java.lang.reflect.Method method : thisObject.getClass()
					.getMethods()) {
				if (method.getName().equals(name)
						&& method.getParameterTypes().length == args.length) {
					return createProxy(method);
				}
			}
		} else {
			return invocable;
		}
		return null;
	}

	static Invocable findInvocable(Map<String, Invocable> methodMap,
			Class<?> clazz) {
		Invocable invocation = methodMap.get(clazz.getName());
		if (invocation != null) {
			return invocation;
		} else {
			Class<?>[] interfaces = clazz.getInterfaces();
			for (Class<?> clazz2 : interfaces) {
				invocation = findInvocable(methodMap, clazz2);
				if (invocation != null) {
					return invocation;
				}
			}
		}
		Class<?> clazz2 = clazz.getSuperclass();
		if (clazz2 != clazz) {
			return findInvocable(methodMap, clazz2);
		}
		return null;
	}

	static Invocable createProxy(final java.lang.reflect.Method method) {
		return new Invocable() {
			public Object invoke(Object thiz, Object... args) throws Exception {
				return method.invoke(thiz, args);
			}
		};
	}
}