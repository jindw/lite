package org.xidea.lite.test.oldcases;

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
import org.xidea.lite.LiteTemplate;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseContext;
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
		ParseContext context = createContext();
		Document doc = toDoc(context, "<xml/><c:group xmlns:c='http://www.xidea.org/lite/core'><xml/></c:group>");
		System.out.println(doc);
		context.parse(doc);
		String result = toString(context);
		System.out.println(result);
	}
	@Test
	public void testAutoDTD() throws SAXException, IOException,
			URISyntaxException {
		ParseContext context = createContext();
		Document doc = toDoc(context, "<xml>&nbsp;</xml>");
		System.out.println(doc);
		context.parse(doc);
		String result = toString(context);
		System.out.println(result);
	}

	@Test
	public void testHTML5() throws SAXException, IOException,
			URISyntaxException {
		ParseContext context = createContext();
		Document doc = toDoc(context, "<!doctype html><xml>&nbsp;</xml>");
		System.out.println(doc);
		context.parse(doc);
		String result = toString(context);
		System.out.println(result);
	}
	@Test
	public void testEncoding() throws SAXException, IOException,
			URISyntaxException {
		ParseContext context = createContext();
		Document doc = toDoc(context, "<!doctype html><xml>&nbsp;金大为</xml>");
		doc = toDoc(context, "<!doctype html><xml>&nbsp;金大为</xml>");
		System.out.println(doc);
		context.parse(doc);
		String result = toString(context);
		Assert.assertTrue("",result.indexOf("&amp;") == -1);
		System.out.println("result:"+result);
	}
	@Test
	public void testAnd() throws SAXException, IOException,
			URISyntaxException {
		ParseContext context = createContext();
		Document doc = toDoc(context, "<!doctype html><xml href='dd&bd='>&&</xml>");
		System.out.println(doc);
		context.parse(doc);
		String result = toString(context);
		System.out.println(result);
	}

	@Test
	public void testNoDTD() throws SAXException, IOException,
			URISyntaxException {
		ParseContext context = createContext();
		String source = "<xml>1</xml><xml>2</xml>";
		Document doc = toDoc(context, source);
		System.out.println(doc);
		context.parse(doc);
		String result = toString(context);
		Assert.assertEquals(source, result);
	}

	@Test
	public void testScriptClient() throws SAXException, IOException,
			URISyntaxException {
		ParseContext context = createContext();
		String source = "<c:client id='tt'><script/><script/></c:client>";
		Document doc = toDoc(context, source);
		context.parse(doc);
		String result = toString(context);
		System.out.println("result:"+result);
		Assert.assertTrue("只能剩下最后一个外部script",findCount(result,"</script>") == 1);
		Assert.assertTrue("确保所有的</script>都被编码了",findCount(result,"<\\/script>") == 2);
	}
	int findCount(String text,String sub){
		int p = -1,inc=0;
		while((p = text.indexOf(sub,p+1))>=0){
			inc++;
		}
		return inc;
	}

	private Document toDoc(ParseContext context, String source)
			throws SAXException, IOException, URISyntaxException,
			UnsupportedEncodingException {
		Document doc = LiteTestUtil.loadXML(source,null);
		return doc;
	}

	private String toString(ParseContext context) throws IOException {
		StringWriter out = new StringWriter();
		new LiteTemplate(context.toList(),LiteTestUtil.defaultFeatureMap).render(this, out);
		String result = out.toString();
		return result;
	}

	private ParseContext createContext() {
		ParseContext context = LiteTestUtil.buildParseContext(new File(".").toURI());;
		return context;
	}

}
