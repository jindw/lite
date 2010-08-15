package org.xidea.el.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class ReflectUtil {
	private static final Log log = LogFactory.getLog(ReflectUtil.class);
	private static final String LENGTH = "length";
	private static final Map<Class<?>, Map<String, Method>> readerMap = new WeakHashMap<Class<?>, Map<String, Method>>();
	private static final Map<Class<?>, Map<String, Method>> writerMap = new WeakHashMap<Class<?>, Map<String, Method>>();
	private static final Map<Class<?>, Map<String, Class<?>>> typeMap = new WeakHashMap<Class<?>, Map<String, Class<?>>>();
	private static Object initLock = new Object();;

	public static Map<String, Method> getReaderMap(final Class<?> clazz) {
		Map<String, Method> propertyMap = readerMap.get(clazz);
		if (propertyMap == null) {
			initProperties(clazz);
			propertyMap = readerMap.get(clazz);
		}
		return propertyMap;
	}

	public static Map<String, Method> getWriterMap(final Class<?> clazz) {
		Map<String, Method> propertyMap = writerMap.get(clazz);
		if (propertyMap == null) {
			initProperties(clazz);
			propertyMap = writerMap.get(clazz);
		}
		return propertyMap;
	}

	private static Map<String, Class<?>> getTypeMap(final Class<?> clazz) {
		Map<String, Class<?>> propertyMap = typeMap.get(clazz);
		if (propertyMap == null) {
			initProperties(clazz);
			propertyMap = typeMap.get(clazz);
		}
		return propertyMap;
	}

	private static void initProperties(final Class<?> clazz) {
		synchronized (initLock) {
			HashMap<String, Method> getterMap = new HashMap<String, Method>();
			HashMap<String, Method> setterMap = new HashMap<String, Method>();
			HashMap<String, Class<?>> propertyMap = new HashMap<String, Class<?>>();
			try {
				if (!clazz.equals(Object.class)) {
					getterMap.putAll(getReaderMap(clazz.getSuperclass()));
					setterMap.putAll(getWriterMap(clazz.getSuperclass()));
					propertyMap.putAll(getTypeMap(clazz.getSuperclass()));
				}
				Method[] methods = clazz.getDeclaredMethods();

				for (Method m : methods) {
					if ((m.getModifiers() & Modifier.PUBLIC) > 0) {
						Class<? extends Object> type = m.getReturnType();
						Class<? extends Object>[] params = m
								.getParameterTypes();
						String name = m.getName();
						if (type == Void.TYPE) {
							if (params.length == 1 && name.startsWith("set")) {
								type = params[0];
								initMethod(setterMap, propertyMap, m, name
										.substring(3));
							}
						} else {
							if (params.length == 0) {
								if (name.startsWith("get")) {
									initMethod(getterMap, propertyMap, m, name
											.substring(3));
								} else if (type == Boolean.TYPE
										&& name.startsWith("is")) {
									initMethod(getterMap, propertyMap, m, name
											.substring(2));
								}
							}
						}
					}
				}
			} catch (Exception e) {
				log.warn("初始化属性集合异常", e);
			} finally {
				readerMap.put(clazz, Collections.unmodifiableMap(getterMap));
				writerMap.put(clazz, Collections.unmodifiableMap(setterMap));
				typeMap.put(clazz, Collections.unmodifiableMap(propertyMap));
			}
		}
	}

	private static void initMethod(Map<String, Method> propertyMap,
			Map<String, Class<?>> typeMap, Method m, String name) {
		if (name.length() > 0) {
			char c = name.charAt(0);
			if (Character.isUpperCase(c)) {
				name = Character.toLowerCase(c) + name.substring(1);
				m.setAccessible(true);
				propertyMap.put(name, m);
				Class<?> type = m.getReturnType();
				if (type == Void.TYPE) {
					type = m.getParameterTypes()[0];
				}
				Class<?> ot = typeMap.get(name);
				if (ot != null) {
					if (ot != type) {
						log.warn("属性类型冲突："+ot+"!="+type);
					}
				}
				typeMap.put(name, type);
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static Map<String, ? extends Object> map(final Object context) {
		if (context == null) {
			return Collections.EMPTY_MAP;
		} else if (context instanceof Map) {
			return (Map<String, ? extends Object>) context;
		}
		return new ProxyMap(context, getTypeMap(context.getClass()).keySet());
	}

	private static int toIndex(Object key) {
		return key instanceof Number ? ((Number) key).intValue() : Integer
				.parseInt(String.valueOf(key));
	}

	public static Class<? extends Object> getValueType(Type type) {
		Type result = null;
		Class<? extends Object> clazz = null;
		if (type instanceof ParameterizedType) {
			clazz = (Class<?>) ((ParameterizedType) type).getRawType();
		} else {
			clazz = (Class<?>) type;
		}
		if (Collection.class.isAssignableFrom(clazz)) {
			result = getParameterizedType(type, Collection.class, 0);
		} else if (Map.class.isAssignableFrom(clazz)) {
			result = getParameterizedType(type, Map.class, 1);
		}
		if (result != null) {
			return findClass(result);
		}
		return Object.class;
	}

	public static Class<?> getKeyType(Type type) {
		Class<?> clazz = null;
		if (type instanceof ParameterizedType) {
			clazz = (Class<?>) ((ParameterizedType) type).getRawType();
		} else {
			clazz = (Class<?>) type;
		}
		if (Map.class.isAssignableFrom(clazz)) {
			Type result = getParameterizedType(type, Map.class, 0);
			if (result != null) {
				return findClass(result);
			}
		}
		return Integer.TYPE;
	}

	private static Type getParameterizedType(Type type, Class<?> destClass,
			int paramIndex) {
		Class<?> clazz = null;
		ParameterizedType pt = null;
		Type[] ats = null;
		TypeVariable<?>[] tps = null;
		if (type instanceof ParameterizedType) {
			pt = (ParameterizedType) type;
			clazz = (Class<?>) pt.getRawType();
			ats = pt.getActualTypeArguments();
			tps = clazz.getTypeParameters();
		} else {
			clazz = (Class<?>) type;
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

	private static Type getTureType(Type type, TypeVariable<?>[] typeVariables,
			Type[] actualTypes) {
		if (type instanceof Class<?>) {
			return type;
		} else if (type instanceof TypeVariable<?>) {
			TypeVariable<?> tv = (TypeVariable<?>) type;
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

	private static Class<? extends Object> findClass(Type result) {
		if (result instanceof Class<?>) {
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
				} else if (Number.class.isInstance(key)) {
					return clazz.getComponentType();
				}
			} else if (Collection.class.isAssignableFrom(clazz)) {
				return getValueType(type);
			} else if (Map.class.isAssignableFrom(clazz)) {
				return getValueType(type);
			} else {
				Class<?> pd = getTypeMap(clazz).get(String.valueOf(key));
				if (pd != null) {
					return pd;
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
				} else if (context instanceof Collection<?>) {
					if (LENGTH.equals(key)) {
						return ((Collection<?>) context).size();
					} else if (context instanceof List<?>) {
						return ((List<?>) context).get(toIndex(key));
					}
				} else if (context instanceof String) {
					if (LENGTH.equals(key)) {
						return ((String) context).length();
					}
				}
				if (context instanceof Map<?, ?>) {
					return ((Map<?, ?>) context).get(key);
				}
				Method method = getReaderMap(context.getClass()).get(
						String.valueOf(key));
				if (method != null) {
					return method.invoke(context);
				}
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug(e);
				}
			}
		}
		if (key instanceof String) {
			try {

				if (context instanceof Class<?>) {
					return ((Class<?>) context).getField((String) key).get(
							context);
				} else {
					return context.getClass().getField((String) key).get(
							context);
				}
			} catch (NoSuchFieldException e) {
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug(e);
				}
			}
		}
		return null;
	}

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
				Method method = getWriterMap(base.getClass()).get(
						String.valueOf(key));
				if (method != null) {
					if (value != null) {
						Class<?> type = method.getParameterTypes()[0];
						if (!type.isInstance(value)) {
							type = toWrapper(type);
							if (Number.class.isAssignableFrom(type)) {
								value = toValue((Number) value, type);
							}
						}
					}
					method.invoke(base, value);
				}
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug(e);
				}
			}
		}

	}

	public static Number toValue(Number value, Class<? extends Object> type) {
		if (type == Long.class) {
			return value.longValue();
		} else if (type == Integer.class) {
			return value.intValue();
		} else if (type == Short.class) {
			return value.shortValue();
		} else if (type == Byte.class) {
			return value.byteValue();
		} else if (type == Double.class) {
			return value.doubleValue();
		} else if (type == Float.class) {
			return value.floatValue();
		} else {
			Class<? extends Object> clazz = ReflectUtil.toWrapper(type);
			if (clazz == type) {
				return null;
			} else {
				return toValue(value, clazz);
			}
		}
	}

	public final static Class<? extends Object> toWrapper(
			Class<? extends Object> type) {
		if (type.isPrimitive()) {
			if (Byte.TYPE == type) {
				return Byte.class;
			} else if (Short.TYPE == type) {
				return Short.class;
			} else if (Integer.TYPE == type) {
				return Integer.class;
			} else if (Long.TYPE == type) {
				return Long.class;
			} else if (Float.TYPE == type) {
				return Float.class;
			} else if (Double.TYPE == type) {
				return Double.class;
			} else if (Character.TYPE == type) {
				return Character.class;
			} else if (Boolean.TYPE == type) {
				return Boolean.class;
			}
		}
		return type;
	}
}
