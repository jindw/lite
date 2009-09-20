package org.xidea.lite.test;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.Template;
import org.xidea.lite.parser.test.XMLParser;

public class HTMLParserTest {
	

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSelect() throws Exception {
		Template t = new Template(new XMLParser( ).parse("<select/>"));
		StringWriter out = new StringWriter();
		t.render(new HashMap<Object, Object>(), out);
		Assert.assertEquals("<select></select>", out.toString());
	}
}
