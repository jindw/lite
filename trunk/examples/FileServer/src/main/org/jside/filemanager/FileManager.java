package org.jside.filemanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;

import org.jside.webserver.CGIAdaptor;
import org.jside.webserver.RequestUtil;
import org.jside.webserver.RequestContext;
import org.jside.webserver.action.ActionWebServer;
import org.jside.webserver.action.TemplateAction;

public class FileManager {
	private String contextPath;
	private File base;
	private TemplateAction engine;

	public FileManager(File base, String contextPath) {
		if(contextPath.endsWith("/")){
			contextPath = contextPath.substring(0,contextPath.length()-1);
		}
		this.contextPath = contextPath;
		this.base = base;
		engine = new TemplateAction(URI.create("classpath:///org/jside/filemanager/"));
	}

	public boolean execute() throws IOException {
		RequestContext context = RequestUtil.get();
		String uri = context.getRequestURI();
		if (uri.startsWith(contextPath)) {
			String path = uri.substring(contextPath.length());
			String action = context.getParam().get("action");
			path = decodePath(path);
			if (context.getMethod().equals("POST")) {
				processFileAction(context, path, action);
			} else {
				processFileView(context, path);
			}
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param context
	 * @param path  /
	 *              /dir1/
	 *              /file
	 *              /dir1/file.txt
	 * @throws IOException
	 */
	private void processFileView(RequestContext context, String path)
			throws IOException {
		// long t1 = System.currentTimeMillis(),t2=t1,t3=t1;
		final File file = new File(base,path);
		if (file.isDirectory()) {
			if (path.length() == 0 || path.endsWith("/")) {
				File[] list = file.listFiles();
				HashMap<String, Object> data = new HashMap<String, Object>();
				data.put("path", path.replace('\\', '/'));
				data.put("fileList", list);
				data.put("prefix",this.contextPath);
				context.setMimeType("text/html");
				try {
					OutputStreamWriter out = new OutputStreamWriter(context
							.getOutputStream(), context.getEncoding());
					engine.render("/dir.xhtml", data, out);
					// t3 = System.currentTimeMillis();
					// out.write((t2-t1)+"/"+(t3-t2));
					out.close();

				} catch (Throwable e) {
					throw new RuntimeException(e);
				}

			} else {
				RequestUtil.sendRedirect(contextPath + path + "/");
			}
		} else {
			RequestUtil.printResource(file,null);
		}
	}

	protected void processFileAction(RequestContext context, final String path,
			final String action) throws MalformedURLException,
			UnsupportedEncodingException, IOException {
		final File file = new File(base, path);
		String href = "./";
		boolean success = false;
		if ("text".equals(action)) {
			String fileName = context.getParam().get("name");
			File newFile = new File(file,fileName);
			success =newFile.createNewFile();
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(newFile),"UTF-8");
			String content = context.getParam().get("content");
			out.write(content);
			out.flush();
			out.close();
			href = "./";
		}else if ("delete".equals(action)) {
			success = file.delete();
			href = "./";
		} else if ("move".equals(action)) {
			String dest = context.getParam().get("path");
			success = file.renameTo(getFile(getFile(dest), file.getName()));
			if (success) {
				href = contextPath + dest.substring(1);
			} else {
				href = "../";
			}

		} else if ("rename".equals(action)) {
			File parent = file.getParentFile();
			String name = context.getParam().get("name");
			File dest = getFile(parent, name);
			success = file.renameTo(dest);
			href = "./";
		} else if ("mkdir".equals(action)) {
			String name = context.getParam().get("name");
			File dest = getFile(file, name);
			success = dest.mkdir();
			if (success) {
				href = name + "/";
			}
		}
		href = URLEncoder.encode(href, "UTF-8").replace("%2F", "/");
		//RequestUtil.sendRedirect(href);
		context.setEncoding("utf-8");
		context.setMimeType("text/html");
		OutputStream out = context.getOutputStream();
		StringBuilder buf = new StringBuilder("<a href='");
		buf.append(href).append("'>");
		if (success) {
			buf.append("操作成功");
		} else {
			buf.append("操作失败");
		}
		buf.append(",马上回来...</a>");
		out.write(buf.toString().getBytes(context.getEncoding()));
	}

	private File getFile(String path) {
		return getFile(base, path);
	}

	private File getFile(File base, String path) {
		return new File(base, decodePath(path));

	}

	private String decodePath(String path) {
		try {
			return URLDecoder.decode(path, "UTF-8").replace('+', ' ');
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public static void main(String[] args) throws MalformedURLException {
		String file = "./";
		if(args != null && args.length>0){
			file = args[0];
		}
		File root = new File(file).getAbsoluteFile();
		System.out.println(root);
		ActionWebServer aws = new ActionWebServer(root.toURI());
		
		aws.addAction("/cgi/**", new CGIAdaptor(aws));
		aws.addAction("/fs/**", new FileManager(root, "/fs"));
		aws.addAction("/**",new Object(){
			@SuppressWarnings("unused")
			public void execute(){
				RequestUtil.sendRedirect("/fs/");
			}
		});
		aws.start();
	}

}
