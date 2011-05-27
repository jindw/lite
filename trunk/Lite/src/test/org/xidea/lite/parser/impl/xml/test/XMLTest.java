package org.xidea.lite.parser.impl.xml.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.impl.XMLNormalizeImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLTest {
	private static DocumentBuilder DB;
	static{
		DocumentBuilder db = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			
			db = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DB = db;
	}
	@Test
	public void testSpec() throws SAXException, IOException{
		InputSource is = new InputSource(new StringReader("<xml c='0' a='1' b='2'/>"));
		Document doc = DB.parse(is);
		doc.getDocumentElement().setAttribute("b1", "1");
		NamedNodeMap as = doc.getDocumentElement().getAttributes();
		
		for (int i = 0; i < as.getLength(); i++) {
			Node attr = as.item(i);
			
			System.err.println(attr.getPreviousSibling());
			System.err.println(attr.getNodeName());
		}
		System.err.println(doc.getDocumentElement());
		
	}

}
