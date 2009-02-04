package org.xidea.lite.test.webserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandle {
	private static final Pattern QUERY_PATTERN = Pattern
			.compile("([^=&]+)(?:=([^&]+))?");

	private String encoding = "utf-8";
	private String url = "/";
	private String method;
	private OutputStream out;
	private AbstractWebServer server;
	private boolean headPrinted;
	private Map<String, String[]> headerMap;
	private Map<String, String[]> params = new HashMap<String, String[]>();

	public String getRequestURI() {
		return url;
	}

	public String getMethod() {
		return method;
	}

	public RequestHandle(AbstractWebServer server, InputStream in,
			OutputStream out) throws IOException {
		this.server = server;
		this.out = out;
		String getQuery = parseMethodLine(in);
		String postQuery = parseHeaders(in);
		if (getQuery != null) {
			parseParams(getQuery);
		}
		if (postQuery != null) {
			parseParams(postQuery);
		}
	}

	// public void execute() { }
	private String parseMethodLine(InputStream in) throws IOException {
		StringBuilder buf = new StringBuilder();
		int c;
		String method = null;
		String url = null;
		outer: while ((c = in.read()) > 0) {
			// System.out.print((char) c);
			switch (c) {
			case '\n':
				break outer;
			case ' ':
			case '\t':
				if (method == null) {
					method = buf.toString();
					buf.setLength(0);
				} else if (url == null) {
					url = buf.toString();
				}
				break;
			default:
				buf.append((char) c);
			}
		}
		String query = null;
		int pos = url.indexOf('?');
		if (pos >= 0) {
			query = url.substring(pos + 1);
			url = url.substring(0, pos);
		}
		this.url = url;
		this.method = method;
		return query;
	}

	private String parseHeaders(InputStream in) throws IOException {
		int c;
		String key = null;
		StringBuilder buf = new StringBuilder();
		HashMap<String, String[]> headerMap = new HashMap<String, String[]>();
		int contentLength = 0;
		try {
			outer: while ((c = in.read()) > 0) {
				switch (c) {
				case ':':
					key = buf.toString().trim();
					buf.setLength(0);
					break;
				case '\r':
					break;
				case '\n':
					if (key == null) {
						break outer;
					} else {
						String[] values = headerMap.get(key);
						String value = buf.toString().trim();
						if (key.equalsIgnoreCase("Content-Length")) {
							contentLength = Integer.parseInt(value);
						}
						if (values == null) {
							values = new String[] { value };
						} else {
							String[] values2 = new String[values.length + 1];
							System.arraycopy(values, 0, values2, 0,
									values2.length);
							values2[values.length] = value;
							values = values2;
						}
						headerMap.put(key, values);
						buf.setLength(0);
						key = null;
					}
					break;
				case ' ':
				case '\t':
					if (buf.length() == 0) {
						break;
					}
				default:
					buf.append((char) c);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.headerMap = headerMap;
		if (contentLength > 0) {
			return parsePost(in, contentLength);
		}
		return null;
	}

	protected String parsePost(InputStream in, int contentLength)
			throws IOException {
		StringBuffer buf = new StringBuffer();
		int b;
		while (contentLength > 0 && (b = in.read()) > -1) {
			contentLength--;
			buf.append((char) b);
		}
		return buf.toString();
	}

	private void parseParams(String query) throws UnsupportedEncodingException {
		Matcher matcher = QUERY_PATTERN.matcher(query);
		while (matcher.find()) {
			String name = matcher.group(1);
			String value = matcher.group(2);

			String[] values = params.get(name);
			if (values == null) {
				params.put(name, new String[] { decode(value) });
			} else {
				String[] values2 = new String[values.length + 1];
				System.arraycopy(values, 0, values2, 0, values.length);
				values2[values.length] = decode(value);
				params.put(name, values2);
			}
		}
	}

	private String decode(String value) throws UnsupportedEncodingException {
		return value == null ? null : URLDecoder.decode(value, encoding);
	}

	private final void writeln(Object message) throws IOException {
		if (message instanceof InputStream) {
			InputStream in = (InputStream) message;
			int len;
			byte[] buf = new byte[1024];
			while ((len = in.read(buf)) > -1) {
				out.write(buf, 0, len);
			}

		} else if (message instanceof byte[]) {
			out.write((byte[]) message);
		} else {
			out.write(String.valueOf(message).getBytes(encoding));
		}
		out.write('\r');
		out.write('\n');
	}

	public void printNotFound(String message) throws IOException {
		writeln("HTTP/1.1 404" + message);
		writeln("Server: Simple java");
		writeln("Date: " + (new Date()));
		headPrinted = true;
		writeln("");
		writeln(message);
	}

	public void printContext(Object content, String contentType)
			throws IOException {
		writeln("HTTP/1.1 200 OK");
		writeln("Server: Simple java");
		writeln("Date: " + (new Date()));
		String uri = this.getRequestURI();
		if (contentType == null) {
			uri = uri.substring(uri.lastIndexOf('/') + 1);
			int extIndex = uri.lastIndexOf('.');
			contentType = extIndex > 0 ? server.getContentType(uri
					.substring(extIndex + 1)) : "unknown/unknown";
		}
		writeln("Content-Type: " + contentType + ";charset=" + encoding);
		headPrinted = true;
		writeln("");
		writeln(content);
	}

	public void printFile(File file) throws IOException {
		if (headPrinted) {
			throw new IllegalStateException("head printed");
		}
		headPrinted = true;
		if (!file.exists()) {
			// rCode = HTTP_NOT_FOUND;
			printNotFound(file + " not found");
			return;
		} else {
			// rCode = HTTP_OK;
			writeln("HTTP/1.1 200 OK");
		}
		writeln("Server: Simple java");
		writeln("Date: " + (new Date()));
		if (file.isDirectory()) {
			writeln("Content-Type: text/html;charset=" + encoding);
			writeln("");
			File[] list = file.listFiles();
			writeln("<h2>");
			writeln(file.getAbsolutePath());
			writeln("</h2>");
			for (File sub : list) {
				String name = sub.isDirectory() ? sub.getName() + '/' : sub
						.getName();
				writeln("<a href='" + name + "'>" + name + "</a><br/>");
			}
		} else {
			writeln("Content-Length: " + file.length());
			writeln("Last Modified: " + (new Date(file.lastModified())));
			String name = file.getName();
			int extIndex = name.lastIndexOf('.');
			String contentType = extIndex > 0 ? server.getContentType(name
					.substring(extIndex + 1)) : "unknown/unknown";
			writeln("Content-Type: " + contentType + ";charset=" + encoding);
			writeln(new FileInputStream(file));
		}
	}

	public Map<String, String[]> getParameterMap() {
		return params;
	}

	public OutputStream getOutputStream() {
		return out;
	}

	public String getParameter(String name) {
		String[] values = params.get(name);
		return values != null?values[0]:null;
	}

	public void printRederect(String path) throws IOException {

		writeln("HTTP/1.1 200 ok");
		writeln("Refresh: 0; url="+path);
		headPrinted = true;
		writeln("");
		
		 

	}

}
