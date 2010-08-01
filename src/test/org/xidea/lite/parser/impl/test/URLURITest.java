package org.xidea.lite.parser.impl.test;


import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.test.LiteTestUtil;


public class URLURITest {

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testResourceContext() throws Exception{
		String base = "http://lh:8080/test";
		String base2 = "lite:///";
		ParseContext rc =LiteTestUtil.buildParseContext(new URI(base));
		Assert.assertEquals(base2+"test.xml", rc.createURI("test.xml").toString());
		Assert.assertEquals(base2+"test.xml", rc.createURI("./test.xml").toString());
		
		base = "http://lh:8080/test/aa/bb/";
		rc = LiteTestUtil.buildParseContext(new URI(base));
		rc.setCurrentURI(new URI(base));
		System.out.println(rc.getCurrentURI());
		Assert.assertEquals("http://lh:8080/test/aa/bb/test.xml", rc.createURI("test.xml").toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../../test.xml").toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../.././test.xml").toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI(".././../test.xml").toString());
//		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../././../test.xml").toString());
		
	
	}
	@Test
	public void testURIRelative() throws Exception{
		System.out.println(new File(".").toURI().getPath());
		String t = "classpath:///aa/bb/cc/.././../dd";
		System.out.println(new URI(t).normalize().getPath());
		System.out.println(new URI(t).resolve("/1.xx"));
		System.out.println(new URI(t).resolve("xx:./test.x"));
	}
	@Test
	public void testURIFile(){
		File f = new File("C:/1.txt");;
		System.out.println(f.toURI());
		System.out.println(ReflectUtil.map(f.toURI()));
		System.out.println(ReflectUtil.map(new File(".").toURI()));
	}

	@Test
	public void test1() throws Exception {
		URI u = new URI("lite:///");
		System.out.println(new File("c:/x/^.x").toURI());
		System.out.println(new URI("c",null,"/x/^.x",null));
		System.out.println(u.resolve("/111.xx"));
		System.out.println(u.resolve(".111.xx#23"));
		System.out.println(u.resolve(URLEncoder.encode("^111.xx#23","UTF-8")));
		System.out.println(u.resolve("%5E111.xx"));
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
