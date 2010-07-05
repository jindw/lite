package org.xidea.lite.parser.impl.test;


import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xidea.lite.impl.XMLContextImpl;
import org.xidea.lite.impl.XMLFixerImpl;

public class XMLFixedTest extends XMLContextImpl {

	public XMLFixedTest() {
		super(null);
	}
	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void test() throws IOException{
		Document doc = new XMLFixerImpl().parse(documentBuilder, 
				new ByteArrayInputStream("<br><br>".getBytes()), "/uri");
		System.out.println(doc.toString());
	}

}
