package org.xidea.lite.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.Assert;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.test.ELTest;
import org.xidea.jsi.JSIRuntime;
import org.xidea.lite.Template;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.impl.XMLNormalizeImpl;
import org.xidea.lite.parse.ParseContext;
import org.xml.sax.SAXException;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class LiteTest {
	private static JSIRuntime js = org.xidea.jsi.impl.RuntimeSupport.create();
	private static String phpcmd = "php";
	private static String currentPHP;
	static {
		String mbload = execPhp("echo extension_loaded('mbstring')?'true':'false';");
		String extdir = execPhp("echo ini_get('extension_dir');");
		if (mbload.endsWith("false")) {
			File file = new File(extdir, "php_mbstring.dll");
			if (file.exists()) {
				phpcmd = "php -d extension=php_mbstring.dll";
			} else {
				phpcmd = "php -d extension=ext/php_mbstring.dll";
			}
		}
		String flag = execPhp("echo extension_loaded('mbstring')?'true':'false';");
		Assert.assertEquals("true", flag);
		js.eval("$import('org.xidea.jsidoc.util:*');");
		js.eval("$import('org.xidea.lite.impl.js:JSTranslator')");
		js.eval("$import('org.xidea.lite.impl.php:PHPTranslator')");
	}

	@Test
	public void testFor() throws TransformerConfigurationException,
			IOException, SAXException, TransformerException,
			TransformerFactoryConfigurationError {
		HashMap<String, String> sm = new HashMap<String, String>();
		sm
				.put(
						"/s.xhtml",
						"<p><c:for var='a' list='${[1,2,3]}'>"
								+ "${for.index}"
								+ "<c:for var='a' list='${[1,2,3]}'>${for.index}</c:for></c:for></p>");
		String expected = testTemplate(sm, this, "/s.xhtml", null);
		System.out.println(expected);
	}

	@Test
	public void testDate() throws TransformerConfigurationException,
			IOException, SAXException, TransformerException,
			TransformerFactoryConfigurationError {
		HashMap<String, String> sm = new HashMap<String, String>();
		sm
				.put(
						"/s.xhtml",
						"<c:def name='dateFormat(date)'><c:date-format pattern='YYYY-MM-DD hh:mm:ss' value='${date}'/></c:def>"
								+ "<div>今天是：${dateFormat(date)}	</div>");
		String expected = testTemplate(sm, this, "/s.xhtml", null);
		System.out.println(expected);
	}

	@Test
	public void testDef() throws TransformerConfigurationException,
			IOException, SAXException, TransformerException,
			TransformerFactoryConfigurationError {
		HashMap<String, String> sm = new HashMap<String, String>();
		sm.put("/s.xhtml",
				"<c:def name='dateFormat(date,arg2=1)'>${arg2}</c:def>"
						+ "<div>今天是：${dateFormat(date)}	</div>");
		String expected = testTemplate(sm, this, "/s.xhtml", null);
		System.out.println(expected);
	}

	static String testTemplate(Map<String, String> relativeSourceMap,
			Object context, String path, String expected) throws IOException,
			SAXException {
		return runTemplate(relativeSourceMap, context, path, expected).get(
				"#expect");
	}

	public static HashMap<String, String> runTemplate(
			Map<String, String> relativeSourceMap, Object context, String path,
			String expected) throws IOException, SAXException {
		ParseContext pc = buildContext(relativeSourceMap, path);
		Template tpl = new Template(pc.toList(), pc.getFeatureMap());
		String contextJSON;
		Object contextObject;
		if (context instanceof String) {
			contextObject = ExpressionFactory.getInstance().create(
					(String) context).evaluate(relativeSourceMap);
			contextJSON = JSONEncoder.encode(contextObject);
		} else {
			contextObject = context;
			contextJSON = JSONEncoder.encode(context);
		}
		StringWriter out = new StringWriter();
		tpl.render(contextObject, out);
		String javaresult = out.toString();
		String jsresult = runNativeJS(pc, contextJSON);
		String phpresult = runNativePHP(pc, contextJSON);
		expected = expected != null ? expected : jsresult;
		HashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("#model", contextJSON);
		result.put("#expect", expected);
		result.put("java", javaresult);
		result.put("js", jsresult);
		result.put("php", phpresult);
		if (!expected.equals(phpresult)) {
			LiteTest.printLatestPHP();
		}
		return result;
	}

	static void checkResultEqual(String source, String expected,
			Map<String, String> resultMap,Map<String, String> outputMap) throws IOException, SAXException {
		//Map<String, String> outputMap = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : resultMap.entrySet()) {
			if (!entry.getKey().startsWith("#")) {
				try {
					if (expected == null) {
						Assert.assertEquals(entry.getKey() + "运行结果有误：#"
								+ source, expected, entry.getValue());
					}
				} catch (Error e) {
					if (outputMap == null) {
						throw e;
					} else {
						outputMap.put(entry.getKey(), entry.getValue());
					}
				}

			}
		}
	}

	public static String normalizeXML(String result) throws IOException,
			SAXException {
		if (!result.trim().startsWith("<")) {
			return result;
		}
		try {
			result = new XMLNormalizeImpl() {
				{
					this.defaultNSMap = Collections.emptyMap();
				}

				public void appendScript(String content) {
					super.appendScript(replaceUnicode(content));
				}

				public void addAttr(String space, String name, String value,
						char qute) {
					if (name.startsWith("on")) {
						value = replaceUnicode(value);
					}
					super.addAttr(space, name, value, qute);
				}
			}.normalize(result, LiteTest.class.getName());
			Document doc = ParseUtil
					.loadXMLBySource(result, "normalize source");
			StringWriter out = new StringWriter();
			Transformer newTransformer = TransformerFactory.newInstance()
					.newTransformer();
			newTransformer.setOutputProperty(
					javax.xml.transform.OutputKeys.INDENT, "yes");
			newTransformer.transform(new DOMSource(doc), new StreamResult(out));
			result = out.toString();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;

	}

	public static String replaceUnicode(String content) {
		Pattern p = Pattern.compile("\\\\u([0-9a-f]{4})|\\\\+/");
		Matcher m = p.matcher(content);
		boolean result = m.find();
		if (result) {
			StringBuffer sb = new StringBuffer();
			do {
				String c = m.group(1);
				if (c != null && c.length() > 0) {
					m
							.appendReplacement(sb, ""
									+ (char) Integer.parseInt(c, 16));
				} else {
					c = m.group();
					if (c.length() % 2 == 0) {
						c = c.substring(0, c.length() - 2) + '/';
					}
					m.appendReplacement(sb, c);
				}
				result = m.find();
			} while (result);
			m.appendTail(sb);
			// System.out.println(m);
			return sb.toString();
		}
		return content;
	}

	private static ParseContext buildContext(
			Map<String, String> relativeSourceMap, String path)
			throws SAXException, IOException {
		ParseConfigMock config = new ParseConfigMock(relativeSourceMap);
		ParseContextImpl pc = new ParseContextImpl(config, path);
		URI uri = pc.createURI(path);
		Document xml = pc.loadXML(uri);
		pc.parse(xml);
		return pc;
	}

	private static class ParseConfigMock extends ParseConfigImpl {
		private Map<String, String> sourceMap;

		public ParseConfigMock(Map<String, String> sourceMap) {
			super(URI.create("test:///"), null);
			this.sourceMap = sourceMap;
		}

		public Document loadXML(URI uri) throws IOException, SAXException {
			String source = this.sourceMap.get(uri.getPath());
			String id = uri.toString();
			if (source == null) {
				System.out.println(id);
				System.out.println(uri.getPath());
				System.out.println(this.sourceMap.keySet());
			}
			source = ParseUtil.normalize(source, id);
			return ParseUtil.loadXMLBySource(source, id);
		}
	}

	private static String execPhp(final String code) {
		Process proc;
		try {
			proc = Runtime.getRuntime().exec(phpcmd);
			OutputStream out = proc.getOutputStream();
			out.write(("<?php\n" + code + "\nflush();exit();")
					.getBytes("UTF-8"));
			out.flush();
			currentPHP = code;
			out.close();
			// System.out.println(code);
			final InputStream error = proc.getErrorStream();
			Thread t = new Thread() {
				public void run() {
					int c;
					boolean e = false;
					try {
						while ((c = error.read()) >= 0) {
							e = true;
							System.out.print((char) c);
						}
					} catch (IOException ex) {
					}
					if (e) {
						System.out.println(code);
					}
				}
			};
			t.start();
			String flag = ParseUtil.loadTextAndClose(proc.getInputStream(),
					"utf-8");
			// t.interrupt();
			return flag;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String runNativeJS(ParseContext parsedContext,
			String contextJSON) {
		Object ts = js.eval("new JSTranslator('')");
		String code = (String) js.invoke(ts, "translate", js.eval("("
				+ JSONEncoder.encode(parsedContext.toList()) + ")"), true);
		// System.out.println(code);
		String jsResult = (String) js.eval("(function(){" + code + "})()("
				+ contextJSON + ")");
		return (jsResult);
	}

	public static String runNativePHP(ParseContext parsedContext,
			String contextJSON) {
		String litecode = JSONEncoder.encode(Arrays.asList(
				new ArrayList<String>(), parsedContext.toList(), parsedContext
						.getFeatureMap()));
		Object ts = js.eval("new PHPTranslator('/test'," + litecode + ")");
		String code = (String) js.invoke(ts, "translate");
		StringBuilder buf = new StringBuilder(code.replaceFirst("<\\?php", ""));
		URL f = ELTest.class.getResource("/lite/LiteEngine.php");
		try {
			buf
					.append("\nrequire_once('"
							+ new File(f.toURI()).toString().replace('\\', '/')
							+ "');");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		buf.append("\nlite_template_test(json_decode('");
		buf.append(contextJSON.replaceAll("['\\\\]", "\\\\$0"));
		buf.append("',true));");

		// System.out.println(buf);
		try {
			String result = execPhp(currentPHP = buf.toString());
			return result;
		} catch (RuntimeException e) {
			System.out.println("出错PHP脚本\n" + currentPHP);
			throw e;
		} catch (Error e) {
			System.out.println("出错PHP脚本\n" + currentPHP);
			throw e;
		}

	}

	public static void printLatestPHP() {
		System.out.println(currentPHP);
	}

}
