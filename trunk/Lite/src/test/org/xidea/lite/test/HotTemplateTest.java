package org.xidea.lite.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.impl.HotTemplateEngine;
import org.xidea.lite.impl.ParseConfigImpl;

public class HotTemplateTest {
	@Test
	public void testConfig() throws URISyntaxException, IOException{
		URI uri = this.getClass().getResource("/").toURI();
		File root = new File(new File(uri),"../../").getCanonicalFile();
		File config = new File(root,"WEB-INF/lite.xml");
		System.out.println(root);
		HotTemplateEngine ht = new HotTemplateEngine(new ParseConfigImpl(root.toURI(), config.toURI()),null);
		StringWriter out = new StringWriter();
		ht.getTemplate("/example/test.xhtml").render(new Object(),out);
		System.out.println(out);
		new File(root,"WEB-INF/litecode/example/test.xhtml").delete();
	}
	@Test
	public void testA(){
		String t = "\\\"\\\"";
		System.out.println(t);
		System.out.println(JSONEncoder.encode(t));
	}
	@Test
	public void testCache() throws URISyntaxException, IOException{
		String path = "org/xidea/lite/test/input.xml";
		URI root = this.getClass().getResource("/").toURI();
		System.out.println(root);
		HotTemplateEngine ht = new HotTemplateEngine(new ParseConfigImpl(root, null),null);
		cacheTest(path, ht);
		ht = new HotTemplateEngine(new ParseConfigImpl(URI.create("classpath:///"),null),null);
		cacheTest(path, ht);
	}
	

	@Test
	public void testWeb() throws URISyntaxException, IOException{
		//String path = "/test/client-optimize/test-call-closure.xhtml";
		String path = "/example/test.xhtml";
		URI root = this.getClass().getResource("/").toURI().resolve("../../");
		System.out.println(root);
		HotTemplateEngine ht = new HotTemplateEngine(new ParseConfigImpl(root, null),null);
		cacheTest(path, ht);
		StringWriter out = new StringWriter();
		ht.render(path, new Object(), out);
		System.out.println(out);
	}

	private void cacheTest(String path, HotTemplateEngine ht) throws IOException {
		Template t1 = ht.getTemplate(path);
		Template t2 = ht.getTemplate(path);
		t2 = ht.getTemplate(path);
		t2 = ht.getTemplate(path);
		Assert.assertTrue("缓存生效时两者应该是同一个模板实现",t1==t2);
	}

}
