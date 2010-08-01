package org.xidea.lite.parser.impl.test;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.security.auth.login.FailedLoginException;
import javax.xml.parsers.DocumentBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xidea.lite.impl.ParseUtil;

public class XMLFixedTest  {

	public XMLFixedTest() {
	}
	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void test() throws Exception{
		Field field = ParseUtil.class.getDeclaredField("documentBuilder");
		field.setAccessible(true);
		Assert.fail();
//		Document doc = new XMLFixerImpl().parse((DocumentBuilder) field.get(null),
//				new ByteArrayInputStream("<br><br>".getBytes()), "/uri");
//		System.out.println(doc.toString());
	}

}
