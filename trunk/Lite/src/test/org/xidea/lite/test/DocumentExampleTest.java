package org.xidea.lite.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.test.AutoELTest;
import org.xidea.lite.impl.ParseUtil;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class DocumentExampleTest {
	static File webRoot = new File(new File(DocumentExampleTest.class
			.getResource("/").getFile()), "../../");
	private static ArrayList<Object[]> params;
	private File file;

	public DocumentExampleTest(File file) {
		this.file = file;
	}

	@Parameters
	public static Collection<Object[]> getParams() throws IOException {
		if (params == null) {
			params = findCaseFiles(webRoot);
		}
		return params;
	}

	private static ArrayList<Object[]> findCaseFiles(File webRoot) throws IOException {
		ArrayList<Object[]> rtv = new ArrayList<Object[]>();
		File dir = new File(webRoot.getCanonicalFile(), "doc/guide");
		for (File file : dir.listFiles()) {
			if(file.getName().endsWith(".xhtml")){
				rtv.add(new Object[]{file});
			}
		}
		return rtv;
	}

	@Test
	public void testFile() throws Exception {
		String name = file.getName();
		String status = "失败:";
		try {
			testFile(file, name);
			status = "成功:";
		} finally {
			System.out.println("文件测试" + status + file);
		}
	}

	public static void main(String[] args) throws Exception {
		File root;
		if (args.length > 0) {
			root = new File(args[0]);
		} else {
			root = new File(
					new File(AutoELTest.class.getResource("/").toURI()),
					"../../");
		}
		File dest = new File(root, "doc/test-data/test-guide-example.json");
		Writer out = new OutputStreamWriter(new FileOutputStream(dest));
		try {
			ArrayList<Object> result = new ArrayList<Object>();
			ArrayList<String> errors = new ArrayList<String>();
			for (Object[] args2 : getParams()) {
				File file = (File) args2[0];
				String name = file.getName();
				String status = "失败:";
				try {
					List<Object> item = testFile(file, name);
					if (item != null) {
						result.add(item);
					}
					status = "成功:";
				} catch (Exception e) {
					e.printStackTrace();
					errors.add(file.toString() + '\n');
					// throw e;
				} finally {
					System.out.println("文件测试" + status + file);
				}
			}
			out.write(JSONEncoder.encode(result));
			out.flush();
		} finally {
			System.out.println("文档示例测试结果写入:" + dest);
			out.close();
		}
	}

	private static List<Object> testFile(File file, String name)
			throws SAXException, IOException, FileNotFoundException,
			XPathExpressionException, TransformerConfigurationException,
			TransformerException, TransformerFactoryConfigurationError {
		if (name.endsWith(".xhtml") && !name.startsWith("layout")) {
			String xhtml = ParseUtil.loadXMLTextAndClose(new FileInputStream(
					file));
			xhtml = ParseUtil.normalize(xhtml, file.getAbsolutePath());
			Document doc = ParseUtil.loadXMLBySource(xhtml, file
					.getAbsolutePath());
			NodeList ns = ParseUtil.selectByXPath(doc, "//x:code");
			HashMap<String, String> fileMap = new HashMap<String, String>();
			HashMap<String, String> varMap = new HashMap<String, String>();
			HashMap<String, String[]> evalMap = new LinkedHashMap<String, String[]>();

			ArrayList<Object> result = new ArrayList<Object>();
			result.add(file.getName());
			for (int i = 0; i < ns.getLength(); i++) {
				Element el = (Element) ns.item(i);
				String varName = el.getAttribute("var");
				String alias = el.getAttribute("alias");
				String path = el.getAttribute("path");
				String model = el.getAttribute("model");
				String source = el.getTextContent().trim();

				if (el.hasAttribute("error")) {
					continue;
				}
				if (path.length() > 0) {
					path = path.replaceFirst("^/?", "/");
					fileMap.put(path, source);
				}
				if (varName.length() > 0) {
					varMap.put(varName, source);
				} else if (alias.length() > 0) {
					varMap.put(alias, source);
				}
				if (model.length() > 0) {
					String expect = el.getAttribute("expect");
					if (expect.length() > 0) {
						NodeList ns2 = ParseUtil.selectByXPath(doc,
								"//x:code[@id='" + expect + "']");
						if (ns2.getLength() > 0) {
							expect = ns.item(0).getTextContent();
							System.out.println(expect);
							//expect = LiteTest.normalizeXML(expect.trim());
							System.out.println(expect);
							System.out.println("===============");
						} else {
							expect = null;
						}
					} else {
						expect = null;
					}
					evalMap.put(source, new String[] { model, expect });
				}
			}
			int index = 0;
			for (Map.Entry<String, String[]> entry : evalMap.entrySet()) {
				String model = entry.getValue()[0];
				String expect = entry.getValue()[1];
				String source = entry.getKey();
				String path = '/' + name + "@" + index++;
				if (varMap.containsKey(model)) {
					model = varMap.get(model);
				}
				fileMap.put(path, source);
				model = model.replaceAll("\\+\\s*new\\s+Date\\(\\)", ""
						+ System.currentTimeMillis());

				Map<String, String> map = LiteTest.runTemplate(fileMap, model,
						path, expect);
				Map<String, String> outputMap = new HashMap<String, String>();
				expect = map.get("#expect");
				outputMap.put("source", source);
				outputMap.put("model", model);
				outputMap.put("expect", expect);
				checkResultEqual(source, expect, map, outputMap);
				result.add(outputMap);
			}
			if (evalMap.size() > 0) {
				System.out.println(file + "测试成功:" + evalMap.size());
			}
			return result;
		}
		return null;
	}

	private static void checkResultEqual(String source, String expect,
			Map<String, String> resultMap, Map<String, String> outputMap)
			throws IOException, SAXException {
		// Map<String, String> outputMap = new HashMap<String, String>();
		expect = LiteTest.normalizeXML(expect);
		for (Map.Entry<String, String> entry : resultMap.entrySet()) {
			if (!entry.getKey().startsWith("#")) {
				try {
					String value = LiteTest.normalizeXML(entry.getValue());
					Assert.assertEquals(entry.getKey() + "运行结果有误：#" + source,
							expect, value);
				} catch (Error e) {
					if (outputMap == null) {
						throw e;
					} else {
						outputMap.put(entry.getKey(), entry.getValue());
					}
					throw e;
				}

			}
		}
	}
}
