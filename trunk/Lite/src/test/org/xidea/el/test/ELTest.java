package org.xidea.el.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionToken;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.ExpressionParser;
import org.xidea.el.impl.TokenImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.jsi.JSIRuntime;
import org.xidea.lite.Template;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.test.LiteTest;

public class ELTest {
	private static JSIRuntime js = org.xidea.jsi.impl.v3.RuntimeSupport.create();
	private static ExpressionFactory optimizedFactory = new ExpressionFactoryImpl();
	private static ExpressionFactory noneOptimizedFactory = new ExpressionFactoryImpl(){

		public Object parse(String el) {
			ExpressionParser ep = new ExpressionParser(el);
			ep.setAliasMap(aliseMap);
			ExpressionToken tokens = ep.parseEL();
			//tokens = ((TokenImpl) tokens).optimize(strategy, Collections.EMPTY_MAP);
			return tokens;
		}
	};
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

	public static void testEL(Object context, String source,boolean jsonStringResult) {
		System.out.println("测试表达式：" + source + ",context:" + context);
		Map<String, String> resultMap = resultMap(context, source, jsonStringResult);
		String expect = resultMap.get("#expect");
		for (Map.Entry<String, String> entry : resultMap.entrySet()) {
			if(!entry.getKey().startsWith("#")){
				Assert.assertEquals(entry.getKey()+"运行结果有误：#" + source, expect,entry.getValue());
			}
		}
	}
	public static Map<String, String> resultMap(Object context, String source,boolean jsonStringResult) {
				String contextJSON;
		Object contextObject;
		if (context instanceof String) {
			contextJSON = (String) context;
			contextObject = JSONDecoder.decode(contextJSON);
			contextJSON = encoder.encode(contextObject,new StringBuilder()).toString();
		} else {
			contextJSON = encoder.encode(context,new StringBuilder()).toString();
			contextObject = context;
		}
		final String litecode = checkOptimizedLiteParse(source);
		final String expect = runAsJS(source, contextJSON);
		Expression el = optimizedFactory.create(JSONDecoder.decode(litecode));
		ParseContext parsedContext = createParserContext("${JSON.stringify(" + source + ")}");
		String javaresult = encoder.encode(el.evaluate(contextObject),new StringBuilder()).toString();
		String jsStepResult = runStepJS(contextJSON, litecode);
		//Assert.assertEquals("JS 运行结果有误(单步)：#" + source, expect, jsStepResult);
		String jsresult = normalizeJSON(LiteTest.runNativeJS(parsedContext, contextJSON),false);
		//Assert.assertEquals("JS 运行结果有误(编译)：#" + source, expect,jsresult);
		//LiteTest.replaceUnicode(content)
		String phpresult = LiteTest.runNativePHP(parsedContext, contextJSON);
		phpresult= normalizeJSON(phpresult,jsonStringResult);
		HashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("#model" , contextJSON);
		result.put("#expect", expect);
		result.put("java" , javaresult);
		result.put("jsStep" , jsStepResult);
		result.put("js" , jsresult);
		result.put("php" , phpresult);
		if(!expect.equals(phpresult)){
			LiteTest.printLatestPHP();
		}
		return result;
	}

	private static String runStepJS(String contextJSON, final String litecode) {
		String jsResultString = (String) js.eval("JSON.stringify(evaluate("+ litecode + "," + contextJSON + "))");
		//System.out.println("!!##"+jsResultString);
		return encoder.encode(JSONDecoder.decode(jsResultString),new StringBuilder()).toString();
	}

	private static String runAsJS(String source, String contextJSON) {
		String evaljs = "(function(){with(" + contextJSON
				+ "){return JSON.stringify(" + source + ")}})()";
		String expect = (String) js.eval(evaljs);
		System.out.println(evaljs);
		System.out.println(expect);
		return normalizeJSON(expect,false);
	}


	private static String normalizeJSON(String result,boolean jsonStringResult) {
		try {
//			System.out.println(result);
			if(jsonStringResult){
				String raw = JSONDecoder.decode((String)JSONDecoder.decode(result));
				raw = encoder.encode(raw,new StringBuilder()).toString();
				return encoder.encode(raw,new StringBuilder()).toString();
			}
			result = encoder.encode(JSONDecoder.decode(result),new StringBuilder()).toString();
		} catch (Exception e) {
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static String checkOptimizedLiteParse(String source) {
		final String litecode = (String) js.eval("JSON.stringify(parseEL("
				+ encoder.encode(source,new StringBuilder()).toString() + "))");
		final Object javacode = optimizedFactory.parse(source);

		System.out.println(litecode);
		System.out.println(javacode);
		TokenImpl jsc = TokenImpl.toToken((List<Object>) JSONDecoder
				.decode(litecode));
		jsc = jsc.optimize(ExpressionFactoryImpl.getInstance()
				.getStrategy(), new HashMap<String, Object>());
		Assert.assertEquals("Java 和 JS EL编译中间结果不一致："+source, 
				
				encoder.encode(jsc,new StringBuilder()).toString().toString(), 
				encoder.encode(javacode,new StringBuilder()).toString());
		return litecode;
	}


	private static ParseContext createParserContext(String el) {
		URI uri = new File(".", "unknow").toURI();
		ParseContextImpl pc = new ParseContextImpl(new ParseConfigImpl(uri,null), "/");
//		ParseContext pc = (ParseContextImpl) LiteTestUtil.buildParseContext(uri);
		pc.setExpressionFactory(noneOptimizedFactory);
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
