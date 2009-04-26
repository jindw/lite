package org.xidea.lite.parser.test;

import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.Template;
import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xidea.lite.parser.impl.TextParser;

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
		TextParser p = new TextParser();
		ParseContextImpl context = new ParseContextImpl(new URL("http://localhost:8080/"));
		p.parse(context, null, text);
		List<Object> insts = context.toList();
		Template t = new Template(insts);
		Writer out = new StringWriter();
		t.render(new HashMap<Object, Object>(), out);
		Assert.assertEquals(result, out.toString());
	}

}
