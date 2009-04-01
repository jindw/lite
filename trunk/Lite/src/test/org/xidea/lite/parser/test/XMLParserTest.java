package org.xidea.lite.parser.test;

import java.net.URL;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.ParseContextImpl;
import org.xidea.lite.parser.XMLParser;


public class XMLParserTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testSelect() throws Exception {
		URL url = this.getClass().getResource("include-test.xml");
		ParseContextImpl context = new ParseContextImpl(url); 
		org.w3c.dom.Document doc = new XMLParser().loadXML(url, context);
		
		DocumentFragment node = new XMLParser().selectNodes("//xhtml:body",doc);
		Assert.assertTrue(node.getChildNodes().getLength()==1);
	}

}
