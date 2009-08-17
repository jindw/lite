package org.jside.server;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;

import org.jside.server.web.TemplateEngine;
import org.jside.webserver.MutiThreadWebServer;
import org.jside.webserver.RequestContext;

public class FileServer extends MutiThreadWebServer {

	private TemplateEngine engine = new TemplateEngine();

	public FileServer() {
		super("file:///");
	}

	@Override
	public void processRequest(RequestContext context) throws Exception {
		URL res = new URL(webBase, context.getRequestURI().substring(1));
		if (res.getProtocol().equals("file")) {
			File file = getFile(res);
			if (file.isDirectory()) {
				OutputStreamWriter out = new OutputStreamWriter(context
						.getOutputStream(), "UTF-8");
				File[] list = file.listFiles();
				HashMap<String, Object> data = new HashMap<String, Object>();
				data.put("path", file.getAbsoluteFile());
				data.put("fileList", list);
				context.setContentType("text/html;charset=utf-8");
				try {
					engine.render("dir.xhtml", data, out);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			} else {
				super.processRequest(context);
			}
		} else {
			super.processRequest(context);
		}
	}

	private File getFile(URL res) {
		try {
			return new File(URLDecoder.decode(res.getFile(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

}
