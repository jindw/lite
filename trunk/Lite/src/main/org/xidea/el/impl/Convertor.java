package org.xidea.el.impl;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.xidea.el.fn.NumberArithmetic;

public interface Convertor<T> {
	public T getValue(String value, Class<? extends T> expectedType,
			Object context, String key);
	public Map<Class<?>, Convertor<?>> DEFAULT_MAP = Collections.unmodifiableMap(Default.INSTANCE.toMap());
	public static class Default implements Convertor<Object>{
		public static Default INSTANCE = new Default();
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
					return NumberArithmetic.getValue(clazz, 0);

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
				Constructor<? extends Object> constructor = expectedType.getConstructor(String.class);
				return constructor.newInstance(value);
			}catch(NoSuchMethodException e){
				try {
					@SuppressWarnings("unchecked")
					Class clazz2 = Class.forName(value);
					if(expectedType.isAssignableFrom(clazz2)){
						return clazz2.newInstance();
					}
				} catch (Exception e1) {
				}
			}catch(Exception e){
			}
			return null;
		}
		private static Class<?>[] SUPPORTS = {File.class,Long.TYPE,Long.class,
			Integer.TYPE,Integer.class,
			Double.TYPE,Double.class,
			Short.TYPE,Short.class,
			Boolean.TYPE,Boolean.class,
			String.class,Object.class
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