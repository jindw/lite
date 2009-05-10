package org.xidea.lite.js.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
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
import org.w3c.dom.NodeList;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.dtd.DefaultEntityResolver;
import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class JSCompileTest {
	ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");

	File webRoot = new File(new File(JSCompileTest.class.getResource("/")
			.getFile()), "../../");
	ParseContextImpl context;

	public JSCompileTest() throws MalformedURLException {
		context = new ParseContextImpl(webRoot.toURI().toURL());
	}

	public String getText(Node doc, String xpath)
			throws XPathExpressionException {
		Node node = context.selectNodes(xpath, doc).getFirstChild();
		return node == null ? null : node.getTextContent();
	}

	@Before
	public void setup() throws Exception, ScriptException {
		engine.eval(new InputStreamReader(this.getClass().getResourceAsStream(
				"/boot.js"), "utf-8"));
		engine.eval("$import('org.xidea.lite:Template');");
		engine.eval("$import('org.xidea.lite:buildNativeJS');");
		engine.eval("$import('org.xidea.lite:XMLParser');");
		engine.eval("$import('org.xidea.lite:findELEnd')");
		engine.eval(new InputStreamReader(JSCompileTest.class
				.getResourceAsStream("JSCompileTest.js"), "utf-8"));
	}
	@Test
	public void testTextParser() throws Exception {
		String test = "您好：${user}，您的的ip地址是：${ippart0}  .${ip.part1}.${ip.part2}.${ip.part3}。";
		
		Assert.assertEquals(11,engine.eval("new XMLParser(true).parseText('"+test+"').length"));
		System.out.println("###"+engine.eval("new XMLParser(true).parseText('2${..}2').join('/')"));
		Assert.assertEquals("2${..}2",engine.eval("new XMLParser(true).parseText('2${..}2').join('')"));
	}
	@Test
	public void testFindELEnd() throws Exception {
		Assert.assertEquals(16,engine.eval("findELEnd(\"${'jin '+'dawei'}\",1)"));
		Assert.assertEquals(5,engine.eval("findELEnd('${123}xxx',1)"));
		Assert.assertEquals(6,engine.eval("findELEnd('x${123}xxx',2)"));
	}
	@Test
	public void testExample() throws Exception {
		URL menuURL = new File(webRoot, "menu.xml").toURI().toURL();
		Document doc = context.loadXML(menuURL);
		String defaultContext = getText(doc, "/root/context");
		engine.getContext().setAttribute("testMenuDoc", doc,
				ScriptContext.ENGINE_SCOPE);
		engine
				.eval("XMLHttpRequest['menu.xml'] = {responseXML:testMenuDoc,'#getResponseHeader':{'Content-Type':'text/xml'}}");
		DocumentFragment node = context.selectNodes("/root/entry", doc);
		Element child = (Element) node.getFirstChild();
		while (child != null) {
			String key = child.getAttribute("key");
			String source = getText(child, "source");
			String context = getText(child, "context");
			context = context == null ? defaultContext : context;

			String sourceJSON = JSONEncoder.encode(source);
			String contextJSON = context;

			engine.eval("var jsTemplate = new Template(" + sourceJSON
					+ ",new XMLParser(true))");
			engine.eval("var liteTemplate = new Template(" + sourceJSON
					+ ",new XMLParser(false))");
			// .buildResult()");

			System.out.println("\n======" + key + "======\n");
			System.out.println(engine.eval("jsTemplate.data+''"));
			// System.out.println(engine.eval("liteTemplate.data+''"));
			Object jsJSON = engine.eval("liteTemplate.render(" + contextJSON
					+ ")");
			Object jsJS = engine.eval("jsTemplate.render(" + contextJSON
					+ ")");
			Assert.assertEquals("JS编译后结果不一致", jsJSON, jsJS);
			ParseContextImpl pc = new ParseContextImpl(menuURL);
			pc.parse(DOMParser.parseFromString(source, ""));
			StringWriter out = new StringWriter();
			new Template(pc.toList()).render(JSONDecoder.decode(contextJSON),
					out);
			String java = out.toString();
			Assert.assertEquals("JS结果与Java不一致", sumText((String)jsJSON), sumText(java));

			child = (Element) child.getNextSibling();
		}
	}

	private String sumText(String java) {
		java = java.replace("&gt;", "&#62;");
		java = java.replace("&lt;", "&#60;");
		java = java.replace("&amp;", "&#38;");
		java = java.replaceAll("[\r\\\n]", " ");
		return java;
	}

	public static class DOMParser {
		private static DocumentBuilder documentBuilder;

		static {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				factory.setNamespaceAware(true);
				factory.setValidating(false);
				// factory.setExpandEntityReferences(false);
				factory.setCoalescing(false);
				// factory.setXIncludeAware(true);
				documentBuilder = factory.newDocumentBuilder();
				documentBuilder.setEntityResolver(new DefaultEntityResolver());
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			}
		}

		public static Document parseFromString(String text, String contentType)
				throws SAXException, IOException, ParserConfigurationException {
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(text));
			return documentBuilder.parse(is);
		}
	}

	public static class XPathEvaluator {
		private NodeList nodeList;
		private int index;

		// .evaluate(xpath, currentNode, function(prefix){return nsMap[prefix]},
		// 5, null);
		public Object evaluate(String path, Node node, Object map, int type,
				Object x) throws XPathExpressionException {
			XPath xpath = XPathFactory.newInstance().newXPath();
			this.nodeList = (NodeList) xpath.evaluate(path, node,
					XPathConstants.NODESET);
			this.index = 0;
			return this;
		}

		public Node iterateNext() {
			if (index < nodeList.getLength()) {
				return nodeList.item(index++);
			}
			return null;
		}
	}
}
