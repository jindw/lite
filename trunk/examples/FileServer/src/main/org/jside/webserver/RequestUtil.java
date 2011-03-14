package org.jside.webserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class RequestUtil {
	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
	public static final String PLAIN_TEXT = "text/plain";
	private static ThreadLocal<RequestContextImpl> holder = new ThreadLocal<RequestContextImpl>();

	public static RequestContext enter(WebServer server, Socket remote)
			throws IOException {
		RequestContextImpl context = new RequestContextImpl(server, remote);
		holder.set(context);
		return context;
	}

	public static void exit() throws IOException {
		RequestContextImpl context = holder.get();
		if (context != null) {
			holder.remove();
			context.close();
		}
	}

	public static RequestContext get() {
		return holder.get();
	}

	private static Map<String, String> contentTypeMap = new HashMap<String, String>();

	static {
		try {
			Properties props = new Properties();
			InputStream in = RequestUtil.class
					.getResourceAsStream("mime.types");
			if (in != null) {
				props.load(in);
				in.close();
			}
			in = RequestUtil.class.getResourceAsStream("/mime.types");
			if (in != null) {
				props.load(in);
				in.close();
			}
			
			for (Map.Entry<?,?> entry : props.entrySet()) {
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

	public static String getMimeType(String name) {
		int extIndex = name.lastIndexOf('.');
		if (extIndex >= 0) {
			name = name.substring(extIndex + 1).toLowerCase();
		}
		String contentType = (String) contentTypeMap.get(name);
		if (contentType == null) {
			if (name.endsWith("/")) {
				contentType = "text/html";
			} else {
				contentType = DEFAULT_CONTENT_TYPE;
			}
		}
		return contentType;
	}

	public static void printResource() throws IOException {
		RequestContext context = get();
		String uri = context.getRequestURI();
		URI res = context.getResource(uri);
		File file = getFile(res);
		if (file != null && file.isDirectory() && !uri.endsWith("/")) {
			sendRedirect(uri.replaceFirst("[\\\\]?$", "/"));
		} else {
			printResource(res, null);
		}
	}

	public static void printResource(Object data, String contentType)
			throws IOException {
		File f = null;
		if (data instanceof URI) {
			f = getFile((URI) data);
			if (f == null) {
				data = ((URI) data).toURL();
			}
		} else if (data instanceof URL) {
			try {
				f = getFile(((URL) data).toURI());
			} catch (URISyntaxException e) {
			}
		} else if (data instanceof File) {
			f = (File) data;
		}
		if (f != null) {
			printFile(f, contentType);
			return;
		}
		if (data instanceof URL) {
			URL resource = (URL) data;
			writeContentType(contentType, resource.getFile());
			write(resource, get().getOutputStream());
		} else {
			writeContentType(contentType, null);
			write(data, get().getOutputStream());
		}
	}

	private static void printFile(File file, String contentType)
			throws IOException, FileNotFoundException {
		if (contentType == null) {
			contentType = getMimeType(file.getPath());
		}
		RequestContext context = get();
		String html = null;
		if (file == null || !file.exists()) {
			// rCode = HTTP_NOT_FOUND;
			String message = file + " not found";
			context.setStatus(404, message);
			html = message;// , context.getEncoding(),
							// context.getOutputStream());
		}
		// rCode = HTTP_OK;
		if (file.isDirectory()) {
			File[] list = file.listFiles();
			StringBuilder buf = new StringBuilder();
			buf.append("<h2>");
			buf.append(file.getAbsolutePath());
			buf.append("</h2>");
			for (File sub : list) {
				String name = sub.isDirectory() ? sub.getName() + '/' : sub
						.getName();
				buf.append("<a href='" + name + "'>" + name + "</a><br/>");
			}
			html = buf.toString();
		}
		if (html != null) {
			write(html.getBytes(context.getEncoding()), context
					.getOutputStream());
		} else {
			context.setMimeType(contentType);
			String fileModified = new Date(file.lastModified()).toString();
			String headModified = context.getRequestHeader("If-Modified-Since");
			if (fileModified.equals(headModified)) {
				context.setStatus(304, "Not Modified");
				context.getOutputStream().flush();
			} else {
				context.setResponseHeader("Content-Length: " + file.length());
				context.setResponseHeader("Last-Modified: " + fileModified);
				writeContentType(contentType, context.getRequestURI());
				write(file, context.getOutputStream());
			}
		}
	}

	private final static void writeContentType(String contentType, String path) {
		RequestContext context = get();
		if (contentType == null) {
			if (contentType == null) {
				if (path == null) {
					path = context.getRequestURI();
				}
				contentType = getMimeType(path);
			}
		} else if (contentType.indexOf('/') < 0) {
			contentType = getMimeType("." + contentType);
		}
		if (contentType.indexOf("text/") == 0
				&& contentType.indexOf("charset=") < 0) {
			contentType += ";charset=" + context.getEncoding();
		}
		context.setMimeType(contentType);
	}

	/**
	 * File,InputStream,byte[],Throwable,Object.toString()
	 * 
	 * @param data
	 * @param out
	 * @throws IOException
	 */
	private final static void write(Object data, OutputStream out)
			throws IOException {
		if (data instanceof File) {
			data = new FileInputStream((File) data);
		} else if (data instanceof URL) {
			data = ((URL) data).openStream();
		}
		if (data instanceof InputStream) {
			InputStream in = (InputStream) data;
			try {
				int len;
				byte[] buf = new byte[1024];
				while ((len = in.read(buf)) > -1) {
					out.write(buf, 0, len);
				}
			} finally {
				in.close();
			}

		} else if (data instanceof byte[]) {
			out.write((byte[]) data);
		} else if (data instanceof Throwable) {
			PrintWriter pout = new PrintWriter(out);
			((Throwable) data).printStackTrace(pout);
			pout.flush();
		} else {
			String encoding = get().getEncoding();
			out.write(String.valueOf(data).getBytes(encoding));
		}
//		out.write('\r');
//		out.write('\n');
	}

	private static File getFile(URI root) {
		if (root != null && "file".equals(root.getScheme())) {
			return new File(root.getPath());
		}
		return null;
	}

	public static void sendRedirect(String href) {
		RequestContext context = get();
		context.setResponseHeader("Refresh:0;URL=" + href);
		context.setResponseHeader("Content-Type:text/html;charset="
				+ context.getEncoding());

	}

}