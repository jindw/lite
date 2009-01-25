package org.xidea.lite.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.lite.dtd.DefaultEntityResolver;

public class NamedIncludeTest {
	private DocumentBuilder documentBuilder;
	private XPathFactory xpathFactory;

	@Before
	public void setUp() throws Exception {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);

			documentBuilder = factory.newDocumentBuilder();
			documentBuilder.setEntityResolver(new DefaultEntityResolver());
			xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public void removeDefaultNamespace(Node node){
		//TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
	}
	@Test
	public void testXpath() throws Exception {
		final Document doc = documentBuilder.parse(this.getClass().getResourceAsStream("input.xml"));
		XPath xpath = xpathFactory.newXPath();
		System.out.println(xpath.getNamespaceContext());
		//NodeList attributes = (NodeList)xpath.evaluate("//namespace::*", doc ,XPathConstants.NODESET);
		NamedNodeMap attributes = doc.getDocumentElement().getAttributes();
		HashMap<String, String> prefixMap = new HashMap<String, String>();
		for (int i = 0; i < attributes.getLength(); i++) {
			Attr attr = (Attr) attributes.item(i);
			String value = attr.getNodeValue();
			if("xmlns".equals(attr.getNodeName())){
				int p1 = value.lastIndexOf('/');
				String prefix = value;
				if(p1>0){
					prefix = value.substring(p1+1);
					if(prefix.length()==0){
						int p2 = value.lastIndexOf('/',p1-1);
						prefix = value.substring(p2+1,p1);
					}
				}
				prefixMap.put(prefix, value);
			}else if("xmlns".equals(attr.getPrefix())){
				prefixMap.put(attr.getLocalName(), value);
			};
		}
		System.out.println(prefixMap);
		final String defaultNamespaceURI = doc.getDocumentElement().getNamespaceURI();
		xpath.setNamespaceContext(new NamespaceContext(){

			public String getNamespaceURI(String prefix) {
				if("".equals(prefix ) ||"xmlns".equals(prefix )){
				    return defaultNamespaceURI;
				}
				return defaultNamespaceURI;
			}

			public String getPrefix(String namespaceURI) {
				if(defaultNamespaceURI.equals(namespaceURI )){
					return null;
				}
				return null;
			}

			public Iterator<?> getPrefixes(String namespaceURI) {
				if(defaultNamespaceURI.equals(namespaceURI )){
				return Arrays.asList(null,"","ns").iterator();
				}
				return new ArrayList<Object>().iterator();
			}
			
		});
		System.out.println(xpath.evaluate("count(//@*)", doc.getDocumentElement().getChildNodes() ,XPathConstants.STRING));
		NodeList ns = (NodeList)xpath.evaluate("namespace::*", doc.getDocumentElement() ,XPathConstants.NODESET);
		System.out.println(ns.getLength());
		for (int i = 0; i < attributes.getLength(); i++) {
			System.out.println(attributes.item(i).getLocalName());
			System.out.println(attributes.item(i).getNodeValue());
		}
		Object result = xpath.evaluate("count(//xmlns:body)", doc, XPathConstants.STRING);
		System.out.println(result);
		System.out.println(124);

		
	}

}
