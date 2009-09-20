package org.xidea.el.test;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.xidea.el.impl.ReflectUtil;

public class ReflectUtilTest {
	public static class Bean{
		private int a1 =1;
		private String a2 ="v2";
		public int getA1() {
			return a1;
		}
		public void setA1(int a1) {
			this.a1 = a1;
		}
		public String getA2() {
			return a2;
		}
		public void setA2(String a2) {
			this.a2 = a2;
		}
		
	}
//
//	@Test
//	public void testObjectMap() {
//		Map<Object, Object> map2 = ReflectUtil.map(new Bean());
//		map2.put(3,"v3");
//		assertEquals(ReflectUtil.getValue(map2,"a1"), 1);
//		assertEquals(ReflectUtil.getValue(map2,"a2"), "v2");
//		assertEquals(ReflectUtil.getValue(map2,3), "v3");
//	}
//	@Test
//	public void testMapMap() {
//		Map<Object, Object> map = new LinkedHashMap<Object, Object>();
//		map.put("1", "34");
//		Map<Object, Object> map2 = ReflectUtil.map(map);
//		Map<Object, Object> map3 = ReflectUtil.map(map);
//		map3.put(3,45);
//		assertEquals(map2, map);
//		assertTrue(1==map3.size()-map2.size());
//	}

	@Test
	public void testGetMapValue() {
		Map<Object, Object> map = new LinkedHashMap<Object, Object>();
		map.put("1", "v1");
		map.put(2,"v2");
		assertEquals(ReflectUtil.getValue(map,"1"), "v1");
		assertEquals(ReflectUtil.getValue(map,1), null);
		assertEquals(ReflectUtil.getValue(map,"2"), null);
		assertEquals(ReflectUtil.getValue(map,2), "v2");
	}

}
