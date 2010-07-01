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
	public static Object testEL(Object context2,String source) throws Exception {
				String contextJSON;
		Object contextObject;
		if(context2 == null){
			contextObject = Collections.EMPTY_MAP;
			contextJSON = "{}";
		}else if(context2 instanceof String){
			contextJSON = (String) context2;
			contextObject = JSON.decode(context2);
		}else{
			contextJSON = JSON.stringify(context2);
			contextObject = context2;
		}
		Object javacode = expressionFactory.parse(source);
		String jscode = (String)js.eval("JSON.stringify(parseEL("+JSON.encode(source)+"))");
		Assert.assertEquals("Java 和 JS EL解析结果不一致：", JSON.encode(javacode), jscode);
		Expression el = expressionFactory.create(javacode);
		String expect = (String) js.eval("(function(){with("+contextJSON+"){return JSON.stringify("+source+")}})()");
		Object javaResult = el.evaluate(contextObject);
		String javaResultString =(String)js.eval("JSON.stringify(eval(["+JSON.encode(javaResult)+"][0]))");
		String jsResultString = (String)js.eval("JSON.stringify(evaluate("+jscode+","+contextJSON+"))");
		Assert.assertEquals("Java 运行结果有误：", expect, javaResultString);
		Assert.assertEquals("JS 运行结果有误：", expect, jsResultString);
		
		System.out.println("表达式测试成功："+source+"\t\t#"+contextJSON);
		return javaResult;
		
	}
}
