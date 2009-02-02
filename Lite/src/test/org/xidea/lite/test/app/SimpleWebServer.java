package org.xidea.lite.test.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.test.webserver.MutiThreadWebServer;
import org.xidea.lite.test.webserver.RequestHandle;


public class SimpleWebServer extends MutiThreadWebServer {
	private static final String INDEX_XHTML = "index.xhtml";
	private File webBase ;
	private TemplateEngine engine;
	public SimpleWebServer(File webBase){
		this.webBase = webBase;
		engine = new TemplateEngine(webBase);
	}
	protected void processRequest(RequestHandle handle) throws IOException {
		String url = handle.getRequestURI().substring(1);
		File file = new File(webBase,url);
		if(file.isDirectory()){
			File index = new File(file,INDEX_XHTML);
			if(index.exists()){
				url = url.endsWith("/")?INDEX_XHTML:"/"+INDEX_XHTML;
				file = index;
			}
		}
		if(url.endsWith(INDEX_XHTML)){
			Writer out = new StringWriter(); 
			File json = new File(webBase,url.substring(0,url.lastIndexOf('.'))+".json");
			Object object = null;
			if(json.exists()){
				String text = loadText(json); 
				if(text.startsWith("\uFEFF")){
					text = text.substring(1);
				}
				object = JSONDecoder.decode(text);
			}
			engine.render(url, object, out);
			handle.printContext(out, "text/html");
			return;
		}
		handle.printFile(file);
	}
	public String loadText(File file) {
		try {
			Reader in = new InputStreamReader(new FileInputStream(file),
					"utf-8");
			StringBuilder buf = new StringBuilder();
			char[] cbuf = new char[1024];
			for (int len = in.read(cbuf); len > 0; len = in.read(cbuf)) {
				buf.append(cbuf, 0, len);
			}
			return buf.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static void main(String[] a) throws Exception {
		SimpleWebServer server = new SimpleWebServer(new File(".").getAbsoluteFile());
		server.start();
	}
}
