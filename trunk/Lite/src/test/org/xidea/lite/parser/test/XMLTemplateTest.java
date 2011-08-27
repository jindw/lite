package org.xidea.lite.parser.test;

import java.io.File;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.Template;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.test.LiteTestUtil;

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
		return new Template(context.toList(),context.getFeatureMap());
	}

	@Test
	public void testCompileTime() throws Exception {
		long n1 = System.nanoTime();
		getTemplate(XMLTemplateTest.class.getResource("skin-list.xhtml"));
		System.out.println(System.nanoTime() - n1);
	}
}
