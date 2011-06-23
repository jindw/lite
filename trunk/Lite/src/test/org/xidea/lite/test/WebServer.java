package org.xidea.lite.test;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class WebServer {
	public static void main(String[] args) throws Exception{
		
		File webroot = new File(new File(WebServer.class.getResource("/").toURI()),"../../").getCanonicalFile().getAbsoluteFile();
		File targetClasses = new File(webroot,"../../FileServer/web/WEB-INF/classes").getCanonicalFile();
		//webroot = new File("D:\\workspace\\TT\\web\\lite2php-smarty");
		
		URLClassLoader cl = new URLClassLoader(new URL[]{targetClasses.toURI().toURL()});
		Class<?> app = cl.loadClass("org.jside.webserver.test.WebServer");
		Method method = app.getMethod("main", String[].class);
		method.invoke(null, (Object)(new String[]{webroot.toString()}));
	}

}
