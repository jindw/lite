package org.xidea.lite.parser.impl.xml.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xidea.el.json.JSONEncoder;
import org.xidea.jsi.JSIRuntime;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.impl.HotTemplateEngine;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.impl.XMLNormalizeImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLNormalizeTest {
	private static DocumentBuilder DB;
	XMLNormalizeImpl impl = new XMLNormalizeImpl(Collections.EMPTY_MAP,XMLNormalizeImpl.DEFAULT_ENTRY_MAP);
	static{
		DocumentBuilder db = null;
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DB = db;
	}
	@Test
	public void testContent() throws FileNotFoundException, IOException, SAXException{
		String s = norm("<html><head></head>&aaaa=13</html>");
		System.out.println(s);
	}
	@Test
	public void testFile() throws FileNotFoundException, IOException, SAXException{
		String s = norm(ParseUtil.loadTextAndClose(new FileInputStream("D:\\workspace\\FireSite\\web\\index.xhtml")));
		s = norm("<html xmlns:f=\"http://firekylin.my.baidu.com/ns/2010\"><head>\n"+
"<title f:block=\"title\">for</title></head><body><f:include path=\"i18n-test-inc.xhtml\"/></body></html>"
);
		System.out.println(s);
	}
	@Test
	public void testRoot() throws FileNotFoundException, IOException, SAXException{
		XMLNormalizeImpl impl = new XMLNormalizeImpl(Collections.EMPTY_MAP,Collections.EMPTY_MAP);
		impl.setDefaultRoot("<root>");
		Assert.assertEquals(impl.normalize("<br><img>", ""), "<root><br/><img/></root>");
		impl.setDefaultRoot("<root/>");
		Assert.assertEquals(impl.normalize("<br><img>", ""), "<root><br/><img/></root>");
		impl.setDefaultRoot("<root></root>");
		Assert.assertEquals(impl.normalize("<br><img>", ""), "<root><br/><img/></root>");
		impl.setDefaultRoot("<root> </root>");
		Assert.assertEquals(impl.normalize("<br><img>", ""), "<root><br/><img/></root>");
	}
	/**
	 * 
	 * @throws SAXException
	 * @throws IOException
	 */
	@Test
	public void testUnmach() throws SAXException, IOException{
		assertNorm("<hr><a></a>","<c:group xmlns:c='http://www.xidea.org/lite/core'><hr/><a></a></c:group>");
		assertNorm("<hr>","<hr/>");
		assertNorm("<hr><hr title=jindw selected>","<c:group xmlns:c='http://www.xidea.org/lite/core'><hr/><hr title=\"jindw\" selected=\"selected\"/></c:group>");
		assertNorm("<img src=\"'<hr>\">","<img src=\"'&lt;hr>\"/>");
		assertNorm("<img src=\"'<hr>\" title=${1 <e}>","<img src=\"'&lt;hr>\" title=\"${1 &lt;e}\"/>");
		assertNorm("<hr c:if=${1<a}></hr>","<hr c:if=\"${1&lt;a}\" xmlns:c=\"http://www.xidea.org/lite/core\"/>");
		//System.out.println(impl.normalize("<hr>"));
	}
	@Test
	public void testSpec() throws SAXException, IOException{
		assertNorm("<a href=\"&copy;\"></a>","<a href=\"&#169;\"></a>");
		assertNorm("<a href='&nbsp;nbsp;'></a>","<a href='&#160;nbsp;'></a>");
		assertNorm("<a href='a&b'></a>","<a href='a&amp;b'></a>");
		assertNorm("<a href='a&amp,;'></a>","<a href='a&amp;amp,;'></a>");
	}
	private void assertNorm(String source, String expect) throws SAXException, IOException {
		String result = norm(source);
		Assert.assertEquals("转换失败:"+source, expect, result);
		
	}
	private String norm(String source) throws SAXException, IOException {
		String path = source.replaceAll("[\r\n][\\s\\S]*", "");
		String result = impl.normalize(source,path);
		System.out.println(result);
		InputSource s = new InputSource(new StringReader(result));
		s.setSystemId(path);
		Document doc = DB.parse(s);
//		String uri = ((Document)doc.cloneNode(true)).getDocumentURI();
//		System.out.println(uri);
		return result;
	}

	@Test
	public void testXMLTime() throws SAXException, Exception{
		String source = ParseUtil.loadTextAndClose(XMLNormalizeTest.class.getResourceAsStream("index.xhtml"));
		Field f = RuntimeSupport.class.getDeclaredField("testRhino");
		f.setAccessible(true);
		f.set(null,false);
		JSIRuntime rs = RuntimeSupport.create();
		Object rtv = null;
		System.out.println(rs);
		rs.eval("$import('org.xidea.lite.util:normalizeXML')");
		rs.eval("this.xml = ("+JSONEncoder.encode(source)+")");
		//String code = "normalizeXML("+JSONEncoder.encode(source)+",'')";
		String code = "normalizeXML(this.xml,'')";
		long java=0,js=0;
		int i = 0;
		while(i-->0){
		long l1 = System.currentTimeMillis();
		String result = impl.normalize(source,"index.xhtml");
		long l2 = System.currentTimeMillis();
		rtv = rs.eval(code);
		long l3 = System.currentTimeMillis();

		System.out.println(java+=l2-l1);
		System.out.println(js+=l3-l2);
		}
		System.out.println();
		System.out.println(java);
		System.out.println(js);
		ParseConfigImpl config = new ParseConfigImpl(URI.create("classpath:///org/xidea/lite/parser/impl/xml/test/"), null);

		ParseContextImpl p = new ParseContextImpl(config, "/index.xhtml");
		p.parse(p.createURI("/index.xml"));
		p = new ParseContextImpl(config, "/index.xhtml");
		p.parse(p.createURI("/index.xhtml"));

		p = new ParseContextImpl(config, "/index.xhtml");
		p.parse(p.createURI("/index.xhtml"));
		p = new ParseContextImpl(config, "/index.xhtml");
		p.parse(p.createURI("/index.xhtml"));
		p = new ParseContextImpl(config, "/index.xhtml");
		p.parse(p.createURI("/index.xhtml"));
		long l1 = System.currentTimeMillis();
		rs.eval(JSONEncoder.encode(source));
		long l2 = System.currentTimeMillis();
		System.out.println(l2-l1);
		//System.out.println(rtv);
	}
}
