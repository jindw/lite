package org.xidea.lite.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class AutoSyntaxTest {
	static final String TEST_XHTML = "/test.xhtml";

	static ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();


	private Map<String, String> resultMap;


	protected TestItem currentItem;
	//
	@Parameters(name="{index} -{0}")
	
	public static Collection<Object[]> getParams() {
		String[] casefiles = {
				"choose-case.xml",
				"def-case.xml",
				"extends-case.xml",
				"extension-case.xml",
				"for-case.xml",
				"if-case.xml",
				"include-case.xml" ,
				"out-case.xml",
				"widget-case.xml"};

		//System.out.println(getParams(casefiles));
		return getParams(casefiles);

	}

	static Collection<Object[]> getParams(String[] casefiles) {
		ArrayList<Object[]> rtv = new ArrayList<Object[]>();
		for (String file : casefiles) {
			Map<String, List<Object[]>> cases;
			try {
				cases = loadCases(file);
				for (List<Object[]> args : cases.values()) {
					rtv.addAll(args);
				}
			} catch (Exception e) {
				System.out.println("case load failed!"+file);
				e.printStackTrace();
			}

		}
		return rtv;
	}

	// public AutoTest(){}
	public AutoSyntaxTest(TestItem item) throws IOException, SAXException {
		this.currentItem = item;
		try {
			HashMap<String,Object> ctx = JSONDecoder.decode(item.model);
			item.sourceMap.put(TEST_XHTML, item.source);
			resultMap = LiteTest.runTemplate(item.sourceMap, ctx, TEST_XHTML,
					item.expect);

			if(item.format){
				item.expect = LiteTest.normalizeXML(item.expect);
			}
		} catch (Error e) {
			e.printStackTrace();
			throw e;
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void testJava() throws IOException {
		test("java");
	}

	@Test
	public void testPhp() throws IOException {
		test("php");
	}

	@Test
	public void testJS() throws IOException {
		test("js");
	}

	public void test(String type) throws IOException {
		String value = resultMap.get(type);
		if(!currentItem.expect.equals(value)){
			if(currentItem.format){
				value = LiteTest.normalizeXML(value);
			}else{
				value=value.replace("&quot;", "&#34;").replace("&lt;", "&#60;");
			}
		}
		Assert.assertEquals( type+" 运行结果有误：#" +currentItem.source+"\n\n",
				currentItem.expect, value);
	}

	static Map<String, List<Object[]>> loadCases(final String path) throws Exception {
		LinkedHashMap<String, List<Object[]>> caseMap = new LinkedHashMap<String, List<Object[]>>();
		Document doc;
		try {
			doc = LiteTest.loadXML(new FileInputStream(new File(LiteTest.projectRoot,"src/test/cases/"+path)));
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new RuntimeException("load test cases xml failure:" + path);
		}
		String format0 = doc.getDocumentElement().getAttribute("format");
		NodeList units = doc.getElementsByTagName("unit");
		for (int i = 0; i < units.getLength(); i++) {
			Element unit = (Element) units.item(i);
			String unitTitle = unit.getAttribute("title");

			String format1 = initFormat(unit, format0);
			NodeList ns = unit.getChildNodes();
			ArrayList<Object[]> result = new ArrayList<Object[]>();
			HashMap<String, String> sourceMap = new HashMap<String, String>();
			String defaultModel = "{}";
			for (int j = 0; j < ns.getLength(); j++) {
				Node node = ns.item(j);
				if (node.getNodeType() == 1) {
					Element case0 = (Element) node;
					String tagName = case0.getTagName();
					if ("source".equals(tagName)) {
						sourceMap.put(case0.getAttribute("path"), case0
								.getTextContent());
						continue;
					} else if ("model".equals(tagName)) {
						defaultModel = node.getTextContent();
						defaultModel = (String) LiteTest.engine.eval("JSON.stringify("+defaultModel+")");
					} else if ("case".equals(tagName)) {
						String source = getChildContent(case0, "source", null);
						String expect = getChildContent(case0, "expect", null);
						String title = case0.getAttribute("title") ;
						String model = getChildContent(case0, "model",
								defaultModel);
						if(title.length()==0){
							title = unitTitle+'['+j+']';
						}
						if(defaultModel != model){
							model = (String) LiteTest.engine.eval("JSON.stringify("+model+")");
						}
						TestItem item = new TestItem();
						item.sourceMap = new HashMap<String,String>(sourceMap);
						item.expect = expect;
						item.source = source;
						item.title = title;
						item.file = path;
						item.model = model;
						item.format = !"false".equals(initFormat(case0, format1));
						//if(expect == null){
							//System.out.println(path+source);
						//}else{
							result.add(new Object[] { item });
						//}

					}
				}
			}
			caseMap.put(unitTitle, result);
		}
		return caseMap;
	}

	private static String initFormat(Element unit, String format) {
		format =unit.hasAttribute("format")? unit.getAttribute("format"):format;
		return format;
	}

	static class TestItem{
		public HashMap<String, String> sourceMap;
		String file;
		String title;
		String source;
		String model;
		String expect;

		boolean format;
		public String toString(){
			return file+'#'+title;
		}

	}
	private static String getChildContent(Node e, String tagName,
			String defaultText) {
		Node c = e.getFirstChild();
		while (c != null) {
			if (c instanceof Element) {
				if (tagName.equals(((Element) c).getTagName())) {
					return c.getTextContent();
				}
			}
			c = c.getNextSibling();
		}
		return defaultText;
	}

}
