package org.xidea.el.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import org.xidea.lite.impl.ParseUtil;

@RunWith(Parameterized.class)
public class AutoTest {
	static ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();

	static String[] casefiles = { "value-case.xml", "json-case.xml",
			"op-case.xml", "global-case.xml", "array-case.xml",
			"string-case.xml", "math-case.xml" };
	static Collection<Object[]> params = null;

	private String source;

	private String model;

	private boolean isJSONResult;

	private Map<String, String> resultMap;

	@Parameters
	public static Collection<Object[]> getParams() {
		if (params == null) {
			ArrayList<Object[]> rtv = new ArrayList<Object[]>();
			for (String file : casefiles) {
				for (List<Object[]> args : loadCases(file).values()) {
					rtv.addAll(args);
				}
			}
			params = rtv;
		}
		return params;
	}

	// public AutoTest(){}
	public AutoTest(String source,String model,boolean isJSONResult) {
		this.source = source;
		this.model = model;
		this.isJSONResult = isJSONResult;
		this.resultMap = ELTest.resultMap(model, source,
				isJSONResult);
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
		String expect = resultMap.get("#expect");
		String value = resultMap.get(type);
				Assert.assertEquals(type + "运行结果有误：#"
						+ source, expect, value);
	}

	private static Map<String, List<Object[]>> loadCases(String path) {
		LinkedHashMap<String, List<Object[]>> caseMap = new LinkedHashMap<String, List<Object[]>>();
		Document doc;
		try {
			doc = ParseUtil.loadXML(AutoTest.class.getResource(path).toURI()
					.toString());
		} catch (Exception e1) {
			throw new RuntimeException("load test cases xml failure:" + path);
		}
		NodeList units = doc.getElementsByTagName("unit");
		for (int i = 0; i < units.getLength(); i++) {
			Element unit = (Element) units.item(i);
			String title = unit.getAttribute("title");
			NodeList ns = unit.getElementsByTagName("case");
			ArrayList<Object[]> result = new ArrayList<Object[]>();
			for (int j = 0; j < ns.getLength(); j++) {
				Element case0 = (Element) ns.item(j);
				String isJSONResult = case0.getAttribute("json");
				Element se = getFirstChild(case0, "source");
				String source = (se == null ? case0 : se).getTextContent();
				String model = null;
				Element me = getFirstChild(case0, "model");
				if (me == null) {
					me = getFirstChild(unit, "model");
				}
				if (me != null) {
					model = me.getTextContent();
				}
				if (model == null || model.length() == 0) {
					model = "{}";
				}
				result.add(new Object[] { source, model,
						isJSONResult.equals("true") });
			}
			caseMap.put(title, result);
		}
		return caseMap;
	}


	private static Element getFirstChild(Node e, String tagName) {
		Node c = e.getFirstChild();
		while (c != null) {
			if (c instanceof Element) {
				if (tagName.equals(((Element) c).getTagName())) {
					return (Element) c;
				}
			}
			c = c.getNextSibling();
		}
		return null;

	}

	public static void main(String[] arg) throws Exception {
		File root = new File(new File(AutoTest.class.getResource("/").toURI()),
				"../../");
		Writer out = new OutputStreamWriter(new FileOutputStream(new File(root,
				"test/data/test-el.json")));
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
						String source = (String) args[0];
						String model = (String) args[1];
						boolean isJSONResult = (Boolean) args[2];
						Map<String, String> resultMap = ELTest.resultMap(model,
								source, isJSONResult);
						String expect = resultMap.get("#expect");
						HashMap<String, String> info = new HashMap<String, String>();
						info.put("source", source);
						info.put("model", model);
						info.put("expect", expect);
						for (Map.Entry<String, String> entry : resultMap
								.entrySet()) {
							if (!entry.getKey().startsWith("#")) {
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
			out.close();
		}
	}
}
