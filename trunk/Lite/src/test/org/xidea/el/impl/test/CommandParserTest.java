package org.xidea.el.impl.test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class CommandParserTest {
	public List<String> data = new ArrayList<String>();
	public TestList<Integer> data2 = new TestList<Integer>();

	public class TestList<T> extends ArrayList<String> {
		private static final long serialVersionUID = 1L;
	};

	public class TestList2 extends TestList<Boolean> {
		private static final long serialVersionUID = 1L;
	};

	public List<String> getData() {
		return data;
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testAnotationArgs() throws SecurityException,
			NoSuchFieldException {

		Field field = this.getClass().getDeclaredField("data2");
		System.out.println(Arrays.asList(field.getAnnotations()));
		System.out.println(field.getType());
		ParameterizedType pt = (ParameterizedType) field.getGenericType();
		System.out.println(Arrays.asList(pt.getActualTypeArguments()));

	}

}
