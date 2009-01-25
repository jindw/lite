package org.xidea.lite.test;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.Template;
import org.xidea.lite.parser.XMLParser;

public class HTMLParserTest {
	
	XMLParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new XMLParser();
	}

	@Test
	public void testSelect() throws Exception {
		Template t = new Template(parser.parse("<select/>"));
		StringWriter out = new StringWriter();
		t.render(new HashMap<Object, Object>(), out);
		Assert.assertEquals("<select></select>", out.toString());
	}
}
