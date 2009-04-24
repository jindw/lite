package org.xidea.lite.parser.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.lite.dtd.DefaultEntityResolver;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;
import org.xidea.lite.parser.ResultContext;
import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xidea.lite.parser.impl.TextParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
public
class XMLParser extends TextParser{
	private static Log log = LogFactory.getLog(XMLParser.class);
	private static final Pattern XML_HEADER_SPACE_PATTERN = Pattern
	.compile("^[\\s\\ufeff]*<");
	protected DocumentBuilder documentBuilder;
	
	
	public XMLParser(){
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
	public List<Object> parse(Object data) throws MalformedURLException {
		return parse(data,new ParseContextImpl(new URL("http://xx/")));
	}
	public List<Object> parse(Object data, ParseContext context) {
		try {
			Node node = null;
			if (data instanceof String) {
				String path = (String) data;
				if (XML_HEADER_SPACE_PATTERN.matcher(path).find()) {
					node = documentBuilder.parse(new InputSource(
							new StringReader(path)));
				} else {
					int pos = path.indexOf('#');
					String xpath = null;
					if (pos > 0) {
						xpath = path.substring(pos + 1);
						path = path.substring(0, pos);
					}
					node = context.loadXML(context.createURL(null, path));
					if (xpath != null) {
						node = context.selectNodes(xpath, node);
					}
				}

			} else if (data instanceof URL) {
				node = context.loadXML((URL) data);
			} else if (data instanceof File) {
				node = context.loadXML(((File) data).toURI().toURL());
			}
			if (node != null) {
				parseNode(node, context);
			}
			return context.toResultTree();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void parseNode(Object node, ParseContext context) {
		context.parse(node);
	}

	public Document loadXML(URL url, ParseContextImpl context) throws SAXException, IOException {
		return (Document) context.loadXML(url);
	}

}
