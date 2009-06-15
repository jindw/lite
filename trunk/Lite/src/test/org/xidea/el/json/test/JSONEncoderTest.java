package org.xidea.el.json.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.xidea.el.impl.ExpressionImpl;
import org.xidea.el.json.JSONEncoder;

public class JSONEncoderTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testEncodeObject() throws IOException {
		assertEquals("{}", JSONEncoder.encode(new Object()));
		StringWriter out = new StringWriter();
		new JSONEncoder(true,10).encode(new Object(),out,null);
		assertEquals("{\"class\":\"java.lang.Object\"}", out.toString());
	}

	@Test
	public void testJSEL() throws IOException {
		Object o = new ExpressionImpl("{key:'value'}").evaluate(null);
		System.out.println(o);
		//assertEquals("{\"class\":\"java.lang.Object\"}", out.toString());
	}


}
