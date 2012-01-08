package org.xidea.lite.test.oldcases;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.LiteTemplate;
import org.xidea.lite.parse.ParseContext;

public class XMLTemplateTest {
	private File base;

	@Before
	public void setup() throws URISyntaxException {
		this.base = new File(new File(XMLTemplateTest.class.getResource("/")
				.toURI()), "../../");
	}

	public Object getTemplate(URL resource) throws Exception {
		ParseContext context = LiteTestUtil.buildParseContext(null);
		context.parse(resource.toString());
		return new LiteTemplate(context.toList(),context.getFeatureMap());
	}

	@Test
	public void testCompileTime() throws Exception {
		long n1 = System.nanoTime();
		getTemplate(XMLTemplateTest.class.getResource("skin-list.xhtml"));
		System.out.println(System.nanoTime() - n1);
	}
}
