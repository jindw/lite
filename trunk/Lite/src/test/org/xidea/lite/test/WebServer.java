package org.xidea.lite.test;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.xidea.jsi.impl.RuntimeSupport;

public class WebServer {
	public static void main(String[] args) throws Exception{
		
		Logger root = Logger.getLogger("classpath:");

			for (Handler h : root.getHandlers()) {
				Filter f = h.getFilter();
				if (!(f instanceof TitleFilter)) {
					h.setFilter(new TitleFilter(f));
				}
			}
		
		
		File webroot = new File(new File(WebServer.class.getResource("/").toURI()),"../../").getCanonicalFile().getAbsoluteFile();
		File targetClasses = new File(webroot,"../../FileServer/web/WEB-INF/classes").getCanonicalFile();
		//webroot = new File("D:\\workspace\\Lite2\\build\\dest\\php-example-20110706");
		
		URLClassLoader cl = new URLClassLoader(new URL[]{targetClasses.toURI().toURL()});
		Class<?> app = cl.loadClass("org.jside.webserver.test.WebServer");
		Method method = app.getMethod("main", String[].class);
		method.invoke(null, (Object)(new String[]{webroot.toString()}));
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
			if (className.equals(RuntimeSupport.class.getName()) && methodName.equals("log")) {
				className = loggerName;
			}
			record.setSourceClassName(className);

//				String pos = FireKylinParser.getCurrentPosition();
//				if (pos == null) {
//					pos = FireKylinCompiler.getCurrentFile();
//				}
//				if (pos != null) {
//					record.setSourceMethodName("." + record.getSourceMethodName() + "\t[lite-src]" + pos.replaceFirst(".*[\\/]", ""));
//				}
			return base != null ? base.isLoggable(record) : true;
		}
	}

}
