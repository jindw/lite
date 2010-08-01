package org.xidea.el.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xidea.el.Invocable;
import org.xidea.el.Reference;
import org.xidea.el.fn.ECMA262Impl;

class ReferenceImpl implements Reference {
	private Object base;
	private Object name;
	private Class<? extends Object> type;
	private static Map<String, Invocable> cachedInvocableMap = new HashMap<String, Invocable>();

	public ReferenceImpl(Object base, Object name) {
		this.base = base;
		this.name = name;
	}

	public String toString() {
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
		} else  if(base !=null){
			Object value = getValue();
			if (value != null) {
				return value.getClass();
			} else {
				Class<? extends Object> t = ReflectUtil.getPropertyType(base
						.getClass(), name);
				return t == null ? Object.class : t;
			}
		}else{
			return null;
		}
	}

	public Object setValue(Object value) {
		ReflectUtil.setValue(base, name, value);
		return null;
	}

	static Invocable createInvocable(Reference ref,
			Map<String, Map<String, Invocable>> methodMap, Object[] args) {
		Invocable invocable = createInvocable(methodMap, ref.getBase(), ref
				.getName().toString(), args);
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static Invocable createInvocable(
			Map<String, Map<String, Invocable>> methodMap,
			final Object thisObject, final String name, Object[] args) {
		Map<String, Invocable> invocableMap = methodMap.get(name);
		Invocable invocable = null;
		if (invocableMap != null) {
			invocable = findInvocable(invocableMap, thisObject.getClass());
		}
		if (invocable == null) {
			invocable = getInvocable(thisObject.getClass(), name,
					args.length);

			if (invocable == null && thisObject instanceof Class) {
				invocable = getInvocable((Class) thisObject, name,
						args.length);
			}
		}
		return invocable;
	}

	static Invocable getInvocable(final Class<? extends Object> clazz, final String name,
			int length) {
		String key = clazz.getName() + '.' + length + name;
		Invocable result = cachedInvocableMap.get(key);
		if (result == null && !cachedInvocableMap.containsKey(key)) {
			ArrayList<Method> methods = new ArrayList<Method>();
			for (Method method : clazz.getMethods()) {
				if (method.getName().equals(name)
						&& (length <0 || method.getParameterTypes().length == length)) {
					methods.add(method);
				}
			}
			if(methods.size()>0){
				result = createProxy(methods.toArray(new Method[methods.size()]));
				cachedInvocableMap.put(key, result);
			}
		}
		return result;
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

	static Invocable createProxy(final Method... methods) {
		for (Method method : methods) {
			try{
				method.setAccessible(true);
			}catch (Exception e) {
			}
		}
		return new Invocable() {
			public Object invoke(Object thiz, Object... args) throws Exception {
				nextMethod: for (Method method : methods) {
					Class<? extends Object> clazzs[] = method
							.getParameterTypes();
					if (clazzs.length == args.length) {
						for (int i = 0; i < clazzs.length; i++) {
							Class<? extends Object> type = ReflectUtil.toWrapper(clazzs[i]);
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
		};
	}

	public Object getName() {
		return name;
	}

}