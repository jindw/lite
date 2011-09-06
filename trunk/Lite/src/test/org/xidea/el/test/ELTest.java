package org.xidea.el.test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.TokenImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.jsi.JSIRuntime;
import org.xidea.lite.Template;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.test.oldcases.LiteTestUtil;

public class ELTest {
	private static JSIRuntime js = org.xidea.jsi.impl.RuntimeSupport.create();
	private static ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();
	private static String phpcmd = "php";
	private static String currentPHP;
	static {

		try {
			js.eval("$import('org.xidea.el:*');//parseEL,Expression");
			js.eval("$import('org.xidea.lite.parse:*');//parseEL,Expression");
			// js.eval("$import('org.xidea.lite.impl:*');//parseEL,Expression");
			js.eval("$import('org.xidea.jsidoc.util:*');");
			js
					.eval("function parseEL(el){return new ExpressionTokenizer(el).getResult()}");
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
		{
			String mbload = execPhp("echo extension_loaded('mbstring')?'true':'false';");
			String extdir = execPhp("echo ini_get('extension_dir');");
			if (mbload.endsWith("false")) {
				File file = new File(extdir, "php_mbstring.dll");
				if (file.exists()) {
					phpcmd = "php -d extension=php_mbstring.dll";
				} else {
					phpcmd = "php -d extension=ext/php_mbstring.dll";
				}
			}
			String flag = execPhp("echo extension_loaded('mbstring')?'true':'false';");
		}
	}

	private static String execPhp(String code) {
		Process proc;
		try {
			proc = Runtime.getRuntime().exec(phpcmd);
			OutputStream out = proc.getOutputStream();
			out.write(("<?php\n" + code).getBytes("UTF-8"));
			out.flush();
			out.close();
			String flag = ParseUtil.loadTextAndClose(proc.getInputStream(),
					"utf-8");
			return flag;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Object testEL(Object context, String source) {
		String contextJSON;
		System.out.println("测试表达式：" + source + ",context:" + context);
		Object contextObject;
		if (context == null || "".equals(context)) {
			contextObject = Collections.EMPTY_MAP;
			contextJSON = "{}";
		} else if (context instanceof String) {
			contextJSON = (String) context;
			contextObject = JSONDecoder.decode(contextJSON);
		} else {
			contextJSON = JSONEncoder.encode(context);
			contextObject = context;
		}
		final Object javacode = expressionFactory.parse(source);
		final String jscode = (String) js.eval("JSON.stringify(parseEL("
				+ JSONEncoder.encode(source) + "))");
		Expression el = expressionFactory.create(javacode);
		String evaljs = "(function(){with(" + contextJSON
				+ "){return JSON.stringify(" + source + ")}})()";
		String expect = (String) js.eval(evaljs);
		Object javaResult = el.evaluate(contextObject);
		String javaResultString = (String) js.eval("JSON.stringify("
				+ JSONEncoder.encode(javaResult) + ")");
		String jsResultString = (String) js.eval("JSON.stringify(evaluate("
				+ jscode + "," + contextJSON + "))");
		String nativeJSResultString = runNativeJS(source, contextJSON);
		String nativePHPResultString = runNativePHP(source, contextJSON);

		// System.out.println(evaljs);
		// System.out.println(JSONEncoder.encode(jscode));
		Assert.assertEquals("Java 运行结果有误：#" + source, expect, javaResultString);
		Assert.assertEquals("JS 运行结果有误(单步)：#" + source, expect, jsResultString);
		Assert.assertEquals("JS 运行结果有误(编译)：#" + source, expect,
				nativeJSResultString);
		try {
			Assert.assertEquals("PHP 运行结果有误(编译)：#" + source, expect,
					nativePHPResultString);
		} catch (RuntimeException e) {
			System.out.println("出错PHP脚本\n" + currentPHP);
			throw e;
		}catch (Error e) {
			System.out.println("出错PHP脚本\n" + currentPHP);
			throw e;
		}
		TokenImpl jsc = TokenImpl.toToken((List<Object>) JSONDecoder
				.decode(jscode));
		jsc = jsc.optimize(ExpressionFactoryImpl.getInstance().getStrategy(),
				new HashMap<String, Object>());
		Assert.assertEquals("Java 和 JS EL编译中间结果不一致：", JSONEncoder
				.encode(javacode), JSONEncoder.encode(jsc));

		System.out.println("表达式测试成功：" + source + "\t\t#" + contextJSON + '\n'
				+ javaResultString);
		return javaResult;

	}

	private static String runNativeJS(String source, String contextJSON) {
		ParseContext pc = createParserContext(source);
		js.eval("$import('org.xidea.lite.impl.js:JSTranslator')");
		Object ts = js.eval("new JSTranslator('')");
		String code = (String) js.invoke(ts, "translate", pc.toList(), true);
		// System.out.println(code);
		String jsResult = (String) js.eval("(function(){" + code + "})()("
				+ contextJSON + ")");
		return jsResult;
	}

	private static ParseContext createParserContext(String el) {
		URI uri = new File(".", "unknow").toURI();
		ParseContext pc = LiteTestUtil.buildParseContext(uri);
		// System.out.println(pc.getFeatureMap());
		List<Object> tps = pc.parseText("${JSON.stringify(" + el + ")}",
				Template.EL_TYPE);
		pc.appendAll(tps);
		return pc;
	}

	private static String runNativePHP(String source, String contextJSON) {
		ParseContext pc = createParserContext(source);
		js.eval("$import('org.xidea.lite.impl.php:PHPTranslator')");

		String litecode = JSONEncoder.encode(Arrays.asList(
				new ArrayList<String>(), pc.toList(), pc.getFeatureMap()));
		Object ts = js.eval("new PHPTranslator('/test'," + litecode + ")");
		String code = (String) js.invoke(ts, "translate");
		StringBuilder buf = new StringBuilder(code.replaceFirst("<\\?php", ""));
		URL f = ELTest.class.getResource("/lite/LiteEngine.php");
		try {
			buf
					.append("\nrequire_once('"
							+ new File(f.toURI()).toString().replace('\\', '/')
							+ "');");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		// buf.append("print_r(('");
		// buf.append(contextJSON.replaceAll("['\\\\]", "\\\\$0"));
		// buf.append("',true));");
		// buf.append("echo '\n',json_last_error();");

		buf.append("\nlite_template_test(json_decode('");
		buf.append(contextJSON.replaceAll("['\\\\]", "\\\\$0"));
		buf.append("',true));");
		
		// System.out.println(buf);
		String result = execPhp(currentPHP = buf.toString());
		try{
			return JSONEncoder.encode(JSONDecoder.decode(result));
		}catch (Exception e) {
		}
		return result;
		// System.out.println(code);
		// String phpResult =
		// (String)js.eval("(function(){"+code+"})()("+contextJSON+")");
		// return phpResult;
	}

	public static void main(String[] args) throws IOException {
		String context = "{\"a\":\"123''sddfg\"}";
		System.out.println(runNativePHP("1+a", context));

	}
}
