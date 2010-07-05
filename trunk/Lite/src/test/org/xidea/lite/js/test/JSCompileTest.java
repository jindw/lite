package org.xidea.lite.js.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.test.TestUtil;
import org.xml.sax.SAXException;

import sun.org.mozilla.javascript.internal.BaseFunction;
import sun.org.mozilla.javascript.internal.Callable;
import sun.org.mozilla.javascript.internal.Context;
import sun.org.mozilla.javascript.internal.Scriptable;

public class JSCompileTest {
	ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");
	URI menuURL;
	File webRoot = new File(new File(JSCompileTest.class.getResource("/")
			.getFile()), "../../");
	ParseContextImpl context;

	public JSCompileTest() throws MalformedURLException {
		context = TestUtil.buildParseContext(webRoot.toURI());
	}

	public String getText(Node doc, String xpath)
			throws XPathExpressionException {
		Node node = context.selectNodes(doc, xpath).getFirstChild();
		if(node == null){
			return null;
		}
		return node.getTextContent();
	}

	@Before
	public void setup() throws Exception, ScriptException {

		menuURL = new File(webRoot, "menu.xml").toURI();
		engine.eval("this['javax.script.filename']='<boot.js>'");
		engine.eval(new InputStreamReader(this.getClass().getResourceAsStream(
				"/boot.js"), "utf-8"));
		engine.put("evalFile", new BaseFunction() {

			@Override
			public Object call(Context ct, Scriptable scope,
					Scriptable thisArg, Object[] args) {
				String sourceName = String.valueOf(args[1]);
				String source = (String) args[0];
				//thisArg = (Scriptable) args[2];
				source = "function(){"+source+"\n}";
				Callable call = (Callable) ct.compileFunction(scope,source, sourceName, 1, null);
				try{
					//ct.enter();
					System.out.println(scope.getClass());
					System.out.println(scope);
					return call.call(ct, scope, thisArg, args);
				}finally{
					//ct.exit();
				}
			}

		});
		//engine.eval("evalFile.call(null,'print(this)',111)");
		engine.eval("$import('org.xidea.lite.impl:Template');");
		engine.eval("$import('org.xidea.lite.impl:Translator');");
		engine.eval("$import('org.xidea.lite.impl:XMLParser');");
		engine.eval("$import('org.xidea.el:findELEnd')");
	}

	@Test
	public void testTextParser() throws Exception {
		String test = "您好：${user}，您的的ip地址是：${ippart0}  .${ip.part1}.${ip.part2}.${ip.part3}。";

		Assert.assertEquals(11, engine.eval("new XMLParser(true).parseText('"
				+ test + "').length"));
		System.out
				.println("###"
						+ engine
								.eval("new XMLParser(true).parseText('2${..}2').join('/')"));
		Assert.assertEquals("2${..}2", engine
				.eval("new XMLParser(true).parseText('2${..}2').join('')"));
	}

	@Test
	public void testFindELEnd() throws Exception {
		Assert.assertEquals(16, engine
				.eval("findELEnd(\"${'jin '+'dawei'}\",1)"));
		Assert.assertEquals(5, engine.eval("findELEnd('${123}xxx',1)"));
		Assert.assertEquals(6, engine.eval("findELEnd('x${123}xxx',2)"));
	}

	@Test
	public void testClasspath() throws Exception {
		String obj = (String)engine.eval("$import('org.xidea.lite.impl:Template');new Template('classpath:///org/xidea/lite/test/input.xml').render({})");
		Assert.assertTrue(obj.startsWith("<!DOCTYPE html PUBLIC"));
		System.out.println(obj);
	}

	@Test
	public void testExample() throws Exception {
		Document doc = context.loadXML(menuURL);
		String defaultContext = getText(doc, "/root/context");

		DocumentFragment node = context.selectNodes(doc, "/root/entry");
		Element child = (Element) node.getFirstChild();
		while (child != null) {
			String key = child.getAttribute("key");
			String source = getText(child, "source");
			String context = getText(child, "context");
			context = context == null ? defaultContext : context;

			System.out.println(context);
			// engine.put(ScriptEngine.FILENAME, "<file>");

			doTestItem(key, source,  context);

			child = (Element) child.getNextSibling();
		}
	}

	@Test
	public void test3op() throws Exception{
		testEL("1?1:3 + 0?5:7");
		testEL("1?0?5:7:3 ");
		testEL("0?0?5:7:3 ");
		testEL("1?0?5:0?11:13:3");
		testEL("1?1?0?5:0?11:13:3?1?0?5:0?11:13:3:0?11:13:3");
	}

	private void testEL(String el) throws Exception {
		doTestItem(el,"<xml>${"+el+"}</xml>","{}");
	}

	private void doTestItem(String key, String source,
			String contextJSON) throws ScriptException,
			SAXException, IOException {
		System.out.println("\n======" + key + "======\n");
		String sourceJSON = JSONEncoder.encode(source);
		engine.eval("var jsTemplate = new Template(" + sourceJSON
				+ ",new XMLParser(true,'"+menuURL+"'))");

		engine.eval("var liteTemplate = new Template(" + sourceJSON
				+ ",new XMLParser(false,'"+menuURL+"'))");
		// .buildResult()");

		System.out.println(engine.eval("jsTemplate.data+''"));
//			System.out.println(contextJSON);
		// System.out.println(engine.eval("liteTemplate.data+''"));
		Object jsJSON = engine.eval("liteTemplate.render(" + contextJSON
				+ ")");
		Object jsJS = engine.eval("jsTemplate.render(" + contextJSON + ")");
		Assert.assertEquals("JS编译后结果不一致"+source, jsJSON, jsJS);
		ParseContextImpl pc = TestUtil.buildParseContext(menuURL);
		source = source.replace("=\"menu.xml\"", "=\""+menuURL+"\"");
		System.out.println(source);
		pc.parse(ParseUtil.loadXML(source));
		StringWriter out = new StringWriter();
		new Template(pc.toList()).render(JSONDecoder.decode(contextJSON),
				out);
		String java = out.toString();
		Assert.assertEquals("JS结果与Java不一致", sumText((String) jsJSON),
				sumText(java));
	}

	private String sumText(String java) {
		java = java.replace("&gt;", "&#62;");
		java = java.replace("&lt;", "&#60;");
		java = java.replace("&amp;", "&#38;");
		java = java.replaceAll("[\r\\\n]", " ");
		return java;
	}

}
