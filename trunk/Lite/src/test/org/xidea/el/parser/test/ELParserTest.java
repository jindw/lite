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
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.ExpressionImpl;
import org.xidea.el.json.JSONEncoder;

public class ELParserTest {

	protected Map<String, Object> context;
	protected Map<String, String> elResultMap;
	ExpressionFactory factory = ExpressionFactoryImpl.getInstance();

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
		if(result.trim().startsWith("[")){
			Object el = factory.parse(text);
			String eljson = JSONEncoder.encode(el);
			Assert.assertEquals(result, eljson);
		}else{
			Assert.assertEquals(
					result,
					String.valueOf(new ExpressionImpl(text).evaluate(this))
					);
			
		}
	}

}
