package org.xidea.lite.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.FutureWaitStack;
import org.xidea.lite.LiteCompiler;
import org.xidea.lite.LiteTemplate;
import org.xidea.lite.Template;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LiteTest {
	private static String phpcmd = "php";
	private static String currentPHP;
	private static DocumentBuilder documentBuilder;
	public static final ScriptEngine engine;
	private static ExecutorService executorService =  Executors.newScheduledThreadPool(8);
	private static final Log log = LogFactory.getLog(LiteTest.class);
	public final static File projectRoot;
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
		org.junit.Assert.assertEquals("true", flag);

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			// factory.setExpandEntityReferences(false);
			factory.setCoalescing(false);
			// factory.setXIncludeAware(true);
			documentBuilder = factory.newDocumentBuilder();
			// documentBuilder.setEntityResolver(new DefaultEntityResolver());
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}

		try {

            URL base = LiteTest.class.getResource("/");

            File root = new File(base.toURI());
            while(true){
                root = root.getParentFile();
                if(new File(root,"package.json").exists()){
                    break;
                }
            }
            projectRoot  = root;

			engine = new ScriptEngineManager().getEngineByExtension("js");
			engine.eval(new InputStreamReader(new FileInputStream(new File(root,"src/main/java/org/xidea/lite/java-proxy.js"))));
			engine.eval(new InputStreamReader(new FileInputStream(new File(root,"src/test/java/org/xidea/lite/test/test.js"))));
			//System.out.println(base.toURI());
			engine.eval("initTest("
					+ JSONEncoder.encode(root.getAbsolutePath()) + ")");
			// engine.eval("var liteCompiler = new LiteCompiler(root)");
			// xwengine.eval("liteCompiler.translator = null;");
			///Users/jinjinyun/Documents/workspace/node_modules/lite
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		// CHARSETS = cs.toArray(new String[cs.size()]);
	}

	@Test
	public void testFor() throws IOException, SAXException {
		HashMap<String, String> sm = new HashMap<String, String>();
		sm.put("/s.xhtml",
				"<p><c:for var='a' list='${[1,2,3]}'>"
						+ "${for.index}"
						+ "<c:for var='a' list='${[1,2,3]}'>${for.index}</c:for></c:for></p>");

		HashMap<String, Object> context = new HashMap<String, Object>();
		context.put("date", new Date().getTime());
		String expected = testTemplate(sm, context, "/s.xhtml", null);
		log.info(expected);
	}

	@Test
	public void testDate() throws IOException, SAXException {
		HashMap<String, String> sm = new HashMap<String, String>();
		sm.put("/s.xhtml",
				"<c:def name='dateFormat(date)'><c:date-format pattern='YYYY-MM-DD hh:mm:ss' value='${date}'/></c:def>"
						+ "<div>今天是：${dateFormat(date)}	</div>");

		HashMap<String, Object> context = new HashMap<String, Object>();
		context.put("date", new Date().getTime());
		String expected = testTemplate(sm, context, "/s.xhtml", null);
		log.info(expected);
	}

	@Test
	public void testDef() throws IOException, SAXException {
		HashMap<String, String> sm = new HashMap<String, String>();
		sm.put("/s.xhtml",
				"<c:def name='dateFormat(date,arg2=1)'>${arg2}</c:def>"
						+ "<div>今天是：${dateFormat(date)}	</div>");
		HashMap<String, Object> context = new HashMap<String, Object>();
		context.put("date", new Date());
		String expected = testTemplate(sm, context, "/s.xhtml", null);
		log.info(expected);
	}

	static String testTemplate(Map<String, String> relativeSourceMap,
			HashMap<String,Object> context, String path, String expected) throws IOException,
			SAXException {
		return runTemplate(relativeSourceMap, context, path, expected).get(
				"#expect");
	}
	static ExecutorService executor =java.util.concurrent.Executors.newSingleThreadExecutor();

	public static HashMap<String,Object> randomWaitWrap( HashMap<String,Object> contextObject){
		for (Map.Entry<String,Object> entry:contextObject.entrySet()){
			final Object value = entry.getValue();
			if(FutureWaitStack.futureClass!= null && !FutureWaitStack.futureClass.isInstance(value) &&
					Math.random()>0) {
				entry.setValue(executor.submit(new Callable<Object>() {
					public Object call() throws Exception{
						//System.out.println("!! wait :10"+value);
						Thread.sleep(10);
						return value;
					}
				}));
			}
		}
		return contextObject;

	}

	/**
	 * 
	 * @param relativeSourceMap
	 * @param contextObject
	 * @param path
	 * @param expect
	 * @return value is unformat
	 * @throws IOException
	 * @throws SAXException
	 */
	public static HashMap<String, String> runTemplate(
			Map<String, String> relativeSourceMap, HashMap<String,Object> contextObject, String path,
			String expect) throws IOException, SAXException {
		try{
		// ParseContext pc = buildContext(relativeSourceMap, path);
		String compileResult = (String) engine.eval("buildTemplate("
				+ JSONEncoder.encode(relativeSourceMap) + ","
				+ JSONEncoder.encode(path) + ")");
		Map<String, Object> compileResultMap = JSONDecoder
				.decode(compileResult);
		Map<String, String> config = (Map<String, String>) compileResultMap
				.get("config");
		List<Object> litecode = (List<Object>) compileResultMap.get("litecode");
		//System.out.println(litecode);
		//System.out.println(config);
		Template tpl = new LiteTemplate(executorService,litecode, config);
		String contextJSON = JSONEncoder.encode(contextObject);
		StringWriter out = new StringWriter();

		tpl.render(randomWaitWrap(contextObject), out);
		String javaresult = out.toString();
		String jsresult = runNativeJS(compileResult, contextJSON);
		String phpresult = runNativePHP(compileResult, contextJSON);
		expect = expect != null ? expect : jsresult;
		HashMap<String, String> result = new LinkedHashMap<String, String>();

		//expect = normalizeXML(expect);
		//phpresult = normalizeXML(phpresult);
		//jsresult = normalizeXML(jsresult);
		//javaresult = normalizeXML(javaresult);
		result.put("#model", contextJSON);
		result.put("#expect", expect);
		result.put("js", jsresult);
		result.put("java", javaresult);
		result.put("php", phpresult);
//		if (!expect.equals(phpresult)) {
//			if (!expect.equals(phpresult)) {
//				System.out.println("expect:" + expect);
//				System.out.println("php:" + phpresult);
//				LiteTest.printLatestPHP();
//			}
//		}
		return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String normalizeXML(String result) throws IOException {
		result = result.trim();
			try {
				return (String) engine.eval("formatXML("+JSONEncoder.encode(result)+")");
			} catch (ScriptException e) {
				e.printStackTrace();
				return result;
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
			String flag = loadTextAndClose(proc.getInputStream(), "utf-8");
			// t.interrupt();
			return flag;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	static String loadTextAndClose(InputStream in, String charset)
			throws IOException {
		InputStreamReader s = new InputStreamReader(in, charset);
		char[] cbuf = new char[1024];
		StringBuilder buf = new StringBuilder();
		int c;
		while ((c = s.read(cbuf)) >= 0) {
			buf.append(cbuf, 0, c);
		}
		return buf.toString();

	}

	public static String runNativeJS(String compileResult, String contextJSON) {
		String code=null ,source = null;
		try {
			code = (String) engine.eval("getJsCode(" + compileResult
					+ ")");
			// System.out.println(code);
			source = "(function(){return " + code + "})()(" + contextJSON + ")";

			String jsResult = (String) engine.eval(source);

			//System.out.println(compileResult+'\n'+code+'\n'+source);
			return (jsResult);
		} catch (Exception e) {
			System.out.println(compileResult+'\n'+code+'\n'+source);
			throw new RuntimeException(e);
		}
	}

	public static String runNativePHP(String compileResult, String contextJSON) {
		StringBuilder buf = new StringBuilder();
		try {
			String code = (String) engine.eval("getPhpCode(" + compileResult
					+ ")");
			buf.append(code.replaceFirst("<\\?php",
					""));
			URL f = LiteTest.class.getResource("/src/main/php/LiteEngine.php");
			//System.out.println(f);
			File file = new File(projectRoot,"src/main/php/LiteEngine.php");//f.toURI());
			buf.append("\nrequire_once('"
					+ file.toString().replace('\\', '/') + "');");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		buf.append("\nlite_template_test_xhtml(json_decode('");
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

	public static InputStream openStream(URI uri) throws IOException {
		try {
			if ("data".equalsIgnoreCase(uri.getScheme())) {
				String data = uri.getRawSchemeSpecificPart();
				int p = data.indexOf(',') + 1;
				String h = data.substring(0, p).toLowerCase();
				String charset = "UTF-8";
				data = data.substring(p);
				p = h.indexOf("charset=");
				if (p > 0) {
					charset = h.substring(h.indexOf('=', p) + 1,
							h.indexOf(',', p));
				}
				return new ByteArrayInputStream(URLDecoder
						.decode(data, charset).getBytes(charset));
				// charset=
			} else if ("classpath".equalsIgnoreCase(uri.getScheme())) {// classpath:///
				ClassLoader cl = LiteTest.class.getClassLoader();
				uri = uri.normalize();
				String path = uri.getPath();
				path = path.substring(1);
				InputStream in = cl.getResourceAsStream(path);
				if (in == null) {
					ClassLoader cl2 = Thread.currentThread()
							.getContextClassLoader();
					if (cl2 != null) {
						in = cl2.getResourceAsStream(path);
					}
				}
				return in;
			} else {
				if ("file".equals(uri.getScheme())) {
					File f = new File(uri);
					if (f.exists()) {
						return new FileInputStream(f);
					} else {
						return null;
					}
				}
				return uri.toURL().openStream();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Document loadXML(InputStream inputStream)
			throws SAXException, IOException {
		return documentBuilder.parse(inputStream);
	}

	public static Document loadXML(String path) throws SAXException,
			IOException {
		if (path.startsWith("<")) {
			InputStream inputStream = new ByteArrayInputStream(
					path.getBytes("utf-8"));
			return documentBuilder.parse(inputStream);
		} else {

			URI uri = URI.create(path);
			InputStream inputStream = openStream(uri);
			return documentBuilder.parse(inputStream);

		}
	}

}