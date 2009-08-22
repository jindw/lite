package org.xidea.el.parser.test;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.parser.ExpressionTokenizer;
import org.xidea.el.parser.Tokens;

public class ELParserTest {

	protected Map<String, Object> context;
	protected Map<String, String> elResultMap;

	@Before
	public void setUp() throws Exception {
		XMLDecoder de = new XMLDecoder(this.getClass().getResourceAsStream(
				this.getClass().getSimpleName() + ".xml"));
		this.context = (Map<String, Object>) de.readObject();
		this.elResultMap = (Map<String, String>) de.readObject();
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
	public void runFile() throws Exception {
		System.out.println(new File("d:/workspace/", "d:/1.txt"));
	}

	@Test
	public void runTest() throws Exception {
		for (String key : elResultMap.keySet()) {
			String value = elResultMap.get(key);
			test(key, value);
		}
	}

	public void test(String text, String result) throws Exception {
		ExpressionTokenizer tokenizer = new ExpressionTokenizer(text);
		Tokens el = tokenizer.getTokens();
		String eljson = JSONEncoder.encode(el);
		Assert.assertEquals(result, eljson);
	}

}
