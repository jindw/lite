package org.xidea.lite.test;

import java.io.File;
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.test.AutoELTest;
import org.xidea.lite.impl.ParseUtil;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class AutoSyntaxTest {
	private static final String TEST_XHTML = "/test.xhtml";

	static ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();

	static String[] casefiles = {  "if-case.xml",
			"choose-case.xml", "for-case.xml", "def-case.xml",
	"extends-case.xml",		
	"include-case.xml" ,
	"extension-case.xml",
	"out-case.xml"};
	static Collection<Object[]> params = null;

	private Map<String, String> sourceMap;
	private Map<String, String> resultMap;

	private boolean format;

	private String expect;

	@Parameters
	public static Collection<Object[]> getParams() {
		if (params == null) {
			ArrayList<Object[]> rtv = new ArrayList<Object[]>();
			for (String file : casefiles) {
				Map<String, List<Object[]>> cases = loadCases(file);

				for (List<Object[]> args : cases.values()) {
					rtv.addAll(args);
				}
			}
			params = rtv;

//			System.out.println(JSONEncoder.encode(rtv));
		}
		return params;
	}

	// public AutoTest(){}
	public AutoSyntaxTest(Map<String, String> sourceMap, String model,
			String expect,boolean format) throws IOException, SAXException {
		this.sourceMap = sourceMap;
		try {
			this.format = format;
			resultMap = LiteTest.runTemplate(sourceMap, model, TEST_XHTML,
					expect);

			if(expect == null){
				expect = resultMap.get("#expect");
				//System.out.println(sourceMap);
			}
			if(this.format){
				expect = LiteTest.normalizeXML(expect);
			}
			this.expect = expect;
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
		if(!expect.equals(value)){
		if(this.format){
			value = LiteTest.normalizeXML(value);
		}else{
			value=value.replace("&quot;", "&#34;").replace("&lt;", "&#60;");
		}
		}
		Assert.assertEquals(type + "运行结果有误：#" + sourceMap.get(TEST_XHTML),
				expect, value);
	}

	private static Map<String, List<Object[]>> loadCases(String path) {
		LinkedHashMap<String, List<Object[]>> caseMap = new LinkedHashMap<String, List<Object[]>>();
		Document doc;
		try {
			doc = ParseUtil.loadXML(AutoSyntaxTest.class.getResource(path)
					.toURI().toString());
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new RuntimeException("load test cases xml failure:" + path);
		}
		String format0 = doc.getDocumentElement().getAttribute("format");
		NodeList units = doc.getElementsByTagName("unit");
		for (int i = 0; i < units.getLength(); i++) {
			Element unit = (Element) units.item(i);
			String title = unit.getAttribute("title");

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
					} else if ("case".equals(tagName)) {
						String source = getChildContent(case0, "source", null);
						String expect = getChildContent(case0, "expect", null);
						String model = getChildContent(case0, "model",
								defaultModel);
						sourceMap = new HashMap<String, String>(sourceMap);
						sourceMap.put(TEST_XHTML, source);
						String format = initFormat(case0, format1);
						//if(expect == null){
							//System.out.println(path+source);
						//}else{
							result.add(new Object[] { sourceMap, model, expect,"true".equals(format) });
						//}
					}
				}
			}
			caseMap.put(title, result);
		}
		return caseMap;
	}

	private static String initFormat(Element unit, String format) {
		format =unit.hasAttribute("format")? unit.getAttribute("format"):format;
		return format;
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

	public static void main(String[] arg) throws Exception {
		File root;
		if (arg.length > 0) {
			root = new File(arg[0]);
		} else {
			root = new File(
					new File(AutoELTest.class.getResource("/").toURI()),
					"../../");
		}
		File dest = new File(root, "doc/test-data/test-syntax.json");
		Writer out = new OutputStreamWriter(new FileOutputStream(dest));
		try {

			ArrayList<Object> allResult = new ArrayList<Object>();
			for (String file : casefiles) {
				Map<String, List<Object[]>> cases = loadCases(file);
				for (Map.Entry<String, List<Object[]>> unitEntry : cases
						.entrySet()) {
					String title = unitEntry.getKey();
					ArrayList<Object> unitResult = new ArrayList<Object>();
					unitResult.add(title);
					for (Object[] args : unitEntry.getValue()) {
						Map<String, String> sourceMap = (Map<String, String>) args[0];
						String model = (String) args[1];
						String expect = (String) args[2];
						Map<String, String> resultMap = LiteTest.runTemplate(
								sourceMap, model, TEST_XHTML, expect);
						HashMap<String, String> info = new HashMap<String, String>();
						info.put("source", sourceMap.get(TEST_XHTML));
						info.put("model", model);
						info.put("expect", expect);
						
						for (Map.Entry<String, String> entry : resultMap
								.entrySet()) {
							if (!entry.getKey().startsWith("#")) {
								if(expect == null){
									expect = entry.getValue();
								}
								if (expect.equals(entry.getValue())) {
								} else {
									info.put(entry.getKey(), entry.getValue());
								}
							}
						}
						unitResult.add(info);
					}
					allResult.add(unitResult);
				}
			}
			out.write(JSONEncoder.encode(allResult));
			out.flush();
		} finally {
			System.out.println("语法测试结果写入:" + dest);
			out.close();
		}
	}
}
