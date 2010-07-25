package org.xidea.el.test;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.fn.ECMA262Impl.JSON;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.jsi.JSIRuntime;
import org.xidea.lite.Template;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.test.LiteTestUtil;

public class ELTest {
	static JSIRuntime js = org.xidea.jsi.impl.RuntimeSupport.create();
	static ExpressionFactory expressionFactory = ExpressionFactoryImpl.getInstance();
	static{
		try{
		js.eval("$import('org.xidea.el:*');//parseEL,Expression");
		js.eval("$import('org.xidea.lite.parse:*');//parseEL,Expression");
		//js.eval("$import('org.xidea.lite.impl:*');//parseEL,Expression");
		js.eval("$import('org.xidea.jsidoc.util:*');");
		js.eval("function parseEL(el){return new ExpressionTokenizer(el).getResult()}");
		}catch(RuntimeException e){
			e.printStackTrace();
			throw e;
		}
	}
	@Test
	public void test(){
		testEL(null,"'abc'*123");
	}
	public static Object testEL(Object context,String source){
				String contextJSON;
		System.out.println("测试表达式："+source+",context:"+context);
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
		String nativeResultString = runNativeJS(source, contextJSON);
		Assert.assertEquals("Java 运行结果有误：#"+source, expect, javaResultString);
		Assert.assertEquals("JS 运行结果有误(单步)：#"+source, expect, jsResultString);
		Assert.assertEquals("JS 运行结果有误(编译)：#"+source, expect, nativeResultString);
		
		System.out.println(JSON.encode(javacode));
		Assert.assertEquals("Java 和 JS EL编译中间结果不一致：", JSON.encode(javacode), jscode);
		
		System.out.println("表达式测试成功："+source+"\t\t#"+contextJSON);
		return javaResult;
		
	}
	private static String runNativeJS(String source, String contextJSON) {
		ParseContext pc = createParserContext();
		List<Object> tps = pc.parseText("${JSON.stringify("+source+")}", Template.EL_TYPE);
		pc.appendAll(tps);
		js.eval("$import('org.xidea.lite.impl:Translator')");
		Object ts = js.eval("new Translator('')");
		String code = (String)js.invoke(ts, "translate", pc);
		String jsResult = (String)js.eval("("+code+")("+contextJSON+")");
		return jsResult;
	}
	private static ParseContext createParserContext() {
		URI uri = new File(".","unknow").toURI();
		ParseContext pc = LiteTestUtil.buildParseContext(uri);
		return pc;
	}
}
