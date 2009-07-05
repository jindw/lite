package org.xidea.lite.tools.test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;
import org.xidea.el.impl.ReflectUtil;


public class CollectionTypeTest {
	public ArrayList<? extends Collection<Double>> stringCollection = new ArrayList<Collection<Double>>();
	public StringList<? extends Integer> stringCollection2 = new StringList<Integer>();
	public StringList2<Integer,? extends String> stringCollection3 = new StringList2<Integer,String>();

	public Collection<String> stringMap = new ArrayList<String>();
	public StringMap<String, Boolean> stringMap2 = new StringMap<String, Boolean>();

	public static class StringList<T> extends ArrayList<String> implements
			Collection<String> {
		private static final long serialVersionUID = 1L;
	};

	public static class NumberList<T> extends ArrayList<Number> {
		private static final long serialVersionUID = 1L;
	};
	public static class StringList2<K, T> extends StringList<K> {
		private static final long serialVersionUID = 1L;
	};

	public static class StringList3<K3, T, K> extends StringList2<K3, T> {
		private static final long serialVersionUID = 1L;
	};

	public static class StringMap<K, T> extends HashMap<Boolean, String> {
		private static final long serialVersionUID = 1L;
	};


	@Test
	public void testGetListType() throws Exception {
		try {
			testField("stringCollection");
			testField("stringCollection2");
			testField("stringCollection3");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void testField(String key) throws NoSuchFieldException {
		Field field = this.getClass().getField(key);
		Class<?> type = field.getType();
		Type gtype = field.getGenericType();
		System.out.println(":" + ReflectUtil.getValueType(gtype));
	}

	@Test
	public void testGetMapKeyType() throws SecurityException,
			NoSuchFieldException {

	}

}
