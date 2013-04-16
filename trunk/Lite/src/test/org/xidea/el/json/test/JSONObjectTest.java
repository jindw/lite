package org.xidea.el.json.test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder;


public class JSONObjectTest {

	public static class JavaBean{
	  String text = "a";
	  List<Long> list;
	  int i;
	  JavaBean inner;
	}
	public static class Wrapper{
		List<JavaBean> values;
	}
	@Test
	public void test() {
		JSONDecoder d = new JSONDecoder(false);
	
		Wrapper w = d.decode("{values:[{text:'b',list:[1,2,3],i:3,inner:{i:2}}]}", Wrapper.class);
		System.out.println(w);
System.out.println(w.values.get(0));
//		JavaBean o = d.decode("{text:'b',list:[1,2,3],i:3,inner:{i:2}}", JavaBean.class);
//		System.out.println(o.text);
//		System.out.println(o.list);
//		System.out.println(o.list.get(0).getClass());
//		System.out.println(o.inner.i);
//		System.out.println(JavaBean.class.isMemberClass());
//
//		System.out.println(JSONObjectTest.class.isMemberClass());
//
//		Object o2 = d.decode("[{text:'b',list:[1,2,3],i:3,inner:{i:2}}]", List.class);
//		System.out.println(o2.getClass());
		
	}

	
}
