package org.xidea.lite.parser.impl.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import javax.print.URIException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.lite.Template;
import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xml.sax.SAXException;

public class AvoidErrorParserTest {
	private void printURI(URI uri) {
		Map values = ReflectUtil.map(uri);

		System.out.println(uri);
		for (Object key : values.keySet()) {
			Object value = values.get(key);
			if (value != null) {
				System.out.println("\t" + key + "=>" + value);
			}
		}
	}

	@Test
	public void testMutiRoot() throws SAXException, IOException,
			URISyntaxException {
		ParseContextImpl context = createContext();
		Document doc = toDoc(context, "<xml/><c:group xmlns:c='http://www.xidea.org/ns/lite/core'><xml/></c:group>");
		System.out.println(doc);
		context.parse(doc);
		String result = toString(context);
		System.out.println(result);
	}
	@Test
	public void testAutoDTD() throws SAXException, IOException,
			URISyntaxException {
		ParseContextImpl context = createContext();
		Document doc = toDoc(context, "<xml>&nbsp;</xml>");
		System.out.println(doc);
		context.parse(doc);
		String result = toString(context);
		System.out.println(result);
	}

	@Test
	public void testNoDTD() throws SAXException, IOException,
			URISyntaxException {
		ParseContextImpl context = createContext();
		String source = "<xml>1</xml><xml>2</xml>";
		Document doc = toDoc(context, source);
		System.out.println(doc);
		context.parse(doc);
		String result = toString(context);
		Assert.assertEquals(source, result);
	}

	private Document toDoc(ParseContextImpl context, String source)
			throws SAXException, IOException, URISyntaxException,
			UnsupportedEncodingException {
		Document doc = context.loadXML(new URI("text:"
				+ URLEncoder.encode(source, "utf-8")));
		return doc;
	}

	private String toString(ParseContextImpl context) throws IOException {
		StringWriter out = new StringWriter();
		new Template(context.toList()).render(this, out);
		String result = out.toString();
		return result;
	}

	private ParseContextImpl createContext() {
		ParseContextImpl context = new ParseContextImpl(new File(".").toURI(),
				null, null, null) {

			@Override
			public InputStream openInputStream(URI uri) {
				if (uri.getScheme().equals("text")) {
					String data = uri.getSchemeSpecificPart().replace('+', ' ');
					System.out.println(data);
					return new ByteArrayInputStream(data.getBytes());
				}
				return super.openInputStream(uri);
			}

		};
		return context;
	}

}
