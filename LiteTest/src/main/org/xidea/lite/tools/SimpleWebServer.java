package org.xidea.lite.tools;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.tools.webserver.MutiThreadWebServer;
import org.xidea.lite.tools.webserver.RequestHandle;

public class SimpleWebServer extends MutiThreadWebServer {
	public static final String INDEX_XHTML = "index.xhtml";
	public static final String POST_FIX_XHTML = ".xhtml";
	protected File webBase;
	protected TemplateEngine engine;
	protected long lastAcessTime = System.currentTimeMillis();

	public SimpleWebServer(File webBase) {
		reset(webBase);
	}

	public void reset(File webBase) {
		this.webBase = webBase;
		engine = new TemplateEngine(webBase){
			protected URL getResource(String pagePath) throws MalformedURLException {
				File file = new File(webRoot, pagePath);
				if(file.exists()){
					return file.toURI().toURL();
				}else{
					return this.getClass().getResource(pagePath);
				}
			}
		};
	}

	protected void processRequest(RequestHandle handle) throws IOException {
		lastAcessTime = System.currentTimeMillis();
		String url = handle.getRequestURI();
		File file = new File(webBase, url);
		if (file.exists()) {

			if (file.isDirectory()) {
				File index = new File(file, INDEX_XHTML);
				if (index.exists()) {
					url = url.endsWith("/") ? INDEX_XHTML : "/" + INDEX_XHTML;
					file = index;
				}
			}
			if (url.endsWith(POST_FIX_XHTML)) {
				Writer out = new StringWriter();
				File json = new File(webBase, url.substring(0, url
						.lastIndexOf('.'))
						+ ".json");
				Map<Object, Object> object = null;
				if (json.exists()) {
					String text = loadText(new FileInputStream(json));
					if (text.startsWith("\uFEFF")) {
						text = text.substring(1);
					}
					object = (Map)JSONDecoder.decode(text);
				} else {
					object = new HashMap<Object, Object>();
				}
				object.put("requestURI",url);
				engine.render(url, object, out);
				handle.printContext(out, "text/html");
				return;
			}
			if (file.isDirectory() && !url.endsWith("/")) {
				handle.printRederect(url + '/');
			} else {
				handle.printFile(file);
			}
		}else{
			handle.printContext(loadText(this.getClass().getResourceAsStream(url)), "text/html");
		}
	}

	public String loadText(InputStream ins) {
		try {
			Reader in = new InputStreamReader(ins, "utf-8");
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
		if (passed > time) {
			System.out.println("timeout and close server:" + time);
			System.exit(1);
		} else {
			// System.out.println("wait time:" +passed);
		}
	}

	public static void main(String[] a) throws Exception {
		File root = new File(".");
		if (new File(root, "web/WEB-INF").exists()) {
			root = new File(root, "web");
		}
		final SimpleWebServer server = new SimpleWebServer(root
				.getAbsoluteFile());
		server.start();

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				server.closeIfTimeout(1000 * 60 * 5);
			}

		}, 1000 * 60, 1000 * 60);
		Desktop.getDesktop().browse(
				new URI("http://localhost:" + server.getPort()));
	}
}
