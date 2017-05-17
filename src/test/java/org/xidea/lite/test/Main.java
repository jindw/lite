package org.xidea.lite.test;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.HotLiteEngine;
import org.xidea.lite.LiteCompiler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main {

	static{
		try {
			File root = new File(new File(AutoLazyWidgetTest.class.getResource("/")
						.toURI()), "../../");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args) throws Exception {

		File root = new File("./");
		File cached = new File("./.litecode/");

		HotLiteEngine hotEngine = new HotLiteEngine(root, cached);
		start(hotEngine);
		testScript(hotEngine);

	}


	private static void start(final HotLiteEngine engine) throws IOException {
		final int port = 2000;
		HttpServer httpserver = HttpServer.create(new InetSocketAddress(port),
				100);

		httpserver.createContext("/", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				exchange.sendResponseHeaders(200, 0);
				Headers headers = exchange.getResponseHeaders();
				OutputStream body = exchange.getResponseBody();
				String path = exchange.getRequestURI().getPath();
				OutputStreamWriter out = new OutputStreamWriter(body);
				if(path.endsWith(".xhtml")){
					engine.render(path, new HashMap(), out);
				}else{
					File file = new File(engine.getRoot(),path.substring(1));
					if(file.isDirectory()){
						File[] files = file.listFiles();
						for(File f: files){
							String name = f.getName();
							if(f.isDirectory()){
								name+='/';
							}
							out.write("<a href='"+name+"'>"+name+"</a><hr>");
						}
						out.flush();
					}else if(file.isFile()){
						FileInputStream in = new FileInputStream(file);
						int b = in.read();
						while(b>=0){
							body.write(b);
							b = in.read();
						}
					}
				}
				out.close();
			}
		});
		httpserver.setExecutor(null);
		httpserver.start();
		System.out.println("server started:"+port);
	}

	private static void testScript(HotLiteEngine hotEngine)
			throws NoSuchFieldException, IllegalAccessException,
			ScriptException {
		Field compilerField = HotLiteEngine.class.getDeclaredField("compiler");
		compilerField.setAccessible(true);
		LiteCompiler compiler = (LiteCompiler) compilerField.get(hotEngine);
		Field engineField = LiteCompiler.class.getDeclaredField("engine");
		engineField.setAccessible(true);
		ScriptEngine engine = (ScriptEngine) engineField.get(compiler);
		engine.eval("for(var n in require.cached){" + "console.log(n);" + "}");
	}
	public static void testCompile(String[] args) throws ScriptException, IOException {
		File root = new File("./");
		File cached = new File("./.litecode/");
		LiteCompiler compiler = new LiteCompiler(root);
		HotLiteEngine engine = new HotLiteEngine(root, cached);
		String path = "/doc/guide/index.xhtml";
		Object context = new HashMap();
		long t1 = System.currentTimeMillis();
		StringWriter out = new StringWriter();
		engine.render(path, context, out);
		String result = out.toString();
		long t2 = System.currentTimeMillis();
		out = new StringWriter();
		engine.render(path, context, out);
		result = out.toString();
		long t3 = System.currentTimeMillis();
		String result3 = compiler.compile("/doc/guide/index.xhtml");
		long t4 = System.currentTimeMillis();
		String result4 = compiler.compile("/doc/guide/index.xhtml");
		long t5 = System.currentTimeMillis();
		System.out.println(result);
		System.out.println(t5 - t4);
		System.out.println(t3 - t4);
		System.out.println(t3 - t2);
		System.out.println(t2 - t1);
	}

	static String[] casefiles = { "if-case.xml", "choose-case.xml",
			"for-case.xml", "def-case.xml", "extends-case.xml",
			"include-case.xml", "extension-case.xml", "out-case.xml" };

	public static void testTestCases(File root) throws Exception {
		
		File dest = new File(root, "doc/test-data/test-syntax.json");
		Writer out = new OutputStreamWriter(new FileOutputStream(dest));
		try {

			ArrayList<Object> allResult = new ArrayList<Object>();
			for (String file : casefiles) {
				Map<String, List<Object[]>> cases = AutoSyntaxTest
						.loadCases(file);
				for (Map.Entry<String, List<Object[]>> unitEntry : cases
						.entrySet()) {
					String title = unitEntry.getKey();
					ArrayList<Object> unitResult = new ArrayList<Object>();
					unitResult.add(title);
					for (Object[] args : unitEntry.getValue()) {
						Map<String, String> sourceMap = (Map<String, String>) args[0];
						String model = (String) args[1];
						String expect = (String) args[2];
						boolean format = (Boolean) args[3];
						HashMap<String, Object> ctx = JSONDecoder.decode(model);
						Map<String, String> resultMap = LiteTest.runTemplate(
								sourceMap, ctx, AutoSyntaxTest.TEST_XHTML,
								expect);
						HashMap<String, String> info = new HashMap<String, String>();
						info.put("source",
								sourceMap.get(AutoSyntaxTest.TEST_XHTML));
						info.put("model", model);
						info.put("expect", expect);
						String formatedExpected = null;
						for (Map.Entry<String, String> entry : resultMap
								.entrySet()) {
							if (!entry.getKey().startsWith("#")) {
								String value = entry.getValue();
								if (expect == null) {
									expect = value;
								}
								if (expect.equals(value)) {
								} else {
									if (format) {
										expect = formatedExpected == null ? (formatedExpected = LiteTest
												.normalizeXML(expect))
												: formatedExpected;
										value = LiteTest.normalizeXML(value);
									}
									if (!expect.equals(value)) {
										info.put(entry.getKey(), value);
									}
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
