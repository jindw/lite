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
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.impl.XMLNormalizeImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLNormalizeTest {
	private static DocumentBuilder DB;
	XMLNormalizeImpl impl = new XMLNormalizeImpl();
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
		XMLNormalizeImpl impl = new XMLNormalizeImpl();
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
		assertNorm("<a href='a&b'></a>","<a href='a&amp;b'></a>");
		assertNorm("<a href='a&amp,;'></a>","<a href='a&amp;amp,;'></a>");
		assertNorm("<a href='&nbsp;nbsp;'></a>","<a href='&#160;nbsp;'></a>");
		assertNorm("<a href='&copy;'></a>","<a href='&#169;'></a>");
	}
	private void assertNorm(String source, String expect) throws SAXException, IOException {
		String result = norm(source);
		Assert.assertEquals("转换失败:"+source, expect, result);
		
	}
	private String norm(String source) throws SAXException, IOException {
		String path = source.replaceAll("[\r\n][\\s\\S]*", "");
		String result = impl.normalize(source,path);
		InputSource s = new InputSource(new StringReader(result));
		s.setSystemId(path);
		Document doc = DB.parse(s);
//		String uri = ((Document)doc.cloneNode(true)).getDocumentURI();
//		System.out.println(uri);
		return result;
	}

}
