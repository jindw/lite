package org.xidea.lite.tools;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class CollectionType {
	public static Class<? extends Object> getType(Object base,String property){
		try {
			Field field = base.getClass().getField(property);
			Class<?> type = field.getType();
			ParameterizedType pt = (ParameterizedType) field.getGenericType();
			Type[] types = pt.getActualTypeArguments();
		} catch (Exception e) {
		}
		return Object.class;
	}
	public void getType(Class<?> type,ParameterizedType pt){
		
	}

}
