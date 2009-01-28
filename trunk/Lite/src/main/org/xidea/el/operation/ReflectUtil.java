package org.xidea.el.operation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class ReflectUtil {
	private static final Log log = LogFactory.getLog(ReflectUtil.class);
	private static final String LENGTH = "length";
	private static final Map<Class<?>, Map<String, PropertyDescriptor>> classPropertyMap = new HashMap<Class<?>, Map<String, PropertyDescriptor>>();

	protected static Map<String, PropertyDescriptor> getPropertyMap(Class<?> clazz) {
		Map<String, PropertyDescriptor> propertyMap = classPropertyMap
				.get(clazz);
		if (propertyMap == null) {
			try {
				propertyMap = new HashMap<String, PropertyDescriptor>();
				PropertyDescriptor[] properties = java.beans.Introspector
						.getBeanInfo(clazz).getPropertyDescriptors();
				for (int i = 0; i < properties.length; i++) {
					PropertyDescriptor property = properties[i];
					propertyMap.put(property.getName(), property);
				}
				// propertyMap = Collections.unmodifiableMap(propertyMap);
				classPropertyMap.put(clazz, propertyMap);
			} catch (Exception e) {
			}
		}
		return propertyMap;
	}

	private static Map<String, ? extends Object> map(final Object context) {
		Map<String, PropertyDescriptor> ps = getPropertyMap(context.getClass());
		HashMap<String, Object> result = new HashMap<String, Object>() {


		};
		return result;
	}


	private static int toIndex(Object key) {
		return key instanceof Number ? ((Number) key).intValue() : Integer
				.parseInt(String.valueOf(key));
	}

	public static Class<? extends Object> getType(Class<? extends Object> type,
			Object key) {
		if (type != null) {
			if (type.isArray()) {
				if (LENGTH.equals(key)) {
					return Integer.TYPE;
				} else {
					return type.getComponentType();
				}
			} else if (Collection.class.isAssignableFrom(type)) {
				// TypeVariable<?>[] ca = type.getTypeParameters();
				// for (TypeVariable tv : ca) {
				// }
				return Object.class;
			} else if (Map.class.isAssignableFrom(type)) {
				return Object.class;
			} else {
				PropertyDescriptor pd = getPropertyDescriptor(type, String
						.valueOf(key));
				if (pd != null) {
					return pd.getPropertyType();
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
				}
				if (context instanceof Map) {
					return ((Map<?, ?>) context).get(key);
				}
				PropertyDescriptor pd = getPropertyDescriptor(context
						.getClass(), String.valueOf(key));
				if (pd != null) {
					Method method = pd.getReadMethod();
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

	private static PropertyDescriptor getPropertyDescriptor(
			Class<? extends Object> type, String key) {
		Map<String, PropertyDescriptor> pm = getPropertyMap(type);
		return pm.get(key);
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
				PropertyDescriptor pd = getPropertyDescriptor(base.getClass(),
						String.valueOf(key));
				if (pd != null) {
					Method method = pd.getWriteMethod();
					if (method != null) {
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
