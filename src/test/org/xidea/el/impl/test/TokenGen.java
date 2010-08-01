package org.xidea.el.impl.test;

import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.xidea.el.ExpressionToken;
import org.xidea.el.Invocable;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.OperationStrategyImpl;

public class TokenGen {
	@SuppressWarnings("unchecked")
	@Test
	public void genFunctionsSupport()throws Exception {
		ExpressionFactoryImpl exp = new ExpressionFactoryImpl();
		OperationStrategyImpl osi = (OperationStrategyImpl) exp.getStrategy();
		System.out.println(osi.getGlobalMap().keySet());
		Field field = OperationStrategyImpl.class.getDeclaredField("methodMap");
		field.setAccessible(true);
		Map<String, Map<String, Invocable>> methodMap = (Map<String, Map<String, Invocable>>) field.get(osi);
		for(String key : methodMap.keySet()){
			Map<String, Invocable> map = methodMap.get(key);
			System.out.println(key);
			System.out.print("类型：");
			for (String c : map.keySet()) {
				if(!c.startsWith("[")){
					System.out.print(c.substring(c.lastIndexOf('.')+1)+",");
				}
			}
			System.out.println();
		}
		
	}
	@Test
	public void genExpressionTokens()throws Exception {
		Field[] fields = ExpressionToken.class.getFields();
		HashMap<String, Object> values = new HashMap<String, Object>();
		StringBuilder php = new StringBuilder();
		StringBuilder js = new StringBuilder();
		for(Field f : fields){
			String name = f.getName();
			Object value = f.get(null);
			if(values.containsValue(value)){
				fail("常量值重复"+name+"=" +value);
			}else{
				values.put(name,value);
			}
			js.append("var "+name + "= "+value+";\n");
			php.append("define('LITE_"+name + "',"+value+");\n");
		}
		System.out.println(js);
		System.out.println();
		System.out.println();
		System.out.println(php);
	}

}
