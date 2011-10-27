package org.xidea.lite.tools.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jside.webserver.CGIEnvironment;
import org.jside.webserver.CGIRunner;
import org.jside.webserver.MutiThreadWebServer;
import org.jside.webserver.RequestContext;
import org.jside.webserver.RequestUtil;
import org.jside.webserver.servlet.ServletContextImpl;
import org.w3c.dom.Document;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.jsi.web.JSIService;
import org.xidea.lite.LiteTemplate;
import org.xidea.lite.impl.HotLiteEngine;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.servlet.TemplateServlet;
import org.xidea.lite.tools.LiteCompiler;
import org.xidea.lite.tools.ResourceManagerImpl;

public class WebServer {
	public static void main(String[] args) throws Exception {

		Logger root = Logger.getLogger("classpath:");

		for (Handler h : root.getHandlers()) {
			Filter f = h.getFilter();
			if (!(f instanceof TitleFilter)) {
				h.setFilter(new TitleFilter(f));
			}
		}

		File webroot = new File(new File(WebServer.class.getResource("/")
				.toURI()), "../../").getCanonicalFile().getAbsoluteFile();
		// webroot = new File(webroot,"../build/dest/php-example");
		startServer(webroot);
	}

	private static long lastModified(List<File> list) {
		long t = 0;
		for (File f : list) {
			if (!f.exists()) {
				return -1;
			}
			t = Math.max(t, f.lastModified());
		}
		return t;
	}

	private static void startServer(final File root) throws IOException {
		final URI base = root.toURI();
		final JSIService js = new JSIService() {
			protected void addHeader(Object[] context, String key, String value) {
				RequestContext request = (RequestContext) context[0];
				request.setResponseHeader(key + ':' + value);
			}
		};
		// final LiteHander resourceHander = new LiteHander(root);
		js.addLib(new File(root, "WEB-INF/lib"));
		js.addSource(new File(root, "scripts"));
		js.addSource(new File(root, "WEB-INF/classes"));

		MutiThreadWebServer mtws = new MutiThreadWebServer(base) {
			private long lastModified = 0;
			ResourceManagerImpl manager;
			HotLiteEngine ht;
			final ServletContextImpl servletAdaptor = new ServletContextImpl(
					);

			TemplateServlet servlet = null;
			

			public void processRequest(RequestContext context) throws Exception {
				init(base);
				final String uri = context.getRequestURI();
				String rp = CGIEnvironment.toRealPath(base, uri);
				final String lite_compile_service = "/WEB-INF/service/lite-compile";
				if(uri.equals(lite_compile_service)){
					String path = context.getParam().get("path");
					String litecode = ht.getLitecode(path);
					String phpcode = LiteCompiler.buildPHP(path, litecode);
					String litecodepath = "/WEB-INF/litecode/" + path.replace('/', '^');
					writeFile(litecodepath, litecode.getBytes("UTF-8"));
					writeFile(litecodepath+".php", phpcode.getBytes(manager.getFeatureMap(path).get(LiteTemplate.FEATURE_ENCODING)));
					
				}else if (rp.endsWith(".php")) {
					Map<String, String> envp = new CGIEnvironment(context)
							.toMap(System.getenv());
					String compile_service = "http://127.0.0.1:"+context.getServer().getPort()+lite_compile_service;
					envp.put("LITE_COMPILE_SERVICE", compile_service);
					CGIRunner cr = new CGIRunner(context, "", envp,
							new File(new File(base), rp).getParentFile(), null);
					cr.setCgiExecutable("php-cgi.exe -d extension_dir=./ -d extension=ext/php_mbstring.dll".split("[\\s]+"));
					cr.run();
				} else {
					String prefix = "/scripts/";
					if (uri.startsWith(prefix)) {
						js.service(uri.substring(prefix.length()), context
								.getParams(), context.getOutputStream(),
								context);
					} else if (uri.endsWith(".xhtml") || uri.indexOf(".xhtml;/")>0) {
						boolean isSource = false;
						for (Cookie cookie : servletAdaptor.getCookies()) {
							if ("LITE_DEBUG".equals(cookie.getName())
									&& "source".equals(cookie.getValue())) {
								Document dom = manager.getFilteredDocument(uri);
								TransformerFactory trans = javax.xml.transform.TransformerFactory
										.newInstance();
								// StringWriter so = new StringWriter();
								trans.newTransformer().transform(
										new DOMSource(dom),
										new StreamResult(servletAdaptor
												.getWriter()));
								isSource = true;
								break;
							}

						}
						if(!isSource){
							Map<String,Object> data = loadData(root, uri);
							try{
								for(String key : data.keySet()){
									servletAdaptor.setAttribute(key, data.get(key));
								}
							}catch (Exception e) {
								RequestUtil.printResource(e, RequestUtil.PLAIN_TEXT);
								throw e;
							}
							servlet.service(servletAdaptor, servletAdaptor);
						}
						
					} else {
						File file = new File(root, uri);
						if (file.isDirectory()) {
							RequestUtil.printResource();
						} else {
							try {
								Object result = manager.getFilteredContent(uri);
								String contentType = null;
								if(uri.matches("\\.(?:vm|ftl)$")){
									contentType = "text/html";
								}
								RequestUtil.printResource(result, contentType);
							} catch (FileNotFoundException e) {
								context.setStatus(404, e.toString());
								RequestUtil.printResource(e.toString(),
										"text/html");
							}
						}
					}
				}
			}

			private void writeFile(String path,byte[] litecode) throws FileNotFoundException,
					IOException, UnsupportedEncodingException {
				File file = new File(root,path);
				file.getParentFile().mkdirs();
				FileOutputStream out1 = new FileOutputStream(file);
				try{
					out1.write(litecode);
					out1.flush();
				}finally{
					out1.close();
				}
			}

			private void init(final URI base) throws IOException, ServletException {
				long time = manager == null ? -1 : lastModified(manager
						.getScriptFileList());
				if (time < 0 || lastModified != time) {
					manager = new ResourceManagerImpl(base, base
							.resolve("WEB-INF/lite.xml"));
					ht = new HotLiteEngine((ParseConfig) manager, null);
					servlet = new TemplateServlet(){
						{
							init(servletAdaptor);
						}
						@Override
						public void initEngine(ServletConfig config){
							this.templateEngine = ht;
						}
					};
					final List<File> scriptFileList = manager
							.getScriptFileList();
					new File(new File(base), "WEB-INF")
							.listFiles(new FileFilter() {
								public boolean accept(File file) {
									String name = file.getName();
									if (file.isDirectory()) {
										if (!name.startsWith(".")) {
											file.listFiles(this);
										}
									} else {
										if (name.endsWith(".js")) {
											scriptFileList.add(file);
										}
									}
									return false;
								}

							});
					lastModified = lastModified(scriptFileList);
				}
			}
		};
		mtws.start();
	}

	// private static void startTestServer(File webroot) throws IOException,
	// MalformedURLException, ClassNotFoundException,
	// NoSuchMethodException, IllegalAccessException,
	// InvocationTargetException {
	// File jsiClasses = new File(webroot, "../../JSI2/web/WEB-INF/classes")
	// .getCanonicalFile();
	// File jsaClasses = new File(webroot, "../../JSA/classes")
	// .getCanonicalFile();
	// File targetClasses = new File(webroot,
	// "../../FileServer/web/WEB-INF/classes").getCanonicalFile();
	// // webroot = new
	// // File("D:\\workspace\\Lite2\\build\\dest\\php-example-20110706");
	//
	// URLClassLoader cl = new URLClassLoader(new URL[] {
	// jsiClasses.toURI().toURL(), jsaClasses.toURI().toURL(),
	// targetClasses.toURI().toURL() }, ClassLoader
	// .getSystemClassLoader());
	// Class<?> app = cl.loadClass("org.jside.webserver.test.WebServer");
	// Method method = app.getMethod("main", String[].class);
	// method.invoke(null, (Object) (new String[] { webroot.toString() }));
	// }

	private static Map<String,Object> loadData(final File root, String uri)
			throws IOException {
		String jsonpath = uri.replaceFirst(".\\w+$", ".json");
		HashMap<String,Object> data = new HashMap<String, Object>();
		if (jsonpath.endsWith(".json")) {
			File df = new File(root, jsonpath);
			if (df.exists()) {
				String source = ParseUtil.loadTextAndClose(new FileInputStream(
						df), null);
				data = (HashMap<String,Object>)ExpressionFactoryImpl.getInstance().create(source).evaluate(
						data);
			}
		}
		return data;
	}

	static class TitleFilter implements Filter {
		Filter base;

		public TitleFilter(Filter f) {
			base = f;
		}

		public boolean isLoggable(LogRecord record) {
			String loggerName = record.getLoggerName();
			String className = record.getSourceClassName();
			String methodName = record.getSourceMethodName();
			if (className.equals(RuntimeSupport.class.getName())
					&& methodName.equals("log")) {
				className = loggerName;
			}
			record.setSourceClassName(className);

			// String pos = FireKylinParser.getCurrentPosition();
			// if (pos == null) {
			// pos = FireKylinCompiler.getCurrentFile();
			// }
			// if (pos != null) {
			// record.setSourceMethodName("." + record.getSourceMethodName() +
			// "\t[lite-src]" + pos.replaceFirst(".*[\\/]", ""));
			// }
			return base != null ? base.isLoggable(record) : true;
		}
	}

}
