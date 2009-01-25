package org.xidea.lite.test;

import java.io.StringWriter;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.Template;
import org.xidea.lite.parser.XMLParser;

public class ELTest {

	XMLParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new XMLParser();
	}

	public void test(String template, String result) throws Exception {
		Template t = new Template(parser.parse(template));
		StringWriter out = new StringWriter();
		t.render(new HashMap<Object, Object>(), out);
		Assert.assertEquals(result, out.toString());
	}

	@Test
	public void testEL1() throws Exception {
		test("<div>${1+1}</div>", "<div>2</div>");
		test("<div>x${1+1}</div>", "<div>x2</div>");
		test("<div>{1+1}</div>", "<div>{1+1}</div>");
		test("<div>x{1+1}</div>", "<div>x{1+1}</div>");
	}
}
