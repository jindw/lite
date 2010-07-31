package org.xidea.el.impl.test;

import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.junit.Test;
import org.xidea.el.ExpressionToken;

public class ExpressionTokenGenTest {
	@Test
	public void testGenString()throws Exception {
		Field[] fields = ExpressionToken.class.getFields();
		HashMap<String, Object> values = new HashMap<String, Object>();
		for(Field f : fields){
			String name = f.getName();
			Object value = f.get(null);
			if(values.containsValue(value)){
				fail("常量值重复"+name+"=" +value);
			}else{
				values.put(name,value);
			}
			//System.out.println("var "+name + "= "+value+";");
			System.out.println("define('LITE_"+name + "',"+value+");");
		}
	}

}
