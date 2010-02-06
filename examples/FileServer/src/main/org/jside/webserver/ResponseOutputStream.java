package org.jside.webserver;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

class ResponseOutputStream extends FilterOutputStream {
	static final String CONTENT_TYPE = "Content-Type";
	private String httpVersion = "HTTP/1.1";
	String status = "200 OK";
	ArrayList<String> headers = new ArrayList<String>();

	public ResponseOutputStream(OutputStream out) {
		super(out);
		headers.add("Server:JSA Server");
		headers.add("Date:" + new Date());
		headers.add("Connection:close");
	}

	void setContentType(String contentType) {
		setHeader(CONTENT_TYPE + ':' + contentType);
	}

	public void addHeader(String value) {
		int length = value.indexOf(':');
		String name = value.substring(0, length);
		if ("Server".equals(name) || "Date".equals(name)) {
			setHeader(value);
		} else if (CONTENT_TYPE.equalsIgnoreCase(name)) {
			setHeader(value);
		} else {
			headers.add(value);
		}
	}

	public void setHeader(String value) {
		int length = value.indexOf(':');
		for (int i = headers.size() - 1; i >= 0; i--) {
			String key = headers.get(i);
			if (key.regionMatches(true, 0, value, 0, length)) {
				headers.set(i, value);
				return;
			}
		}
		headers.add(value);
	}

	private void printHeader(String msg) throws IOException {
		out.write(msg.getBytes());
		out.write('\r');
		out.write('\n');
	}

	private synchronized void beforeWrite() throws IOException {
		if (headers != null) {
			printHeader(httpVersion + ' ' + status);
			initializeContentType();
			//System.out.println(RequestContext.get().getRequestURI());
			for (String h : headers) {
				printHeader(h);
			}
			printHeader("");
			headers = null;
		}
	}

	private void initializeContentType() {
		RequestContext context = RequestContext.get();
		String uri = context.getRequestURI();
		String contentType = RequestContextImpl.findHeader(headers,
				CONTENT_TYPE);
		if (contentType == null) {
			uri = uri.substring(uri.lastIndexOf('/') + 1);
			int extIndex = uri.lastIndexOf('.');
			contentType = extIndex > 0 ? RequestUtil.getContentType(uri
					.substring(extIndex + 1)) : "text/html";
			String encoding = context.getEncoding();
			setContentType(contentType+";charset="+encoding);
		}
//		if (contentType.indexOf("text/") >= 0 && encoding != null) {
//			if (contentType.indexOf("charset=") < 0) {
//				contentType += ";charset=" + encoding;
//				setContentType(contentType);
//			}
//		}
	}

	public void flush() throws IOException {
		this.beforeWrite();
		super.flush();
	}

	public void close() throws IOException {
		this.flush();
		super.close();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		this.beforeWrite();
		out.write(b, off, len);
	}

	public void write(int b) throws IOException {
		this.beforeWrite();
		out.write(b);
	}
}
