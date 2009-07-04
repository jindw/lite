package org.xidea.lite.tools.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class CollectionTypeTest {
	public ArrayList<Collection<Double>> stringCollection = new ArrayList<Collection<Double>>();
	public StringList<Integer> stringCollection2 = new StringList<Integer>();

	public Collection<String> stringMap = new ArrayList<String>();
	public StringMap<String, Boolean> stringMap2 = new StringMap<String, Boolean>();

	public static class StringList<T> extends ArrayList<String> implements
			Collection<String> {
		private static final long serialVersionUID = 1L;
	};

	public static class StringList2<K, T> extends StringList<K> {
		private static final long serialVersionUID = 1L;
	};

	public static class StringList3<K3, T,K> extends StringList2<K3, T> {
		private static final long serialVersionUID = 1L;
	};

	public static class StringMap<K, T> extends HashMap<Boolean, String> {
		private static final long serialVersionUID = 1L;
	};

	public Type getType(Class<?> clazz, Type pt) {
		if (List.class.equals(clazz) || Set.class.equals(clazz)
				|| Collection.class.equals(clazz)) {
			if (pt instanceof ParameterizedType) {
				return ((ParameterizedType) pt)
						.getActualTypeArguments()[0];
			}
			return Object.class;
		}
		Class<?>[] ifs = clazz.getInterfaces();
		Type[] pis = clazz.getGenericInterfaces();
		for (int i = 0; i < ifs.length; i++) {
			Class<?> ifc = ifs[i];
			if (Collection.class.isAssignableFrom(ifc)) {
				return getTureType(getType(ifc, pis[i]),pt);
			}
		}
		Class<?> type2 = clazz.getSuperclass();
		if (type2 != null) {
			if (Collection.class.isAssignableFrom(type2)) {
				return getType(type2, clazz.getGenericSuperclass());
			}
		}
		return null;
	}

	private Type getTureType(Type type, Type actualMap) {
		if (type instanceof Class) {
			return type;
		}else if (type instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable)type;
			if(actualMap instanceof ParameterizedType){
				ParameterizedType pt = (ParameterizedType)actualMap;
				Type[] actualTypes = pt.getActualTypeArguments();
				System.out.println("$"+Arrays.asList(actualTypes[0]));
			}
			return tv;
		}
		return type;
	}
	@Test
	public void testGetListType() throws Exception {
		try {
			Field field = this.getClass().getField("stringCollection");
			Class<?> type = field.getType();
			System.out.println(":"+getType(type, field.getGenericType()));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetMapKeyType() throws SecurityException,
			NoSuchFieldException {

	}

}
