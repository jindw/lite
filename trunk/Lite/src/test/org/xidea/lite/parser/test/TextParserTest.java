package org.xidea.lite.parser.test;

import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xidea.lite.Template;
import org.xidea.lite.parser.AbstractParser;
import org.xidea.lite.parser.TextParser;
import org.xml.sax.InputSource;

public class TextParserTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test1() throws Exception {
		HashMap<String, String> testCase = new LinkedHashMap<String, String>();
		testCase.put("${1+1}", "2");
		testCase.put("${}", "${}");
		testCase.put("${''}", "");

		for (String key : testCase.keySet()) {
			test(key, testCase.get(key));
			test("${" + key, "${" + testCase.get(key));
			test(key + "${", testCase.get(key) + "${");
			test("${" + key + "${", "${" + testCase.get(key) + "${");
		}
	}

	public void test(String text, String result) throws Exception {
		AbstractParser p = new TextParser();
		List<Object> insts = p.parse(text);
		Template t = new Template(insts);
		Writer out = new StringWriter();
		t.render(new HashMap<Object, Object>(), out);
		Assert.assertEquals(result, out.toString());
	}

}
