package org.xidea.el.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.TokenImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.jsi.JSIRuntime;
import org.xidea.lite.Template;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.test.LiteTest;
import org.xidea.lite.test.oldcases.LiteTestUtil;

public class ELTest {
	private static JSIRuntime js = org.xidea.jsi.impl.RuntimeSupport.create();
	private static ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();
	static {

		try {
			js.eval("$import('org.xidea.el:*');//parseEL,Expression");
			js.eval("$import('org.xidea.jsidoc.util:*');");
			js.eval("function parseEL(el){return new ExpressionTokenizer(el).getResult()}");
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}
	private static org.xidea.el.json.JSONEncoder encoder = new org.xidea.el.json.JSONEncoder(org.xidea.el.json.JSONEncoder.W3C_DATE_TIME_FORMAT, true, 128){
		public void print(Object object,StringBuilder out){
			if(object instanceof Number){
				Number n = (Number) object;
				if(Float.isInfinite(n.floatValue())){
					out.append("null");
					//this.print("Infinite", out);
				}else if(Float.isNaN(n.floatValue())){
					out.append("null");
					//this.print("NaN", out);
				}else if(n.doubleValue() == 0){
					out.append("0");
				}else{
					super.print(object, out);
				}
			}else{
				super.print(object, out);
			}
		}
	};

	public static void testEL(Object context, String source) {
		String contextJSON;
		System.out.println("测试表达式：" + source + ",context:" + context);
		Object contextObject;
		if (context instanceof String) {
			contextJSON = (String) context;
			contextObject = JSONDecoder.decode(contextJSON);
		} else {
			contextJSON = encoder.encode(context,new StringBuilder()).toString();
			contextObject = context;
		}
		final String litecode = checkLiteParse(source);
		final String expect = runAsJS(source, contextJSON);
		Expression el = expressionFactory.create(JSONDecoder.decode(litecode));
		ParseContext parsedContext = createParserContext("${JSON.stringify(" + source + ")}");
		
		
		String javaresult = encoder.encode(el.evaluate(contextObject),new StringBuilder()).toString();
		Assert.assertEquals("Java 运行结果有误：#" + source, expect, javaresult);
		
		
		String jsStepResult = runStepJS(contextJSON, litecode);
		Assert.assertEquals("JS 运行结果有误(单步)：#" + source, expect, jsStepResult);
		
		String jsresult = normalizeJSON(LiteTest.runNativeJS(parsedContext, contextJSON));
		Assert.assertEquals("JS 运行结果有误(编译)：#" + source, expect,jsresult);
		
		
		String phpresult= normalizeJSON(LiteTest.runNativePHP(parsedContext, contextJSON));
		try{
			Assert.assertEquals("PHP 运行结果有误(编译)：#" + source, expect,phpresult);
		}catch(Error e){
			LiteTest.printLatestPHP();
			throw e;
		}
		
		System.out.println("表达式测试成功：" + source + "\t\t#" + contextJSON + '\n'
				+ javaresult);

	}

	private static String runStepJS(String contextJSON, final String litecode) {
		String jsResultString = (String) js.eval("JSON.stringify(evaluate("+ litecode + "," + contextJSON + "))");
		return encoder.encode(JSONDecoder.decode(jsResultString),new StringBuilder()).toString();
	}

	private static String runAsJS(String source, String contextJSON) {
		String evaljs = "(function(){with(" + contextJSON
				+ "){return JSON.stringify(" + source + ")}})()";
		String expect = (String) js.eval(evaljs);
		System.out.println(evaljs);
		System.out.println(expect);
		return normalizeJSON(expect);
	}


	private static String normalizeJSON(String result) {
		try {
			System.out.println(result);
			result = encoder.encode(JSONDecoder.decode(result),new StringBuilder()).toString();
		} catch (Exception e) {
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static String checkLiteParse(String source) {
		final String litecode = (String) js.eval("JSON.stringify(parseEL("
				+ encoder.encode(source,new StringBuilder()).toString() + "))");
		final Object javacode = expressionFactory.parse(source);

		TokenImpl jsc = TokenImpl.toToken((List<Object>) JSONDecoder
				.decode(litecode));
		jsc = jsc.optimize(ExpressionFactoryImpl.getInstance()
				.getStrategy(), new HashMap<String, Object>());
		Assert.assertEquals("Java 和 JS EL编译中间结果不一致：", encoder
				.encode(javacode,new StringBuilder()).toString(), encoder.encode(jsc,new StringBuilder()).toString().toString());
		return litecode;
	}


	private static ParseContext createParserContext(String el) {
		URI uri = new File(".", "unknow").toURI();
		ParseContext pc = LiteTestUtil.buildParseContext(uri);
		// System.out.println(pc.getFeatureMap());
		List<Object> tps = pc.parseText( el,
				Template.EL_TYPE);
		pc.appendAll(tps);
		return pc;
	}

	public static void main(String[] args) throws IOException {
		String context = "{\"a\":\"123''sddfg\"}";
		System.out.println(LiteTest.runNativePHP(createParserContext("1+a"), context));

	}
}
