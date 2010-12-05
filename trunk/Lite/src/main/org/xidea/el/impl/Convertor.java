package org.xidea.el.impl;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public interface Convertor<T> {
	public Convertor<Object> DEFAULT = new DefaultConvertor();
	public Map<Class<?>, Convertor<?>> DEFAULT_MAP = DefaultConvertor.toMap();

	public T getValue(String value, Class<? extends T> expectedType,
			Object context, String key);
}

class DefaultConvertor implements Convertor<Object> {
	private static Log log = LogFactory.getLog(DefaultConvertor.class);

	static Map<Class<?>, Convertor<?>> toMap() {
		Class<?>[] SUPPORTS = { File.class, URL.class, URI.class, Long.TYPE,
				Long.class, Integer.TYPE, Integer.class, Double.TYPE,
				Double.class, Short.TYPE, Short.class, Byte.TYPE, Byte.class,
				Boolean.TYPE, Boolean.class, Character.TYPE, Character.class,
				String.class, Object.class };
		HashMap<Class<?>, Convertor<?>> map = new HashMap<Class<?>, Convertor<?>>();
		for (Class<?> type : SUPPORTS) {
			map.put(type, Convertor.DEFAULT);
		}
		return Collections.unmodifiableMap(map);
	}

	public Object getValue(String value, Class<? extends Object> expectedType,
			Object context, String key) {
		boolean primitive = expectedType.isPrimitive();
		Class<? extends Object> clazz = primitive ? ReflectUtil
				.toWrapper(expectedType) : expectedType;
		try {
			if (value == null) {
				if (primitive) {
					if (Number.class.isAssignableFrom(clazz)) {
						value = "0";
					} else if (clazz == Boolean.class) {
						return false;
					} else if (clazz == Character.class) {
						return (char) 0;
					}
				} else {
					return null;
				}
			}
			// value != null
			// String|Object
			if (expectedType.isInstance(value)) {
				return value;
			}

			try {// Number,Boolean,File,URI,URL...
				if (clazz == Boolean.class) {
					if(value.length() ==0 ||value.equalsIgnoreCase("false")||value.equals("0")||value.equals("0.0")){
						return false;
					}else{
						return true;
					}
				}
				Constructor<? extends Object> constructor = clazz
						.getConstructor(String.class);
				try {
					return constructor.newInstance(value);
				} catch (InvocationTargetException e) {
					if (primitive && e.getTargetException() instanceof NumberFormatException) {
						return getValue("0", expectedType, context, key);
					}
				}
			} catch (NoSuchMethodException e) {
				if (Character.class == clazz) {
					if (value.length() == 0) {
						if (primitive) {
							return (char) 0;
						}
					} else {
						return value.charAt(0);
					}
				}
				Class<?> clazz2 = Class.forName(value);
				if (expectedType.isAssignableFrom(clazz2)) {
					return clazz2.newInstance();
				}
			}
		} catch (Exception e) {
			log.warn(e);
		}
		return null;
	}

};