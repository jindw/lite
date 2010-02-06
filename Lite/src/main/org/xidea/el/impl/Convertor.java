package org.xidea.el.impl;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface Convertor<T> {
	public T getValue(String value, Class<? extends T> expectedType,
			Object context, String key);
	public Map<Class<?>, Convertor<?>> DEFAULT_MAP = Collections.unmodifiableMap(new Default().toMap());
	public static class Default implements Convertor<Object>{

		public Object getValue(String value,
				Class<? extends Object> expectedType, Object context, String key) {
			if(File.class.isAssignableFrom(expectedType)){
				return new File(value);
			}
			Class<? extends Object> clazz = ReflectUtil.toWrapper(expectedType);
			if(Number.class.isAssignableFrom(clazz)){
				try {
					if(clazz == Long.class){
						return Long.parseLong(value);
					}else if(clazz == Integer.class){
						return Integer.parseInt(value);
					}else if(clazz == Byte.class){
						return Byte.parseByte(value);
					}else if(clazz == Double.class){
						return Double.parseDouble(value);
					}else if(clazz == Short.class){
						return Short.parseShort(value);
					}
				} catch (Exception ex) {
					if(expectedType == clazz){
						return null;
					}
					if(clazz == Long.class){
						return 0l;
					}else if(clazz == Integer.class){
						return 0;
					}else if(clazz == Byte.class){
						return Byte.parseByte(value);
					}else if(clazz == Double.class){
						return 0d;
					}else if(clazz == Short.class){
						return 0f;
					}
				}
			}else if (Boolean.class == clazz){
				try {
					if (value == null || value.length() == 0) {
						return false;
					}
					value = value.toLowerCase();
					return !("0".equals(value) || "false"
							.equals(value));
				} catch (Exception ex) {
					return false;
				}
			}
			//String|Object
			if(expectedType.isInstance(value)){
				return value;
			}
			try{
				return expectedType.getConstructor(String.class).newInstance(value);
			}catch(Exception e){
				return null;
			}
		}
		private static Class<?>[] SUPPORTS = {File.class,Long.TYPE,Long.class,
			Integer.TYPE,Integer.class,
			Double.TYPE,Double.class,
			Short.TYPE,Short.class,
			Boolean.TYPE,Boolean.class,
			Object.class
		};
		private Map<Class<?>, Convertor<?>> toMap(){
			HashMap<Class<?>, Convertor<?>> map = new HashMap<Class<?>, Convertor<?>>();
			for(Class<?> type : SUPPORTS){
				map.put(type, this);
			}
			return map;
		}
		
	};

}