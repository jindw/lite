package org.xidea.el.impl.test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.Reference;
import org.xidea.el.impl.ExpressionImpl;
import org.xidea.el.impl.ReflectUtil;


public class CollectionTypeTest {
	public ArrayList<? extends Collection<Double>> collectionList = new ArrayList<Collection<Double>>();
	public StringList<? extends Integer> stringList2 = new StringList<Integer>();
	public StringList2<Integer,? extends String> stringList3 = new StringList2<Integer,String>();

//	public Collection<String> stringMap = new ArrayList<String>();
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
		testField("collectionList",Collection.class);
		testField("stringList2",String.class);
		testField("stringList3",String.class);
		testField("stringMap2", String.class);
	}

	private void testField(String key,Class<?> expectType) throws NoSuchFieldException {
		Field field = this.getClass().getField(key);
		Type gtype = field.getGenericType();
		Assert.assertEquals(expectType, ReflectUtil.getValueType(gtype));
	}

	@Test
	public void testGetMapKeyType() throws SecurityException,
			NoSuchFieldException {
		Field field = this.getClass().getField("stringMap2");
		Type gtype = field.getGenericType();
		Assert.assertEquals(Boolean.class, ReflectUtil.getKeyType(gtype));
	}


	@Test
	public void testMapSetter() throws SecurityException,
			NoSuchFieldException {
		final  Map<String, Integer> data = new HashMap<String, Integer>();
		Object context = new Object(){
			public Map<String, Integer> getData() {
				return data;
			}
			
		};
		ExpressionImpl el = new ExpressionImpl("data.key1");
		Reference result = el.prepare(context);
		System.out.println(data);
		result.setValue(123);
		System.out.println(data);
		Assert.assertEquals((Object)123, data.get("key1"));
	}
	

}
