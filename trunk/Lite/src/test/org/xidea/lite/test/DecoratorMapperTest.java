package org.xidea.lite.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.parser.DecoratorContext;
import org.xidea.lite.parser.impl.DecoratorContextImpl;

public class DecoratorMapperTest {
	DecoratorContext mapper;

	@Before
	public void setUp() throws Exception {
		File file = new File(this.getClass().getResource(
		"decorators.xml").getFile());
		mapper = new DecoratorContextImpl(file.toURI());
	}

	@Test
	public void testDecoratorExist() {
		assertEquals("/test1/template.xhtml", mapper
				.getDecotatorPage("/test1/xxx.action"));
		assertEquals("/test1/template.xhtml", mapper
				.getDecotatorPage("/test1/xxxd..d.action"));
		assertEquals("/test1/template.xhtml", mapper
				.getDecotatorPage("/test1/login2.action"));
		assertEquals("/test2/template.xhtml", mapper
				.getDecotatorPage("/test2/login2.action"));
		assertEquals("/test3/template.xhtml", mapper
				.getDecotatorPage("/test3/login2.action"));
	}

	@Test
	public void testGetDecotatorNotExist() {
		assertNull( mapper
				.getDecotatorPage("/test1/login.action"));
		assertNull( mapper
				.getDecotatorPage("/test2/xxx/dd.action"));
	}

}
