package org.xidea.el.impl.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.impl.ReflectUtil;

public class ReflectUtilTest {
	static class MapBean{
		private int attr1 = 1;
		private String attr2 = "2";
		private int attr3 = 3;
		public String getAttr2() {
			return attr2;
		}
		public void setAttr2(String attr2) {
			this.attr2 = attr2;
		}
		public void setAttr1(int attr1) {
			this.attr1 = attr1;
		}
		private int getAttr3() {
			return attr3;
		}
		private void setAttr3(int attr3) {
			this.attr3 = attr3;
		}
	}

	@Test
	public void testMap() {
		Map map = ReflectUtil.map(new MapBean(){
			public int getAttr4() {
				return 4;
			}
		});
		System.out.println(map);
		Assert.assertEquals("2", map.get("attr2"));
		Assert.assertEquals(null, map.get("attr1"));
		Assert.assertEquals(4, map.get("attr4"));
		Assert.assertEquals(map.keySet(),new HashSet(Arrays.asList("class","attr4","attr1","attr2")));
	}

	static class GetValueBean extends MapBean{
		public int testGet1 = 1;
		public String testGet2 = "t2";
		public String testGet3 = "t3";

		public int getTestGet1() {
			return testGet1;
		}
		
		public String getTestGet2() {
			return testGet2;
		}
		public void setTestGet2(String testGet2) {
			this.testGet2 = testGet2;
		}
		String getTestGet3() {
			return testGet3;
		}
		void setTestGet3(String testGet3) {
			this.testGet3 = testGet3;
		}
		
	}

	@Test
	public void testGetValue() {
		GetValueBean map = new GetValueBean();
		System.out.println(map);
		Assert.assertEquals(1, ReflectUtil.getValue(map,"testGet1"));
		Assert.assertEquals("t2", ReflectUtil.getValue(map,"testGet2"));
		Assert.assertEquals(null, ReflectUtil.getValue(map,"testGet3"));
	}
	

	static class GetValueTypeBean extends GetValueBean{
		private GetValueBean sub;

		private int number;
		private int getter;
		private String setter;
		private GetValueTypeBean gsetter;

		public GetValueBean getSub() {
			return sub;
		}

		public void setSub(GetValueBean sub) {
			this.sub = sub;
		}

		public int getGetter() {
			return getter;
		}

		public void setSetter(String setter) {
			this.setter = setter;
		}
		public void setNumber(int number) {
			this.number = number;
		}

		public GetValueTypeBean getGsetter() {
			return gsetter;
		}

		public void setGsetter(GetValueTypeBean gsetter) {
			this.gsetter = gsetter;
		}
		
		
	}
	@Test
	public void testGetValueType() {
		Assert.assertEquals(int.class, ReflectUtil.getPropertyType(GetValueTypeBean.class, "getter"));
		Assert.assertEquals(String.class, ReflectUtil.getPropertyType(GetValueTypeBean.class, "setter"));
	}


	@Test
	public void testSetValue() {
		GetValueTypeBean gvtb = new GetValueTypeBean();

		ReflectUtil.setValue(gvtb, "number",10);
		Assert.assertEquals(10,gvtb.number);

		ReflectUtil.setValue(gvtb, "number",2.2f);
		Assert.assertEquals(2,gvtb.number);
		ReflectUtil.setValue(gvtb, "number",3d);
		Assert.assertEquals(3,gvtb.number);
		
		
		ReflectUtil.setValue(gvtb, "getter",2);
		Assert.assertEquals(0,gvtb.getGetter());
		ReflectUtil.setValue(gvtb, "gsetter",gvtb);
		Assert.assertEquals(gvtb,gvtb.getGsetter());
	}

}
