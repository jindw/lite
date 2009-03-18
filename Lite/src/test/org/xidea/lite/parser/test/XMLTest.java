package org.xidea.lite.parser.test;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.Template;
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
	public void runTest() throws Exception {
		runTest("XMLTest");
		runTest("VarTest");
		runTest("AutoFormTest");
	}
	public void runTest(String file) throws Exception {
		XMLDecoder de = new XMLDecoder(this.getClass().getResourceAsStream(
				file + ".xml"));
		this.context = (Map<String, Object>) de.readObject();
		this.templateResultMap = (Map<String, String>) de.readObject();
		for (String key : templateResultMap.keySet()){
			String value = templateResultMap.get(key);
			test(key, value);
		}
	}

	public void test(String text, String result) throws Exception {
		XMLParser p = new XMLParser();
		List<Object> insts = p
				.parse("<div xmlns:c=\"http://www.xidea.org/ns/template/core\">"
						+ text + "</div>");
		Template t = new Template(insts);
		Writer out = new StringWriter();
		t.render(context, out);
		Assert.assertEquals("<div>" + result + "</div>", out.toString());
	}

}
