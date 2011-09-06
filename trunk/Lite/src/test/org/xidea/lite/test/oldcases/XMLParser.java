package org.xidea.lite.test.oldcases;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.TextNodeParser;
import org.xidea.lite.impl.dtd.DefaultEntityResolver;
import org.xidea.lite.parse.ParseContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.org.apache.bcel.internal.classfile.SourceFile;

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
