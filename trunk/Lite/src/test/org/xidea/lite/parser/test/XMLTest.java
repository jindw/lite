package org.xidea.lite.parser.test;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.impl.HTMLNodeParser;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.test.TestUtil;

public class XMLTest {
	protected Map<String, Object> context;
	protected Map<String, String> templateResultMap;
	XMLParser parser;
	@Before
	public void setUp() throws Exception {
		parser = new XMLParser();
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void print() throws Exception {
		HashMap<String, Object> context = new LinkedHashMap<String, Object>();
		context.put("1", new String[] { "1", "2" });
		XMLEncoder encoder = new XMLEncoder(System.out);
		encoder.writeObject(context);
		encoder.flush();
	}

	@Test
	public void runMacroTest() throws Exception {
		runTest("MacroTest");
	}
	@Test
	public void runELTest() throws Exception {
//		runAutoFormTest();
//		String source = "${'11'.length()}${'a'.length()?'1':'2'}";
//		String result = "1%3A1";
//		test(1111,source,result);
		runTest("ELTest");
	}
	@Test
	public void runXMLTest() throws Exception {
		runTest("XMLTest");
	}

	@Test
	public void runCoreTest() throws Exception {
		runTest("CoreTest");
	}

	@Test
	public void runVarTest() throws Exception {
		runTest("VarTest");
	}

	@Test
	public void runHTMLTest() throws Exception {
		runTest("HTMLTest");
	}

	@Test
	public void runAutoFormTest() throws Exception {
		runTest("AutoFormTest");
	}

	public void runTest(String file) throws Exception {
		XMLDecoder de = new XMLDecoder(this.getClass().getResourceAsStream(
				file + ".xml"));
		this.context = (Map<String, Object>) de.readObject();
		System.out.println(JSONEncoder.encode(this.context));
		this.templateResultMap = (Map<String, String>) de.readObject();
		int i=0;
		for (String key : templateResultMap.keySet()) {
			String value = templateResultMap.get(key);
			test(i++,key, value);
		}
	}

	public void test(int index,String text, String result) throws Exception {

		String info = "第"+index+"个测试错误：";
		ParseContextImpl parseContext = TestUtil.buildParseContext(this.getClass()
				.getResource("/").toURI());
		parseContext.getFeatrueMap().put(HTMLNodeParser.AUTO_FORM_FEATRUE_URI, HTMLNodeParser.AUTO_IN_FORM);
		
		parseContext.setCompress(true);
		List<Object> insts = parser.parse(
				"<div xmlns:c=\"http://www.xidea.org/ns/template/core\">"
						+ text + "</div>", parseContext);
		checkElse(insts,info);
		System.out.println(JSONEncoder.encode(insts));
		Template t = new Template(insts);
		Writer out = new StringWriter();
		t.render(new HashMap(context), out);
		assertXMLEquals(info+text+":\n"+result,"<div>" + result + "</div>", out.toString());
		
	}
	
	public static void checkElse(List<Object> insts,String info) {
		int i = 0;
		int type = -1;
		for(Object item:insts){
			if(item instanceof String){
				type = -1;
			}else {
				List<Object> data = (List)item;
				int type2 = ((Number)data.get(0)).intValue();

				switch(type2){
				case Template.ELSE_TYPE:
				case Template.FOR_TYPE:
				case Template.CAPTRUE_TYPE:
				case Template.IF_TYPE:
				case Template.PLUGIN_TYPE:
					checkElse((List<Object>)data.get(1),info);
					
				}
				
				
				if(type2 == Template.ELSE_TYPE){
					if(data.get(2) == null){
						type2 = -type2;
						if(type == type2){
							Assert.fail(info+"不能有两个连续的无条件Else");
						}
					}
					
				}
				type=type2;
			}
			i++;
		}
	}

	public static void assertXMLEquals(String msg,String expected, String acture) {
		Assert.assertEquals(msg+"(有效字符摘要检查)",sumText(expected), sumText(acture));
		if(empty.matcher(expected).find() || empty.matcher(expected).find()){
			expected = rexml(expected);
			acture = rexml(acture);
		}
		expected = empty.matcher(expected).replaceAll("");
		acture = empty.matcher(acture).replaceAll("");
		Assert.assertEquals(msg,expected, acture);
	}

	private static Pattern empty = Pattern.compile("[\\s]+");
	private static Pattern mutiAttr = Pattern.compile("<[\\w\\-]+([\\s]+[\\w\\-]+\\s*=\\s*(?:'[^']*'|\"[^\"]*\")){2,}");
	@Test
	public void testMutiAttr(){
		Assert.assertTrue("多属性",mutiAttr.matcher("<a\tx=''\n\ty=\"3\"").find());
		Assert.assertTrue("单属性",!mutiAttr.matcher("<a\t\n\ty=\"3 b=''\"").find());
	}
	public static String sumText(String xml){
		StringBuilder sum = new StringBuilder();
		for(char c:xml.toCharArray()){
			if(!Character.isWhitespace(c)){
				sum.append(c);
			}
		}
		return sum.toString();
	}
	public static String rexml(String xml) {
		try {
			Transformer transformer = javax.xml.transform.TransformerFactory
					.newInstance().newTransformer();
			StringWriter out = new StringWriter();
			Result outputTarget = new javax.xml.transform.stream.StreamResult(
					out);
			//transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new StreamSource(new StringReader(xml)),
					outputTarget);
			return out.toString();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
