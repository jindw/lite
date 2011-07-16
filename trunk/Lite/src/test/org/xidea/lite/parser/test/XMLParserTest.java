package org.xidea.lite.parser.test;

import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.NodeList;
import org.xidea.lite.Template;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.test.LiteTestUtil;


public class XMLParserTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testSelect() throws Exception {
		URI url = this.getClass().getResource("include-test.xml").toURI();
		org.w3c.dom.Document doc = ParseUtil.loadXMLBySource(
				ParseUtil.loadTextAndClose(ParseUtil.openStream(url), null)
				,url.toASCIIString());
		
		
		NodeList node = ParseUtil.selectByXPath(doc,"//xhtml:body");
		Assert.assertTrue(node.getLength()==1);
	}
	
	
}
