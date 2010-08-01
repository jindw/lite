package org.xidea.lite.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.lite.Template;
import org.xidea.lite.impl.HotTemplateEngine;
import org.xidea.lite.impl.ParseConfigImpl;

public class HotTemplateTest {
	@Test
	public void testCache() throws URISyntaxException, IOException{
		String path = "org/xidea/lite/test/input.xml";
		URI root = this.getClass().getResource("/").toURI();
		System.out.println(root);
		HotTemplateEngine ht = new HotTemplateEngine(new ParseConfigImpl(root, null));
		cacheTest(path, ht);
		ht = new HotTemplateEngine(new ParseConfigImpl(URI.create("classpath:///"),null));
		cacheTest(path, ht);
	}

	private void cacheTest(String path, HotTemplateEngine ht) throws IOException {
		Template t1 = ht.getTemplate(path);
		Template t2 = ht.getTemplate(path);
		t2 = ht.getTemplate(path);
		t2 = ht.getTemplate(path);
		Assert.assertTrue("缓存生效时两者应该是同一个模板实现",t1==t2);
	}

}
