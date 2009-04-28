package org.xidea.el.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class ReflectUtil {
	private static final Log log = LogFactory.getLog(ReflectUtil.class);
	private static final String LENGTH = "length";
	private static final Map<Class<?>, Map<String, AccessDescriptor>> classPropertyMap = new WeakHashMap<Class<?>, Map<String, AccessDescriptor>>();

	private static class AccessDescriptor{
		private Method reader;
		private Method writer;
		private Class<? extends Object> type;
		public AccessDescriptor(java.beans.PropertyDescriptor property) {
			this.reader = property.getReadMethod();
			this.writer = property.getWriteMethod();
			this.type = property.getPropertyType();
			try{
				if(this.reader!=null){
					this.reader.setAccessible(true);
				}
				if(this.writer!=null){
					this.writer.setAccessible(true);
				}
			}catch (Exception e) {
			}
			
		}
		public Class<? extends Object> getPropertyType() {
			return type;
		}
		public Method getReadMethod() {
			return reader;
		}
		public Method getWriteMethod() {
			return writer;
		}
		
	}
	private static Map<String, AccessDescriptor> getPropertyMap(Class<?> clazz) {
		Map<String, AccessDescriptor> propertyMap = classPropertyMap
				.get(clazz);
		if (propertyMap == null) {
			try {
				propertyMap = new HashMap<String, AccessDescriptor>();
				java.beans.PropertyDescriptor[] properties = java.beans.Introspector
						.getBeanInfo(clazz).getPropertyDescriptors();
				for (int i = 0; i < properties.length; i++) {
					java.beans.PropertyDescriptor property = properties[i];
					propertyMap.put(property.getName(), new AccessDescriptor(property));
				}
				classPropertyMap.put(clazz, propertyMap);
			} catch (Exception e) {
			}
		}
		return propertyMap;
	}

	public static Map<String, ? extends Object> map(final Object context) {
		Map<String, AccessDescriptor> ps = getPropertyMap(context.getClass());
		return new ProxyMap(context, ps.keySet());
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
				AccessDescriptor pd = getPropertyDescriptor(type, String
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
				AccessDescriptor pd = getPropertyDescriptor(context
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
