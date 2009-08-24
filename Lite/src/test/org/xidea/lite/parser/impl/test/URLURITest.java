package org.xidea.lite.parser.impl.test;


import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.xidea.el.impl.ReflectUtil;


public class URLURITest {

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testURIFile(){
		File f = new File("C:/1.txt");;
		System.out.println(f.toURI());
		System.out.println(ReflectUtil.map(f.toURI()));
		System.out.println(ReflectUtil.map(new File(".").toURI()));
	}

}
