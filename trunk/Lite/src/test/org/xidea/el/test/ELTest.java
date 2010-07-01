package org.xidea.el.test;

import java.util.Collections;

import org.junit.Assert;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.fn.ECMA262Impl.JSON;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.jsi.JSIRuntime;

public class ELTest {
	static JSIRuntime js = org.xidea.jsi.impl.RuntimeSupport.create();
	static ExpressionFactory expressionFactory = ExpressionFactoryImpl.getInstance();
	static{
		js.eval("$import('org.xidea.lite:*');//parseEL,Expression");
		js.eval("$import('org.xidea.jsidoc.util:JSON');");
	}
	public static Object testEL(Object context,String source){
				String contextJSON;
		Object contextObject;
		if(context == null || "".equals(context)){
			contextObject = Collections.EMPTY_MAP;
			contextJSON = "{}";
		}else if(context instanceof String){
			contextJSON = (String) context;
			contextObject = JSON.decode(context);
		}else{
			contextJSON = JSON.stringify(context);
			contextObject = context;
		}
		final Object javacode = expressionFactory.parse(source);
		final String jscode = (String)js.eval("JSON.stringify(parseEL("+JSON.encode(source)+"))");
		Expression el = expressionFactory.create(javacode);
		String expect = (String) js.eval("(function(){with("+contextJSON+"){return JSON.stringify("+source+")}})()");
		Object javaResult = el.evaluate(contextObject);
		String javaResultString =(String)js.eval("JSON.stringify("+JSON.encode(javaResult)+")");
		String jsResultString = (String)js.eval("JSON.stringify(evaluate("+jscode+","+contextJSON+"))");
		Assert.assertEquals("Java 运行结果有误：#"+source, expect, javaResultString);
		//Assert.assertEquals("JS 运行结果有误：#"+source, expect, jsResultString);
		
		Assert.assertEquals("Java 和 JS EL编译中间结果不一致：", JSON.encode(javacode), jscode);
		
		System.out.println("表达式测试成功："+source+"\t\t#"+contextJSON);
		return javaResult;
		
	}
}
