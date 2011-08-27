package org.xidea.lite.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jside.webserver.CGIEnvironment;
import org.jside.webserver.CGIRunner;
import org.jside.webserver.MutiThreadWebServer;
import org.jside.webserver.RequestContext;
import org.jside.webserver.RequestUtil;
import org.w3c.dom.Document;
import org.xidea.el.ExpressionFactory;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.jsi.web.JSIService;
import org.xidea.lite.impl.HotTemplateEngine;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
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
		//webroot = new File(webroot,"../build/dest/php-example");
		startServer(webroot);
	}
	private static long lastModified(List<File> list){
		long t = 0;
		for (File f :list) {
			if(!f.exists()){
				return -1;
			}
			t = Math.max(t,f.lastModified());
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
//		final LiteHander resourceHander = new LiteHander(root);
		js.addLib(new File(root, "WEB-INF/lib"));
		js.addSource(new File(root, "scripts"));
		js.addSource(new File(root, "WEB-INF/classes"));
		
		MutiThreadWebServer mtws = new MutiThreadWebServer(base) {
			private long lastModified = 0;
			ResourceManagerImpl manager;
			HotTemplateEngine ht;
			
			public void processRequest(RequestContext context) throws Exception {
				init(base);
				final String uri = context.getRequestURI();
				String rp = CGIEnvironment.toRealPath(base, uri);
				if (rp.endsWith(".php")) {
					Map<String, String> envp = new CGIEnvironment(context)
							.toMap(null);
					CGIRunner cr = new CGIRunner(context, "php-cgi.exe", envp,
							new File(new File(base), rp).getParentFile(), null);
					cr.run();
				} else {
					String prefix = "/scripts/";
					if (uri.startsWith(prefix)) {
						js.service(uri.substring(prefix.length()), context
								.getParams(), context.getOutputStream(),
								context);
					} else if (uri.endsWith(".xhtml")) {
						OutputStream os = context.getOutputStream();
						Map<String, String> fm = ((ParseConfig)manager).getFeatureMap(uri);
						String encoding = fm.get(ParseContext.FEATURE_ENCODING);
						context.setEncoding(encoding);
						String mimeType = fm
								.get(ParseContext.FEATURE_MIME_TYPE);
						context.setMimeType(mimeType == null ? "text/html"
								: mimeType);
						OutputStreamWriter out = new OutputStreamWriter(os,
								encoding);
						Object data = loadData(root, uri);
						try{
							if(context.getParam().containsKey("@source")){
								Document dom = manager.getFilteredDocument(uri);
								TransformerFactory trans = javax.xml.transform.TransformerFactory.newInstance();
//								StringWriter so = new StringWriter();
								trans.newTransformer().transform(new DOMSource(dom),
										new StreamResult(out));
							}else{
								ht.render(uri, data, out);
							}
						}catch (Exception e) {
							RequestUtil.printResource(e, RequestUtil.PLAIN_TEXT);
							throw e;
						}
						out.flush();
					} else {
						File file = new File(root,uri);
						if(file.isDirectory()){
							RequestUtil.printResource();
						}else{
							try{
								Object result = manager.getFilteredContent(uri);
								RequestUtil.printResource(result,null);
							}catch (FileNotFoundException e) {
								context.setStatus(404, e.toString());
								RequestUtil.printResource(e.toString(), "text/html");
							}
						}
					}
				}
			}

			private void init(final URI base) throws IOException {
				long time = manager == null? -1:lastModified(manager.getScriptFileList());
				if(time <0 || lastModified != time){
					manager = new ResourceManagerImpl(base, base.resolve("WEB-INF/lite.xml"));
					ht = new HotTemplateEngine((ParseConfig)manager,null);
					final List<File> scriptFileList = manager.getScriptFileList();
					new File(new File(base),"WEB-INF").listFiles(new FileFilter() {
						public boolean accept(File file) {
							String name = file.getName();
							if (file.isDirectory()) {
								if (!name.startsWith(".")) {
									file.listFiles(this);
								}
							} else {
								if(name.endsWith(".js")){
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

//	private static void startTestServer(File webroot) throws IOException,
//			MalformedURLException, ClassNotFoundException,
//			NoSuchMethodException, IllegalAccessException,
//			InvocationTargetException {
//		File jsiClasses = new File(webroot, "../../JSI2/web/WEB-INF/classes")
//				.getCanonicalFile();
//		File jsaClasses = new File(webroot, "../../JSA/classes")
//				.getCanonicalFile();
//		File targetClasses = new File(webroot,
//				"../../FileServer/web/WEB-INF/classes").getCanonicalFile();
//		// webroot = new
//		// File("D:\\workspace\\Lite2\\build\\dest\\php-example-20110706");
//
//		URLClassLoader cl = new URLClassLoader(new URL[] {
//				jsiClasses.toURI().toURL(), jsaClasses.toURI().toURL(),
//				targetClasses.toURI().toURL() }, ClassLoader
//				.getSystemClassLoader());
//		Class<?> app = cl.loadClass("org.jside.webserver.test.WebServer");
//		Method method = app.getMethod("main", String[].class);
//		method.invoke(null, (Object) (new String[] { webroot.toString() }));
//	}

	private static Object loadData(final File root, String uri)
			throws IOException {
		String jsonpath = uri.replaceFirst(".\\w+$", ".json");
		Object data = new Object();
		if (jsonpath.endsWith(".json")) {
			File df = new File(root, jsonpath);
			if (df.exists()) {
				String source = ParseUtil.loadTextAndClose(new FileInputStream(
						df), null);
				data = ExpressionFactory.getInstance().create(source).evaluate(
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
