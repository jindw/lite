package org.xidea.el.test;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.lite.util.test.XMLParser;

public class AutoTest {
	ExpressionFactory expressionFactory = ExpressionFactoryImpl.getInstance();



	@Before
	public void setup(){
		
	}
	@Test
	public void test(){
		ELTest.testEL(null, "\"\\u91D1\\u5927\\u4E3A\"+'aa'");
	}
	@Test
	public void testAll() throws Exception {
		Document doc = XMLParser.loadXML(this.getClass().getResource("auto-test.xml"));
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
