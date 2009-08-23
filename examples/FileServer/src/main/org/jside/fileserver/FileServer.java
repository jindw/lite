package org.jside.fileserver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;

import org.jside.webserver.RequestContext;
import org.jside.webserver.action.ActionWebServer;
import org.jside.webserver.action.TemplateAction;

public class FileServer extends ActionWebServer {

	private TemplateAction engine = TemplateAction.getInstance();

	public FileServer() throws MalformedURLException {
		super(new URL("file:///"));
		// this.addAction("/filemanager/delete.action", this);

	}

	@Override
	public void processRequest(RequestContext context) throws IOException {
		URL res = new URL(webBase, context.getRequestURI().substring(1));
		if (res.getProtocol().equals("file")) {
			String path = res.getFile();
			String pathInfo = null;
			int p = path.lastIndexOf(';');
			if (p > 0) {
				pathInfo = path.substring(p + 1);
				path = path.substring(0, p);
			}
			File file = getFile(path);
			if (context.getMethod().equals("POST")) {
				File dest = file;
				boolean success = false;
				if ("delete".equals(pathInfo)) {
					success = dest.delete();
					file = file.getParentFile();
				} else if ("move".equals(pathInfo)) {
					dest = new File(context.getParam().get("path"), file
							.getName());
					success = file.renameTo(dest);
					if(success){
						file = dest.getParentFile();
					}else{
						file = file.getParentFile();
					}
					
				} else if ("rename".equals(pathInfo)) {
					dest = new File(file.getParentFile(), context.getParam()
							.get("name"));
					success = file.renameTo(dest);
					file = dest.getParentFile();
				} else if ("mkdir".equals(pathInfo)) {
					dest = new File(file, context.getParam()
							.get("name"));
					success = dest.mkdir();
					if(success){
						file = dest;
					}
				}
				String href = file.toURI().toURL().getFile();
				href = URLEncoder.encode(href,"UTF-8").replace("%2F", "/");
				context.setHeader("Refresh:1;URL="+href);
				context.addHeader("Content-Type:text/html;charset=utf-8");
				OutputStream out = context.getOutputStream();
				StringBuilder buf = new StringBuilder("<a href='");
				buf.append(href).append("'>");
				if(success){
					buf.append("操作成功");
				}else{
					buf.append("操作失败");
				}
				buf.append(",马上回来...</a>");
				out.write(buf.toString().getBytes("UTF-8"));
				// TODO.redirect
			} else {
				//long t1 = System.currentTimeMillis(),t2=t1,t3=t1;
				if (file.isDirectory()) {
					File[] list = file.listFiles();
					HashMap<String, Object> data = new HashMap<String, Object>();
					data.put("path", file.getCanonicalFile().getAbsolutePath()
							.replace('\\', '/'));
					data.put("fileList", list);
					context.setContentType("text/html;charset=utf-8");

					try {
						OutputStreamWriter out = new OutputStreamWriter(context
								.getOutputStream(), "UTF-8");
						engine.render("/dir.xhtml", data, out);
						//t3 = System.currentTimeMillis();
						//out.write((t2-t1)+"/"+(t3-t2));
						out.close();
						
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}
				} else {
					super.processRequest(context);
				}
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

	public static void main(String[] args) throws MalformedURLException {
		new FileServer().start();
	}

}
