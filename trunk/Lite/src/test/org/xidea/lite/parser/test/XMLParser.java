package org.xidea.lite.parser.test;

import java.io.File;
import java.io.IOException;
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
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xidea.lite.parser.impl.TextNodeParser;
import org.xidea.lite.parser.impl.dtd.DefaultEntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
public
class XMLParser extends TextNodeParser{
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
	public List<Object> parse(Object data) throws MalformedURLException, URISyntaxException {
		return parse(data,new ParseContextImpl(new org.xidea.lite.TemplateEngine(new URI("http://xx/")),null,null,null));
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
					node = context.loadXML(context.createURI(path, null));
					if (xpath != null) {
						node = context.selectNodes(node, xpath);
					}
				}

			} else if (data instanceof URI) {
				node = context.loadXML((URI) data);
			} else if (data instanceof File) {
				node = context.loadXML(((File) data).toURI());
			}
			if (node != null) {
				parseNode(node, context);
			}
			return context.toList();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void parseNode(Object node, ParseContext context) {
		context.parse(node);
	}

	public Document loadXML(URI url, ParseContextImpl context) throws SAXException, IOException {
		return (Document) context.loadXML(url);
	}

}
