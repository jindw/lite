package org.xidea.el.impl;

import java.lang.reflect.Method;
import java.util.Map;

import org.xidea.el.Invocable;
import org.xidea.el.Reference;
import org.xidea.el.fn.ECMA262Impl;
import org.xidea.el.fn.NumberArithmetic;

class ReferenceImpl implements Reference {
	private Object base;
	private Object name;
	private Class<? extends Object> type;

	public ReferenceImpl(Object base, Object name) {
		this.base = base;
		this.name = name;
	}
	public String toString(){
		return String.valueOf(this.getValue());
	}

	public Reference next(Object key) {
		if (base != null) {
			Object context2 = getValue();
			if (context2 == null) {
				type = ReflectUtil.getPropertyType(base.getClass(), name);
				base = null;
			} else {
				base = context2;
			}
		} else if (type != null) {
			type = ReflectUtil.getPropertyType(type, name);
		}
		name = key;
		return this;
	}

	public Object getBase() {
		return base;
	}

	public Object getValue() {
		return ReflectUtil.getValue(base, name);
	}

	public Class<? extends Object> getType() {
		if (type != null) {
			return ReflectUtil.getPropertyType(type, name);
		} else {
			Object value = getValue();
			if (value != null) {
				return value.getClass();
			} else {
				Class<? extends Object> t = ReflectUtil.getPropertyType(base.getClass(), name);
				return t == null?Object.class:t;
			}
		}
	}

	public Object setValue(Object value) {
		ReflectUtil.setValue(base, name, value);
		return null;
	}

	static Invocable getInvocable(Reference ref,
			Map<String, Map<String, Invocable>> methodMap, Object[] args) {
		Invocable invocable = createInvocable(methodMap, ref.getBase(), ref.getName().toString(),
				args);
		if (invocable == null) {
			Object object = ref.getValue();
			if (object instanceof Invocable) {
				invocable = (Invocable) object;
			} else if (object instanceof Method) {
				return createProxy((Method) object);
			}
		}
		return invocable;
	}

	static Invocable createInvocable(
			Map<String, Map<String, Invocable>> methodMap,
			final Object thisObject, final String name, Object[] args) {
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
		if (clazz2 != clazz && clazz2 != null) {
			return findInvocable(methodMap, clazz2);
		}
		return null;
	}

	static Invocable createProxy(final java.lang.reflect.Method... methods) {
		return new Invocable() {
			public Object invoke(Object thiz, Object... args) throws Exception {
				nextMethod: for (java.lang.reflect.Method method : methods) {
					Class<? extends Object> clazzs[] = method
							.getParameterTypes();
					if (clazzs.length == args.length) {
						for (int i = 0; i < clazzs.length; i++) {
							Class<? extends Object> type = clazzs[i];
							Object value = args[i];
							if (Number.class.isAssignableFrom(type)) {
								args[i] = NumberArithmetic.getValue(type,
										ECMA262Impl.ToNumber(value));
							} else if (String.class.isAssignableFrom(type)) {
								args[i] = value == null ? null : String
										.valueOf(value);
							}
							if (value != null) {
								if (!type.isInstance(value)) {
									continue nextMethod;
								}
							}
						}
					}
					//TODO:....
					method.setAccessible(true);
					return method.invoke(thiz, args);
				}
				return null;
			}
		};
	}

	public Object getName() {
		return name;
	}

}