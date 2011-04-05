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
	public void testFile() throws FileNotFoundException, IOException, SAXException{
		String s = norm(ParseUtil.loadTextAndClose(new FileInputStream("D:\\workspace\\FireSite\\web\\index.xhtml")));
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
	private void assertNorm(String source, String expect) throws SAXException, IOException {
		String result = norm(source);
		Assert.assertEquals("转换失败:"+source, expect, result);
		
	}
	private String norm(String source) throws SAXException, IOException {
		String result = impl.normalize(source,source.replaceAll("[\r\n][\\s\\S]*", ""));
		DB.parse(new InputSource(new StringReader(result)));
		return result;
	}

}
