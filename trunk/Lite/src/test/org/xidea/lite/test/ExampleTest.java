package org.xidea.lite.test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.JSIText;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.Template;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseContext;
import org.xml.sax.SAXException;

import sun.org.mozilla.javascript.internal.Context;

public class ExampleTest {
	ScriptEngine engine;
	JSIRuntime rt = RuntimeSupport.create();
	
	URI menuURL;
	File webRoot = new File(new File(ExampleTest.class.getResource("/")
			.getFile()), "../../");
	ParseContext context;

	public ExampleTest() throws MalformedURLException {
		context = LiteTestUtil.buildParseContext(webRoot.toURI());
	}

	public String getText(Node doc, String xpath)
			throws XPathExpressionException {
		NodeList node = ParseUtil.selectNodes(doc, xpath);
		if(node == null || node.getLength()<1){
			return null;
		}
		return node.item(0).getTextContent();
	}
	Object eval(String source) throws ScriptException{
		if(engine!=null){
			return engine.eval(source);
		}else{
			return rt.eval(source);
		}
	}

	@Before
	public void setup() throws Exception, ScriptException {

		menuURL = new File(webRoot, "menu.xml").toURI();
		if((Boolean)eval("!window.$JSI")){
			eval("this['javax.script.filename']='<boot.js>';");
			eval(JSIText.loadText(this.getClass().getResourceAsStream(
					"/boot.js"), "utf-8"));
		}
		
		Context.enter().getWrapFactory().setJavaPrimitiveWrap(false);
		//engine = new ScriptEngineManager().getEngineByExtension("js");
		//engine.eval("evalFile.call(null,'print(this)',111)");
		eval("$import('org.xidea.lite.impl.js:*');");
		eval("$import('org.xidea.lite.impl:*');");
		eval("$import('org.xidea.lite.parse:*');");
		eval("$import('org.xidea.el:findELEnd')");
	}

	@Test
	public void testTextParser() throws Exception {
		String test = "您好：${user}，您的的ip地址是：${ippart0}  .${ip.part1}.${ip.part2}.${ip.part3}。";

		Assert.assertEquals(11, eval("new ParseContext().parseText('"
				+ test + "',0).length"));
		System.out
				.println("###"
						+ eval("new ParseContext().parseText('2${..}2',0).join('/')"));
		Assert.assertEquals("3${...}2", eval("new ParseContext().parseText('3${...}2',0).join('')"));
	}

	@Test
	public void testFindELEnd() throws Exception {
		Assert.assertEquals(16, eval("findELEnd(\"${'jin '+'dawei'}\",1)"));
		Assert.assertEquals(5, eval("findELEnd('${123}xxx',1)"));
		Assert.assertEquals(6, eval("findELEnd('x${123}xxx',2)"));
	}

	@Test
	public void testClasspath() throws Exception {
		String obj = (String)eval("new TemplateImpl('classpath:///org/xidea/lite/test/input.xml').render({})");
		Assert.assertTrue(obj.startsWith("<!DOCTYPE html PUBLIC"));
		System.out.println(obj);
	}

	@Test
	public void testExample() throws Exception {
		Document doc = context.loadXML(menuURL);
		String defaultContext = getText(doc, "/root/context");

		NodeList nodes = ParseUtil.selectNodes(doc, "/root/entry");
		System.out.println(nodes);
		for (int i = 0;i<nodes.getLength();i++) {
			Element child = (Element) nodes.item(i);
			String key = child.getAttribute("key");
			String source = getText(child, "source");
			String context = getText(child, "context");
			context = context == null ? defaultContext : context;
			// engine.put(ScriptEngine.FILENAME, "<file>");
			doTestItem(key, source,  context);
		}
	}


	private void doTestItem(String key, String source,
			String contextJSON) throws ScriptException,
			SAXException, IOException {
		String sourceJSON = JSONEncoder.encode(source);
		System.out.println("\n======" + key +"/"+ sourceJSON +"/"+contextJSON+ "======\n");
		String base = JSONEncoder.encode(menuURL.toString());
		eval("var jsTemplate = new TemplateImpl(" + sourceJSON
				+ ",new ParseContext(new ParseConfig("+base+"),"+base+"))");

		eval("var liteTemplate = new TemplateImpl(" + sourceJSON
				+ ",new ParseContext(new ParseConfig("+base+"),"+base+"),true)");
		// .buildResult()");

		System.out.println(eval("jsTemplate.data+''"));
		System.out.println("lite:"+eval("liteTemplate.data+''"));
//			System.out.println(contextJSON);
		// System.out.println(engine.eval("liteTemplate.data+''"));
		Object jsJSON = eval("liteTemplate.render(" + contextJSON
				+ ")");
		Object jsJS = eval("jsTemplate.render(" + contextJSON + ")");
		Assert.assertEquals("JS编译前后结果不一致"+source, jsJSON, jsJS);
		ParseContext pc = LiteTestUtil.buildParseContext(menuURL);
		source = source.replace("=\"menu.xml\"", "=\""+menuURL+"\"");
		System.out.println(source);
		pc.parse(ParseUtil.loadXML(source));
		StringWriter out = new StringWriter();
		System.out.println(JSONEncoder.encode(pc.toList()));
		new Template(pc.toList()).render(JSONDecoder.decode(contextJSON),
				out);
		String java = out.toString();
		Assert.assertEquals("JS结果与Java不一致:"+key, sumText((String) jsJSON),
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
