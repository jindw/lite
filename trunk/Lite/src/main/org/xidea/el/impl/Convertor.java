package org.xidea.el.impl;

import java.io.File;
import java.lang.reflect.Constructor;
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
class DefaultConvertor implements Convertor<Object>{
	private static Log log = LogFactory.getLog(DefaultConvertor.class);
	static Map<Class<?>, Convertor<?>> toMap(){
		Class<?>[] SUPPORTS = {File.class,URL.class,URI.class,
				Long.TYPE,Long.class,
				Integer.TYPE,Integer.class,
				Double.TYPE,Double.class,
				Short.TYPE,Short.class,
				Byte.TYPE,Byte.class,
				Boolean.TYPE,Boolean.class,
				Character.TYPE,Character.class,
				String.class,Object.class
			};
		HashMap<Class<?>, Convertor<?>> map = new HashMap<Class<?>, Convertor<?>>();
		for(Class<?> type : SUPPORTS){
			map.put(type, Convertor.DEFAULT);
		}
		return Collections.unmodifiableMap(map);
	}
	public Object getValue(String value,
			Class<? extends Object> expectedType, Object context, String key) {
		boolean primitive = expectedType.isPrimitive();
		if(value == null){
			if(primitive){
				value = "";//number boolean char?
			}else{
				return null;
			}
		}
		Class<? extends Object> clazz = ReflectUtil.toWrapper(expectedType);
		if (Number.class.isAssignableFrom(clazz)) {
			if(value.length() == 0){
				if(primitive){
					value = "0";
				}else{
					return null;
				}
			}
			if (clazz == Long.class) {
				return Long.parseLong(value);
			} else if (clazz == Integer.class) {
				return Integer.parseInt(value);
			} else if (clazz == Byte.class) {
				return Byte.parseByte(value);
			} else if (clazz == Double.class) {
				return Double.parseDouble(value);
			} else if (clazz == Short.class) {
				return Short.parseShort(value);
			}
		}else if (Boolean.class == clazz){
			value = value.toLowerCase();
			if("false".equals(value)){
				return false;
			}else if("true".equals(value)){
				return true;
			}
			if(!primitive && value.length() == 0){
				return null;
			}
			log.info("非标准的boolean字面量！！");
			return !(value.length() == 0 || "0".equals(value) || "0.0".equals(value) || "false".equalsIgnoreCase(value));
		}else if (Character.class == clazz){
			if(value.length() == 0){
				if(primitive){
					value = "\u0000";
				}else{
					return null;
				}
			}
			value = value.toLowerCase();
			return value.charAt(0);
		}
		//String|Object
		if(expectedType.isInstance(value)){
			return value;
		}
		try{//File,URI,URL...
			Constructor<? extends Object> constructor = expectedType.getConstructor(String.class);
			return constructor.newInstance(value);
		}catch(NoSuchMethodException e){
			try {
				Class<?> clazz2 = Class.forName(value);
				if(expectedType.isAssignableFrom(clazz2)){
					return clazz2.newInstance();
				}
			} catch (Exception e1) {
			}
		}catch(Exception e){
		}
		return null;
	}
	
};