package org.xidea.lite.test;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class ForTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testXMLParser() throws Exception {
		boolean[] b = new boolean[12];
		Object o = b;
		System.out.println(b.getClass().isArray());
		System.out.println(o instanceof Object[]);
		System.out.println(o instanceof boolean[]);
		System.out.println(Arrays.asList(b));
		System.out.println(Array.get(b,0));
	}
}
