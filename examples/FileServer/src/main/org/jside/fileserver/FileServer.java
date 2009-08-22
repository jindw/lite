package org.jside.fileserver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;

import org.jside.webserver.RequestContext;
import org.jside.webserver.action.ActionWebServer;
import org.jside.webserver.action.TemplateAction;

public class FileServer extends ActionWebServer {

	private TemplateAction engine = TemplateAction.getInstance();

	public FileServer() throws MalformedURLException {
		super(new URL("file:///"));
		//this.addAction("/filemanager/delete.action", this);
		
	}

	@Override
	public void processRequest(RequestContext context) throws IOException{
		URL res = new URL(webBase, context.getRequestURI().substring(1));
		if (res.getProtocol().equals("file")) {
			String path = res.getFile();
			String pathInfo = null;
			int p = path.lastIndexOf(';');
			if(p>0){
				pathInfo = path.substring(p+1);
				path = path.substring(0, p);
			}
			File file = getFile(path);
			if(context.getMethod().equals("POST")){
				File dest = file;
				if("delete".equals(pathInfo)){
					file.delete();
				}else if("move".equals(pathInfo)){
					dest = new File(context.getParam().get("path"),file.getName());
					file.renameTo(dest);
				}else if("rename".equals(pathInfo)){
					dest = new File(file.getParentFile(),context.getParam().get("name"));
					file.renameTo(dest);
				}
				if(dest.isFile()){
					dest = dest.getParentFile();
				}
				file = dest;
				//TODO.redirect
			}
			if (file.isDirectory()) {
				File[] list = file.listFiles();
				HashMap<String, Object> data = new HashMap<String, Object>();
				data.put("path", file.getCanonicalFile().getAbsolutePath().replace('\\', '/'));
				data.put("fileList", list);
				context.setContentType("text/html;charset=utf-8");
				try {
					OutputStreamWriter out = new OutputStreamWriter(context
							.getOutputStream(), "UTF-8");
					engine.render("/dir.xhtml", data, out);
					out.close();
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

	private File getFile(String path) {
		try {
			return new File(URLDecoder.decode(path, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	public static void main(String[] args) throws MalformedURLException{
		new FileServer().start();
	}

}
