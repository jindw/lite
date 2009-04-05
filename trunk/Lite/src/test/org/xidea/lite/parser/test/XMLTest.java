package org.xidea.lite.parser.test;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.parser.HTMLFormNodeParser;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.ParseContextImpl;
import org.xidea.lite.parser.TextParser;
import org.xidea.lite.parser.XMLParser;

public class XMLTest {
	protected Map<String, Object> context;
	protected Map<String, String> templateResultMap;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void print() throws Exception {
		HashMap<String, Object> context = new LinkedHashMap<String, Object>();
		context.put("1", new String[] { "1", "2" });
		XMLEncoder encoder = new XMLEncoder(System.out);
		encoder.writeObject(context);
		encoder.flush();
	}

	@Test
	public void runELTest() throws Exception {
		runTest("ELTest");
	}
	@Test
	public void runXMLTest() throws Exception {
		runTest("XMLTest");
	}

	@Test
	public void runCoreTest() throws Exception {
		runTest("CoreTest");
	}

	@Test
	public void runVarTest() throws Exception {
		runTest("VarTest");
	}

	@Test
	public void runHTMLTest() throws Exception {
		runTest("HTMLTest");
	}

	@Test
	public void runAutoFormTest() throws Exception {
		runTest("AutoFormTest");
	}

	public void runTest(String file) throws Exception {
		XMLDecoder de = new XMLDecoder(this.getClass().getResourceAsStream(
				file + ".xml"));
		this.context = (Map<String, Object>) de.readObject();
		System.out.println(JSONEncoder.encode(this.context));
		this.templateResultMap = (Map<String, String>) de.readObject();
		int i=0;
		for (String key : templateResultMap.keySet()) {
			String value = templateResultMap.get(key);
			test(i++,key, value);
		}
	}

	public void test(int index,String text, String result) throws Exception {
		XMLParser p = new XMLParser();
		ParseContextImpl parseContext = new ParseContextImpl(this.getClass()
				.getResource("/"));
		parseContext.setCompress(true);
		parseContext.setAttribute(HTMLFormNodeParser.class, HTMLFormNodeParser.AUTO_IN_FORM);
		List<Object> insts = p.parse(
				"<div xmlns:c=\"http://www.xidea.org/ns/template/core\">"
						+ text + "</div>", parseContext);
		System.out.println(JSONEncoder.encode(insts));
		Template t = new Template(insts);
		Writer out = new StringWriter();
		t.render(new HashMap(context), out);
		assertXMLEquals("第"+index+"个测试错误："+text+":\n"+result,"<div>" + result + "</div>", out.toString());
	}

	public static void assertXMLEquals(String msg,String expected, String acture) {
		Assert.assertEquals(msg,restring(expected), restring(acture));
	}

	public static String restring(String xml) {
		try {
			Transformer transformer = javax.xml.transform.TransformerFactory
					.newInstance().newTransformer();
			StringWriter out = new StringWriter();
			Result outputTarget = new javax.xml.transform.stream.StreamResult(
					out);
			//transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new StreamSource(new StringReader(xml)),
					outputTarget);
			return out.toString().replaceAll("(\\s)+", "");
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
