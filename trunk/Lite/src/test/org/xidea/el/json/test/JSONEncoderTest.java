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
		new JSONEncoder(true,10,true,true).encode(new Object(),out);
		assertEquals("{\"class\":\"java.lang.Object\"}", out.toString());
	}

}
