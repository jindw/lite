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
	String filename;

	@Parameters
	public static Collection<Object[]> getParams() {
		ArrayList<Object[]> rtv = new ArrayList<Object[]>();
		for (String file : casefiles) {
			rtv.add(new Object[] { file });
		}
		return rtv;
	}

	// public AutoTest(){}
	public AutoTest(String filename) {
		this.filename = filename;
	}

	@Test
	public void test() throws IOException {
		test(filename, null);
	}

	private static void test(String path, Writer out) throws IOException {
		Document doc;
		try {
			doc = ParseUtil.loadXML(AutoTest.class.getResource(path).toURI()
					.toString());
		} catch (Exception e1) {
			throw new RuntimeException("load test cases failure:" + path);
		}
		NodeList units = doc.getElementsByTagName("unit");
		for (int i = 0; i < units.getLength(); i++) {
			Element unit = (Element) units.item(i);
			String title = unit.getAttribute("title");
			NodeList ns = unit.getElementsByTagName("case");
			ArrayList<Object> result = new ArrayList<Object>();
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
				// ELTest.testEL(model, source,
				// "true".equals(isJSONResult));
				// System.out.println(model);
				// System.out.println(source);
				// System.out.println(filename);
				// System.exit(0);
				Map<String, String> resultMap = ELTest.resultMap(model, source,
						"true".equals(isJSONResult));
				String expect = resultMap.get("#expect");
				HashMap<String, String> info = new HashMap<String, String>();
				info.put("source", source);
				info.put("model", model);
				info.put("expect", expect);
				for (Map.Entry<String, String> entry : resultMap.entrySet()) {
					if (!entry.getKey().startsWith("#")) {

						try {
							Assert.assertEquals(entry.getKey() + "运行结果有误：#"
									+ source, expect, entry.getValue());
						} catch (Error e) {
							if (out == null) {
								throw e;
							} else {
								info.put(entry.getKey(), entry.getValue());
							}
						}

					}
				}
				if (out != null) {
					result.add(info);
				}
				// System.out.println("addCase(" + JSONEncoder.encode(source)
				// + "," + model + "," + resultMap + ")");
			}
			if (out != null) {
				String js = JSONEncoder.encode(result);
				js = js.replaceAll("\\bencodeURI\\(", "encod\\\\u0065URI(");
				out.append("addCase(" + JSONEncoder.encode(title) + ","
						+ js + ");\n");
			}
		}
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

	public static void main(String[] args) throws Exception {
		File root = new File(new File(AutoTest.class.getResource("/").toURI()),
				"../../");
		Writer out = new OutputStreamWriter(new FileOutputStream(new File(root,
				"test/data/test-el.js")));
		try {
			for (String file : casefiles) {
				test(file, out);
			}
			out.flush();
		} finally {
			out.close();
		}
	}
}
