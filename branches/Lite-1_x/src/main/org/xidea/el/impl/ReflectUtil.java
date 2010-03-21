package org.xidea.el.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.fn.NumberArithmetic;

public abstract class ReflectUtil {
	private static final Log log = LogFactory.getLog(ReflectUtil.class);
	private static final String LENGTH = "length";
	private static final Map<Class<?>, Map<String, AccessDescriptor>> classPropertyMap = new WeakHashMap<Class<?>, Map<String, AccessDescriptor>>();

	private static class AccessDescriptor {
		private Method reader;
		private Method writer;
		private Class<? extends Object> type;
	}

	private static Map<String, AccessDescriptor> getPropertyMap(Class<?> clazz) {
		Map<String, AccessDescriptor> propertyMap = classPropertyMap.get(clazz);
		if (propertyMap == null) {
			try {
				propertyMap = new HashMap<String, AccessDescriptor>();
				if (!clazz.equals(Object.class)) {
					propertyMap.putAll(getPropertyMap(clazz.getSuperclass()));
				}
				Method[] methods = clazz.getDeclaredMethods();
				for (Method m : methods) {
					if ((m.getModifiers() & Modifier.PUBLIC) > 0) {
						initProperty(propertyMap, m);
					}
				}
				classPropertyMap.put(clazz, propertyMap);
			} catch (Exception e) {
			}
		}
		return propertyMap;
	}

	private static void initMethod(Map<String, AccessDescriptor> propertyMap,
			Method m, Class<? extends Object> type, boolean write, String name) {
		if (name.length() > 0) {
			char c = name.charAt(0);
			if (Character.isUpperCase(c)) {
				name = Character.toLowerCase(c) + name.substring(1);
				AccessDescriptor ad = propertyMap.get(name);
				if (ad == null) {
					ad = new AccessDescriptor();
					propertyMap.put(name, ad);
				}
				if (type != ad.type) {
					ad.reader = null;
					ad.writer = null;
					ad.type = type;
				}
				m.setAccessible(true);
				if (write) {
					ad.writer = m;
				} else {
					ad.reader = m;
				}
			}
		}

	}

	private static void initProperty(Map<String, AccessDescriptor> propertyMap,
			Method m) {
		String name = m.getName();
		Class<? extends Object> type = m.getReturnType();
		Class<? extends Object>[] params = m.getParameterTypes();
		if (type == Void.TYPE) {
			if (params.length == 1 && name.startsWith("set")) {
				type = params[0];
				initMethod(propertyMap, m, type, true, name.substring(3));
			}
		} else {
			if (params.length == 0) {
				if (name.startsWith("get")) {
					initMethod(propertyMap, m, type, false, name.substring(3));
				} else if (type == Boolean.TYPE && name.startsWith("is")) {
					initMethod(propertyMap, m, type, false, name.substring(2));
				}
			}
		}
	}

	public static Map<String, ? extends Object> map(final Object context) {
		Map<String, AccessDescriptor> ps = getPropertyMap(context.getClass());
		return new ProxyMap(context, ps.keySet());
	}

	private static int toIndex(Object key) {
		return key instanceof Number ? ((Number) key).intValue() : Integer
				.parseInt(String.valueOf(key));
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends Object> getValueType(Type type) {
		Type result = null;
		Class<? extends Object>  clazz = null;
		if (type instanceof ParameterizedType) {
			clazz = (Class) ((ParameterizedType) type).getRawType();
		} else {
			clazz = (Class) type;
		}
		if (Collection.class.isAssignableFrom(clazz)) {
			result = getParameterizedType(type, Collection.class, 0);
		}else if (Map.class.isAssignableFrom(clazz)) {
			result = getParameterizedType(type, Map.class, 1);
		}
		if (result != null) {
			return findClass(result);
		}
		return Object.class;
	}
	@SuppressWarnings("unchecked")
	public static Class getKeyType(Type type) {
		Class clazz = null;
		if (type instanceof ParameterizedType) {
			clazz = (Class) ((ParameterizedType) type).getRawType();
		} else {
			clazz = (Class) type;
		}
		if (Map.class.isAssignableFrom(clazz)) {
			Type result = getParameterizedType(type, Map.class, 0);
			if (result != null) {
				return findClass(result);
			}
		}
		return Integer.TYPE;
	}

	@SuppressWarnings("unchecked")
	private static Type getParameterizedType(Type type, Class destClass,
			int paramIndex) {
		Class clazz = null;
		ParameterizedType pt = null;
		Type[] ats = null;
		TypeVariable[] tps = null;
		if (type instanceof ParameterizedType) {
			pt = (ParameterizedType) type;
			clazz = (Class) pt.getRawType();
			ats = pt.getActualTypeArguments();
			tps = clazz.getTypeParameters();
		} else {
			clazz = (Class) type;
		}
		if (destClass.equals(clazz)) {
			if (pt != null) {
				return pt.getActualTypeArguments()[paramIndex];
			}
			return Object.class;
		}
		Class<?>[] ifs = clazz.getInterfaces();
		Type[] pis = clazz.getGenericInterfaces();
		for (int i = 0; i < ifs.length; i++) {
			Class<?> ifc = ifs[i];
			if (destClass.isAssignableFrom(ifc)) {
				return getTureType(getParameterizedType(pis[i], destClass,
						paramIndex), tps, ats);
			}
		}
		Class<?> type2 = clazz.getSuperclass();
		if (type2 != null) {
			if (destClass.isAssignableFrom(type2)) {
				return getTureType(getParameterizedType(clazz
						.getGenericSuperclass(), destClass, paramIndex), tps,
						ats);
			}
		}
		throw new IllegalArgumentException("必须是Collection 子类");
	}

	@SuppressWarnings("unchecked")
	private static Type getTureType(Type type, TypeVariable[] typeVariables,
			Type[] actualTypes) {
		if (type instanceof Class) {
			return type;
		} else if (type instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) type;
			String name = tv.getName();
			if (actualTypes != null) {
				for (int i = 0; i < typeVariables.length; i++) {
					if (name.equals(typeVariables[i].getName())) {
						return actualTypes[i];
					}
				}
			}
			return tv;
		}
		return type;
	}

	private static Class<?> findClass(Type result) {
		if (result instanceof Class) {
			return (Class<?>) result;
		} else if (result instanceof ParameterizedType) {
			return findClass(((ParameterizedType) result).getRawType());
		} else if (result instanceof WildcardType) {
			return findClass(((WildcardType) result).getUpperBounds()[0]);
		}
		return null;
	}

	public static Class<? extends Object> getPropertyType(Type type, Object key) {
		Class<?> clazz = findClass(type);
		if (clazz != null) {
			if (clazz.isArray()) {
				if (LENGTH.equals(key)) {
					return Integer.TYPE;
				} else {
					return clazz.getComponentType();
				}
			} else if (Collection.class.isAssignableFrom(clazz)) {
				return getValueType(type);
			} else if (Map.class.isAssignableFrom(clazz)) {
				return getValueType(type);
			} else {
				AccessDescriptor pd = getPropertyDescriptor(clazz, String
						.valueOf(key));
				if (pd != null) {
					return pd.type;
				}
			}
		}
		return null;

	}

	public static Object getValue(Object context, Object key) {
		if (context != null) {
			try {
				if (context.getClass().isArray()) {
					if (LENGTH.equals(key)) {
						return Array.getLength(context);
					} else {
						return Array.get(context, toIndex(key));
					}
				} else if (context instanceof Collection) {
					if (LENGTH.equals(key)) {
						return ((Collection<?>) context).size();
					} else if (context instanceof List<?>) {
						return ((List<?>) context).get(toIndex(key));
					}
				}else if (context instanceof String) {
					if (LENGTH.equals(key)) {
						return ((String)context).length();
					}
				}
				if (context instanceof Map) {
					return ((Map<?, ?>) context).get(key);
				}
				AccessDescriptor pd = getPropertyDescriptor(context.getClass(),
						String.valueOf(key));
				if (pd != null) {
					Method method = pd.reader;
					if (method != null) {
						return method.invoke(context);
					}
				}
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug(e);
				}
			}
		}
		return null;
	}

	private static AccessDescriptor getPropertyDescriptor(
			Class<? extends Object> type, String key) {
		Map<String, AccessDescriptor> pm = getPropertyMap(type);
		return pm.get(key);
	}

	@SuppressWarnings("unchecked")
	public static void setValues(Object base, Map<String, Object> attributeMap) {
		for (String key : attributeMap.keySet()) {
			ReflectUtil.setValue(base, key, attributeMap.get(key));
		}
	}

	@SuppressWarnings("unchecked")
	public static void setValue(Object base, Object key, Object value) {
		if (base != null) {
			try {
				if (base.getClass().isArray()) {
					Array.set(base, toIndex(key), value);
				} else if (base instanceof List<?>) {
					((List<Object>) base).set(toIndex(key), value);
				}
				if (base instanceof Map) {
					((Map<Object, Object>) base).put(key, value);
				}
				AccessDescriptor pd = getPropertyDescriptor(base.getClass(),
						String.valueOf(key));
				if (pd != null) {
					Method method = pd.writer;
					if (method != null) {
						if (value != null) {
							Class<? extends Object>  type = pd.type;
							if (!type.isInstance(value)) {
								type = NumberArithmetic.toWrapper(type);
								if (Number.class.isAssignableFrom(type)) {
									value = NumberArithmetic.getValue(type,
											(Number) value);
								}
							}
						}
						method.invoke(base, value);
					}
				}
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug(e);
				}
			}
		}

	}
}
