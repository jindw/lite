package org.xidea.lite.test.app;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.test.webserver.MutiThreadWebServer;
import org.xidea.lite.test.webserver.RequestHandle;


public class SimpleWebServer extends MutiThreadWebServer {
	public static final String INDEX_XHTML = "index.xhtml";
	public static final String POST_FIX_XHTML = ".xhtml";
	protected File webBase ;
	protected TemplateEngine engine;
	protected long lastAcessTime = System.currentTimeMillis();
	public SimpleWebServer(File webBase){
		reset(webBase);
	}
	public void reset(File webBase){
		this.webBase = webBase;
		engine = new TemplateEngine(webBase);
	}
	protected void processRequest(RequestHandle handle) throws IOException {
		lastAcessTime = System.currentTimeMillis();
		String url = handle.getRequestURI();
		File file = new File(webBase,url);
		if(file.isDirectory()){
			File index = new File(file,INDEX_XHTML);
			if(index.exists()){
				url = url.endsWith("/")?INDEX_XHTML:"/"+INDEX_XHTML;
				file = index;
			}
		}
		if(url.endsWith(POST_FIX_XHTML)){
			Writer out = new StringWriter(); 
			File json = new File(webBase,url.substring(0,url.lastIndexOf('.'))+".json");
			Object object = null;
			if(json.exists()){
				String text = loadText(json); 
				if(text.startsWith("\uFEFF")){
					text = text.substring(1);
				}
				object = JSONDecoder.decode(text);
			}else{
				object = new Object();
			}
			engine.render(url, object, out);
			handle.printContext(out, "text/html");
			return;
		}
		if(file.isDirectory() && !url.endsWith("/")){
			handle.printRederect(url+'/');
		}else{
		     handle.printFile(file);
		}
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
	protected void closeIfTimeout(int time) {
		long passed = System.currentTimeMillis() - this.lastAcessTime;
		if(passed > time){
			System.out.println("timeout and close server:"+time);
			System.exit(1);
		}else{
			//System.out.println("wait time:" +passed);
		}
	}
	public static void main(String[] a) throws Exception {
		File root = new File(".");
		if(new File(root,"web/WEB-INF").exists()){
			root = new File(root,"web");
		}
		final SimpleWebServer server = new SimpleWebServer(root.getAbsoluteFile());
		server.start();
		
		new Timer().schedule(new TimerTask(){

			@Override
			public void run() {
				server.closeIfTimeout(1000*60*5);
			}
			
		}, 1000*60,1000*60);
		Desktop.getDesktop().browse(new URI("http://localhost:"+server.getPort()));
	}
}
