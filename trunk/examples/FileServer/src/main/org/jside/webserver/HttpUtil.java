package org.jside.webserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


@SuppressWarnings("unchecked")
public class HttpUtil {
	public static final String PLAIN_TEXT = "text/plain";
	private static Map<String, String> contentTypeMap = new HashMap<String, String>();

	static {
		try {
			Properties props = new Properties();
			InputStream in = HttpUtil.class.getResourceAsStream("mime.types");
			if (in != null) {
				props.load(in);
			}
			in = HttpUtil.class.getResourceAsStream("/mime.types");
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

	private final static void print(RequestContext context, Object newParam)
			throws IOException {
		OutputStream out = context.getOutputStream();
		if (newParam instanceof InputStream) {
			InputStream in = (InputStream) newParam;
			int len;
			byte[] buf = new byte[1024];
			while ((len = in.read(buf)) > -1) {
				out.write(buf, 0, len);
			}

		} else if (newParam instanceof byte[]) {
			out.write((byte[]) newParam);
		} else if (newParam instanceof Throwable) {
			PrintWriter pout = new PrintWriter(out);
			((Throwable) newParam).printStackTrace(pout);
			pout.flush();
		} else {
			String encoding = context.getEncoding();
			out.write(String.valueOf(newParam).getBytes(
					encoding == null ? "UTF-8" : encoding));
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
				File file = new File(URLDecoder.decode(resource.getFile(),
						"UTF-8"));
				printFile(file, contentType);
			} else {
				InputStream in = resource.openStream();
				print(RequestContext.get(), in);
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
		print(context, message);
	}

	private static void printData(Object data, String contentType)
			throws IOException {
		RequestContext context = RequestContext.get();
		if (data instanceof Throwable) {
			try {
				context.setContentType(contentType);
			} catch (Exception e) {
			}
		}
		print(context, data);
	}

	private static void printFile(File file, String contentType)
			throws IOException, FileNotFoundException {

		RequestContext context = RequestContext.get();
		if (!file.exists()) {
			// rCode = HTTP_NOT_FOUND;
			printNotFound(file + " not found");
			return;
		}
		// rCode = HTTP_OK;
		if (file.isDirectory()) {
			context.setContentType("text/html");
			File[] list = file.listFiles();
			print(context, "<h2>");
			print(context, file.getAbsolutePath());
			print(context, "</h2>");
			for (File sub : list) {
				String name = sub.isDirectory() ? sub.getName() + '/' : sub
						.getName();
				print(context, "<a href='" + name + "'>" + name + "</a><br/>");
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
					print(context, in);

				} finally {
					in.close();
				}
			}
			
		}
	}
}