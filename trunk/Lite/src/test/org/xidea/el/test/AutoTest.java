package org.xidea.el.test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.util.test.XMLParser;


public class AutoTest {
	ExpressionFactory expressionFactory = ExpressionFactoryImpl.getInstance();



	@Before
	public void setup(){
		
	}
	private String toRegExp(int c){
		int v = 0x10000+c;
		if(v<=0x100FF){
			return "\\x"+Integer.toHexString(v).substring(3);
		}else{
			return "\\u"+Integer.toHexString(v).substring(1);
		}
	}
	/**
	 * [\x22\x3c\x3e\x5c\x5e\x60\u1680\u180e\u202f\u205f\u3000]|[\x00-\x20]|[\x7b-\x7d]|[\x7f-\xa0]|[\u2000-\u200b]|[\u2028-\u2029]|
	 * [\x2f\x60]|[\x00-\x29]|[\x2b-\x2c]|[\x3a-\x40]|[\x5b-\x5e]|[\x7b-\uffff]|
	 * @throws Exception
	 */
	@Test
	public void testEscape() throws Exception{
		testURI();
		StringBuilder source = new StringBuilder();
		for(int i=Character.MAX_VALUE;i>=0;i--){
			try{
				String result = "http://localhost/"+(char)i+"20";
				String result2 = new URL(result).toExternalForm();
				System.out.println(result2);
//				new URI("data:text/xml,"+(char)i+"20");
//				String result = ""+(char)i;
//				String result2 = URLEncoder.encode(result, "utf-8");
				if(!result2.equals(result)){
					throw new Exception();
				}
			}catch (Exception e) {
				source.append((char)i);
				System.out.println((char)i+Integer.toString(i)+":"+Integer.toHexString(i)+":"+URLEncoder.encode(""+(char)i,"UTF-8"));//+(char)i);
				//System.out.println(Integer.toHexString(i));//+(char)i);
				//System.out.println(Integer.toHexString(i));//+(char)i);
			}
		}
		System.out.println(source);
		StringBuilder buf = new StringBuilder();
		StringBuilder starts = new StringBuilder();
		char[] cs = source.toString().toCharArray();
		int start = 0;
		int current = 0;
		int pre = 0;
		for (int i = 1; i < cs.length; i++) {
			pre = cs[i-1];
			current = cs[i];
			if(pre!=current-1){
				if(start == pre){
					starts.append(toRegExp(start));
				}else{
					buf.append("["+toRegExp(start)+"-"+toRegExp(pre)+"]|");
				}
				start = current;
			}
		}
		if(start == current){
			starts.append(toRegExp(start));
		}else{
			buf.append("["+toRegExp(start)+"-"+toRegExp(current)+"]|");
		}
		System.out.println("["+starts+"]|"+buf);
	}
	@Test
	public void testURI() throws Exception{
		System.out.println(new URI("d3.ata:/text/xml,xm$%20l+#进大为0A2Fxml").resolve("/xx#12"));
		System.out.println(new URI("http://s@,@,的(*,ss/32$4").resolve("d/-!00a0_d2.;9s:s"));
		System.out.println(JSONEncoder.encode(new File("/进大为").toURI())+new File("/进大# 为").toURI());
		
		URI url = new URI("http://localhost/进/大_-#dd/为");
		System.out.println(JSONEncoder.encode(url) +url);
		url = new URL("http://localhost/"+URLEncoder.encode("进/大/为","UTF-8")).toURI().resolve("./。a/三/阿嫂");
		System.out.println(JSONEncoder.encode(url)+url);
		System.out.println(JSONEncoder.encode(url.resolve("aaa")));
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
