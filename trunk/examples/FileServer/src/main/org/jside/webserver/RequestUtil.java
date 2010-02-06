package org.jside.webserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


@SuppressWarnings("unchecked")
public abstract class RequestUtil {
	public static final String PLAIN_TEXT = "text/plain";
	private static Map<String, String> contentTypeMap = new HashMap<String, String>();

	static {
		try {
			Properties props = new Properties();
			InputStream in = RequestUtil.class.getResourceAsStream("mime.types");
			if (in != null) {
				props.load(in);
			}
			in = RequestUtil.class.getResourceAsStream("/mime.types");
			if (in != null) {
				props.load(in);
			}
			for (Map.Entry entry : props.entrySet()) {
				String contentType = (String) entry.getKey();
				String exts = (String) entry.getValue();
				for (String ext : exts.trim().split("[,\\s]+")) {
					contentTypeMap.put(ext, contentType);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getContentType(String name) {
		int extIndex = name.lastIndexOf('.');
		if (extIndex >= 0) {
			name = name.substring(extIndex + 1).toLowerCase();
		}
		String contentType = (String) contentTypeMap.get(name);
		return contentType == null ? "application/octet-stream" : contentType;
	}

	private final static void write(RequestContext context, Object data)
			throws IOException {
		OutputStream out = context.getOutputStream();
		if (data instanceof File) {
			data = new FileInputStream((File)data);
		}
		if (data instanceof InputStream) {
			InputStream in = (InputStream) data;
			int len;
			byte[] buf = new byte[1024];
			while ((len = in.read(buf)) > -1) {
				out.write(buf, 0, len);
			}

		} else if (data instanceof byte[]) {
			out.write((byte[]) data);
		} else if (data instanceof Throwable) {
			PrintWriter pout = new PrintWriter(out);
			((Throwable) data).printStackTrace(pout);
			pout.flush();
		} else {
			String encoding = context.getEncoding();
			out.write(String.valueOf(data).getBytes(
					encoding));
		}
		out.write('\r');
		out.write('\n');
	}

	public static void printResource(Object data) throws IOException {
		String contentType = RequestContextImpl.findHeader(RequestContext.get()
				.getHeaders(), "Content-Type");
		printResource(data, contentType);
	}

	public static void printResource(Object data, String contentType)
			throws IOException {
		if (data instanceof URL) {
			URL resource = (URL) data;
			if (contentType == null) {
				contentType = getContentType(resource.getFile());
			}
			if ("file".equals(resource.getProtocol())) {
				File file = getFile(resource);
				printFile(file, contentType);
			} else {
				InputStream in = resource.openStream();
				write(RequestContext.get(), in);
				in.close();
			}
		} else {
			if (contentType == null) {
				contentType = getContentType(RequestContext.get()
						.getRequestURI());
			} else if (contentType.indexOf('/') < 0) {
				contentType = getContentType("." + contentType);
			}
			printData(data, contentType);
		}
	}

	private static void printNotFound(String message) throws IOException {
		RequestContext context = RequestContext.get();
		context.setStatus(404, message);
		write(context, message);
	}

	private static void printData(Object data, String contentType)
			throws IOException {
		RequestContext context = RequestContext.get();
		context.setContentType(contentType);
		write(context, data);
	}

	private static void printFile(File file, String contentType)
			throws IOException, FileNotFoundException {
		RequestContext context = RequestContext.get();
		if (file == null || !file.exists()) {
			// rCode = HTTP_NOT_FOUND;
			printNotFound(file + " not found");
			return;
		}
		// rCode = HTTP_OK;
		if (file.isDirectory()) {
			context.setContentType("text/html");
			File[] list = file.listFiles();
			write(context, "<h2>");
			write(context, file.getAbsolutePath());
			write(context, "</h2>");
			for (File sub : list) {
				String name = sub.isDirectory() ? sub.getName() + '/' : sub
						.getName();
				write(context, "<a href='" + name + "'>" + name + "</a><br/>");
			}
		} else {
			context.setContentType(contentType);
			String fileModified = new Date(file.lastModified()).toString();
			String headModified = context.findHeader("If-Modified-Since");
			if (fileModified.equals(headModified)) {
				context.setStatus(304, "Not Modified");
				context.getOutputStream().flush();
			} else {
				context.addHeader("Content-Length: " + file.length());
				context.addHeader("Last-Modified: " + fileModified);

				FileInputStream in = new FileInputStream(file);
				try {
					write(context, in);

				} finally {
					in.close();
				}
			}
			
		}
	}
	public static File getFile(URL root) {
		try {
			return getFile(root.toURI());
		} catch (Exception e) {
			return null;
		}
	}
	public static File getFile(URI root) {
		if (root != null && "file".equals(root.getScheme())) {
			return new File(root.getPath());
		}
		return null;
	}
	public static void sendRedirect(String href) {
		RequestContext context = RequestContext.get();
		context.setHeader("Refresh:0;URL=" + href);
		context.addHeader("Content-Type:text/html;charset="
					+ context.getEncoding());
		
	}

}