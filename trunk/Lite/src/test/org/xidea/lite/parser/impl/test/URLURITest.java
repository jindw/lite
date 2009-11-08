package org.xidea.lite.parser.impl.test;


import java.io.File;
import java.net.URI;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.lite.parser.impl.ResourceContextImpl;


public class URLURITest {

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testResourceContext() throws Exception{
		String base = "http://lh:8080/test";
		ResourceContextImpl rc = new ResourceContextImpl(new URI(base));
		Assert.assertEquals(base+".xml", rc.createURI("test.xml", null).toString());
		Assert.assertEquals(base+".xml", rc.createURI("./test.xml", null).toString());
		
		base = "http://lh:8080/test/aa/bb/";
		rc = new ResourceContextImpl(new URI(base));
		System.out.println(ReflectUtil.map(new URI("classpath:///aa/bb")));
		Assert.assertEquals(base+"test.xml", rc.createURI("test.xml", null).toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../../test.xml", null).toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../.././test.xml", null).toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI(".././../test.xml", null).toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../././../test.xml", null).toString());
		
	
	}
	@Test
	public void testURIRelative() throws Exception{
		String t = "classpath:aa/bb/cc/../../dd";
		System.out.println(new URI(t));
		t = t.replaceFirst("([^\\/]+\\/\\.)?\\.\\/", "");
		System.out.println(new URI(t));
	}
	@Test
	public void testURIFile(){
		File f = new File("C:/1.txt");;
		System.out.println(f.toURI());
		System.out.println(ReflectUtil.map(f.toURI()));
		System.out.println(ReflectUtil.map(new File(".").toURI()));
	}

}
