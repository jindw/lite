package org.xidea.lite.test.oldcases;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.LiteTemplate;
import org.xidea.lite.Template;

public class HTMLParserTest {
	

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSelect() throws Exception {
		Template t = new LiteTemplate(LiteTestUtil.parse("<select/>"),LiteTestUtil.defaultFeatureMap);
		StringWriter out = new StringWriter();
		t.render(new HashMap<Object, Object>(), out);
		Assert.assertEquals("<select></select>", out.toString());
	}
}
