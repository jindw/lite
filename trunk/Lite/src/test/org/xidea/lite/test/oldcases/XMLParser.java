package org.xidea.lite.test.oldcases;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xidea.lite.impl.TextNodeParser;
import org.xidea.lite.impl.dtd.DefaultEntityResolver;
import org.xidea.lite.parse.ParseContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLParser extends TextNodeParser {
	private static Log log = LogFactory.getLog(XMLParser.class);
	protected static DocumentBuilder documentBuilder;

	static {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			// factory.setExpandEntityReferences(false);
			factory.setCoalescing(false);
			// factory.setXIncludeAware(true);
			documentBuilder = factory.newDocumentBuilder();
			documentBuilder.setEntityResolver(new DefaultEntityResolver());
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public void parseNode(Object node, ParseContext context) {
		context.parse(node);
	}

	public static Document loadXML(URL url) throws SAXException, IOException {
		try {
			return (Document) documentBuilder.parse(new InputSource(url.toURI()
					.toString()));
		} catch (Exception ex) {
			InputStream in = url.openStream();
			try {
				return null;//new XMLFixerImpl().parse(documentBuilder, in, url.toString());
			} catch (Exception ex2) {
				throw new RuntimeException(ex2);
			}finally {
				in.close();
			}
		}
	}

}
