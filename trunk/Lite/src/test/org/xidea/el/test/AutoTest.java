package org.xidea.el.test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.test.oldcases.XMLParser;


public class AutoTest {
	ExpressionFactory expressionFactory = ExpressionFactoryImpl.getInstance();
	@Before
	public void setup(){
	}
	@Test
	public void test(){
		ELTest.testEL("{}","Math.E.toFixed(5)");
//		ELTest.testEL("{}","JSON.stringify([1,2])");
//		ELTest.testEL("{}", "\"\\u91D1\\u5927\\u4E3A\"+'aa'");
	}
	@Test
	public void testAll() throws Exception {
		test("op-case.xml");
		test("global-case.xml");
		test("array-case.xml");
		test("string-case.xml");
		test("math-case.xml");
	}
	private void test(String path) throws Exception {
		Document doc = ParseUtil.loadXML(this.getClass().getResource(path).toURI().toString());
		
		NodeList ns = doc.getElementsByTagName("case");
		for(int i=0;i<ns.getLength();i++){
			Element e = (Element) ns.item(i);
			String source = e.getAttribute("source");
			if(source.length() == 0){
				Element se = getFirstChild(e,"source");
				source = (se == null?e:se).getTextContent();
			}
			String model = e.getAttribute("model");
			if(model.length() == 0){
				Element me = getFirstChild(e,"model");
				if(me == null){
					me = getFirstChild(e,"model");
					if(me == null){
						me = getFirstChild(e.getParentNode(),"model");
					}
				}
				if(me!=null){
					model = me.getTextContent();
				}
			}
			if(model == null || model.length() == 0){
				model = "{}";
			}
			ELTest.testEL(model,source);
		}
	}
	private Element getFirstChild(Node e, String tagName) {
		Node c = e.getFirstChild();
		while(c!=null){
			if(c instanceof Element){
				if(tagName.equals(((Element) c).getTagName())){
					return (Element) c;
				}
			}
			c = c.getNextSibling();
		}
		return null;
		
	}
}
