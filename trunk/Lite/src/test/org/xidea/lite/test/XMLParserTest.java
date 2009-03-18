package org.xidea.lite.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.xidea.el.ExpressionFactoryImpl;
import org.xidea.lite.Template;
import org.xidea.lite.dtd.DefaultEntityResolver;
import org.xidea.lite.parser.XMLParser;

public class XMLParserTest {

	private XMLParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new XMLParser();
		parser.setExpressionFactory(new ExpressionFactoryImpl());
	}

	@Test
	public void testXMLParser() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(false);
		// factory.setExpandEntityReferences(false);
		factory.setCoalescing(false);
		// factory.setFeature("http://apache.org/xml/features/validation/default-attribute-values",
		// false);
		// System.out.println(factory.getFeature(XMLConstants.XML_DTD_NS_URI));
		// factory.setXIncludeAware(true);
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		documentBuilder.setEntityResolver(new DefaultEntityResolver());
		Document doc = documentBuilder
				.parse(new ByteArrayInputStream(
						"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><pre>---&nbsp;---</pre>"
								.getBytes()));
		CDATASection cdata = doc.createCDATASection("<]]->");
		System.out.println(cdata);
		System.out.println(cdata.getData());
		System.out.println(cdata);
		doc.getDocumentElement().appendChild(cdata);
		javax.xml.transform.TransformerFactory.newInstance().newTransformer()
				.transform(new DOMSource(doc), new StreamResult(System.out));

		System.out
				.println(doc.getDocumentElement().getChildNodes().getLength());
		System.out
				.println(doc.createElement("pre").getAttributes().getLength());
		System.out
				.println(doc.getDocumentElement().getAttributes().getLength());
		System.out.println(doc.getDocumentElement().getFirstChild()
				.getNodeValue());
	}



}
