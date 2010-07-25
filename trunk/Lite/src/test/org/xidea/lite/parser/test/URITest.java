package org.xidea.lite.parser.test;

import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.Template;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.old.TextNodeParser;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.test.LiteTestUtil;

public class URITest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test1() throws Exception {
		test("lite:///xxx/s","lite:/xxx/s/222");
		test("lite:///xxx/s3/4","lite:/xxx/s2/222");
	}
	void test(String uri1,String uri2) throws URISyntaxException{
		URI u = new URI(uri1).relativize(new URI(uri2));
		System.out.println(uri1+"---"+uri2);
		System.out.println(u);
		System.out.println(u.getPath());
	}

}
