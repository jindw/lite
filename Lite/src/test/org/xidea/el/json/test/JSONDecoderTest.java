package org.xidea.el.json.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.xidea.el.impl.ExpressionImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

public class JSONDecoderTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testEncodeObject() throws IOException {
		//非JSON标准,注释,多行
		assertEquals("a\nb",JSONDecoder.decode("/**/\"a\nb\""));
		assertEquals("\"a\\nb\"",JSONEncoder.encode("a\nb"));
		assertEquals(-1,JSONDecoder.decode("-1"));
		assertEquals(-1.1,JSONDecoder.decode("-1.1"));
		assertEquals(-0xFF1,JSONDecoder.decode("-0xFF1"));

		assertEquals(1,JSONDecoder.decode("1"));
		assertEquals(1.1,JSONDecoder.decode("1.1"));
		assertEquals(0xFF1,JSONDecoder.decode("0xFF1"));
		
	}

	@Test
	public void testJSEL() throws IOException {
		Object o = new ExpressionImpl("{key:'value',n:-1}").evaluate(null);
		System.out.println(o);
		assertEquals(JSONDecoder.decode("{\"key\":\"value\",\"n\":-1}"), o);
	}


}
