package org.xidea.lite.parser.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.TextParser;
import org.xidea.lite.test.LiteTestUtil;
import org.xml.sax.SAXException;

public class ClientJSBuilderTest {
	@Before
	public void setUp() throws Exception {

	}
	RuntimeSupport proxy = (RuntimeSupport) RuntimeSupport.create();



	@Test
	public void testBuildJS() throws SAXException, IOException, URISyntaxException {
		URI url = this.getClass().getResource("format-test.xhtml").toURI();
		ParseContext context2 = LiteTestUtil.buildParseContext(url);
		// 前端直接压缩吧？反正保留那些空白也没有调试价值
		// context2.setCompress(context.isCompress());
		context2.parse(context2.loadXML(url));
		JSIRuntime rt = RuntimeSupport.create();
		proxy.eval("$import('org.xidea.lite.impl.js:JSTranslator')");
		Object ts = proxy.eval("new JSTranslator('t1')");
		String result = (String)proxy.invoke(ts, "translate", context2.toList());

		System.out.println("==JS Code==");
		System.out.println(result);
		boolean isError = Pattern.compile("[\r\n]alert", Pattern.MULTILINE)
				.matcher(result).find();
		Assert.assertTrue("生成失败" + result, !isError);
	}


	@Test
	public void testClient() throws SAXException, IOException, URISyntaxException {
		URI url = this.getClass().getResource("asciitable-client.xhtml").toURI();
		ParseContext context2 = LiteTestUtil.buildParseContext(url);
		// 前端直接压缩吧？反正保留那些空白也没有调试价值
		// context2.setCompress(context.isCompress());
		context2.parse(context2.loadXML(url));

		List<Object> clientLiteCode = context2.toList();
		System.out.println("==JS Code==");
		System.out.println(clientLiteCode.toString());
		String result = clientLiteCode.toString();
		boolean isError = Pattern.compile("[\r\n]alert", Pattern.MULTILINE)
		.matcher(result).find() || !Pattern.compile("\\.push\\(", Pattern.MULTILINE)
		.matcher(result).find();
		Assert.assertTrue("生成失败" + result, !isError);
	}
//
//	@Test
//	public void testCTClient() throws SAXException, IOException, URISyntaxException {
//		InputStreamReader source = new InputStreamReader(this.getClass()
//				.getResourceAsStream("ct-client.txt"), "utf-8");
//		
//		ParseContext context2 = LiteTestUtil.buildParseContext(new URI("http://w/"));
//		context2.addTextParser(ELParser.EL);
//		context2.addTextParser(ELParser.IF);
//		context2.addTextParser(ELParser.FOR);
//		context2.addTextParser(ELParser.ELSE);
//		context2.addTextParser(ELParser.CLIENT);
//		context2.addTextParser(ELParser.END);
//		context2.addTextParser(ELParser.VAR);
//		// 前端直接压缩吧？反正保留那些空白也没有调试价值
//		// context2.setCompress(context.isCompress());
//		context2.parse(loadText(source));
//		System.out.println(context2.toList());
//	}

	public static String loadText(Reader in) {
		// Reader in = new InputStreamReader(sin, "utf-8");
		StringWriter out = new StringWriter();
		int count;
		char[] cbuf = new char[1024];
		try {
			while ((count = in.read(cbuf)) > -1) {
				out.write(cbuf, 0, count);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toString();
	}
}
