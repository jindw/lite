package org.jside.webserver;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

class ResponseOutputStream extends FilterOutputStream {
	private static final String CHARSET = "charset=";
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

	void setMimeType(String contentType) {
		setHeader(CONTENT_TYPE + ':' + contentType);
	}

	public void addHeader(String value) {
		int length = value.indexOf(':');
		String name = value.substring(0, length);
		if (CONTENT_TYPE.equalsIgnoreCase(name)) {
			setHeader(value);
		} else {
			headers.add(value);
		}
	}

	public void setHeader(String value) {
		int length = value.indexOf(':')+1;
		if (CONTENT_TYPE.regionMatches(true, 0, value, 0, length)) {
			int p = value.indexOf(CHARSET);
			if(p>0){
				int pd = value.lastIndexOf(';',p);
				if(pd > 0){
					String charset = value.substring(p+CHARSET.length());
					RequestUtil.get().setEncoding(charset);
					value = value.substring(0,pd);
				}else{
					//error
				}
			}
		}
		for (int i = headers.size() - 1; i >= 0; i--) {
			String key = headers.get(i);
			if (key.regionMatches(true, 0, value, 0, length)) {
				headers.set(i, value);
				return;
			}
		}
		headers.add(value);
	}

	private synchronized void beforeWrite() throws IOException {
		if (headers != null) {
			println(httpVersion + ' ' + status);
			int length2flag = CONTENT_TYPE.length();
			//System.out.println(RequestContext.get().getRequestURI());
			for (String h : headers) {
				if (length2flag > 0 && h.regionMatches(true, 0, CONTENT_TYPE, 0, CONTENT_TYPE.length())) {
					if (h.indexOf("text/") == 0 ) {
						h += ";charset=" + RequestUtil.get().getEncoding();
					}
					length2flag = 0;
				}
				println(h);
			}
			if (length2flag > 0) {
				String uri = RequestUtil.get().getRequestURI();
				String contentType = RequestUtil.getMimeType(uri) ;
				if (contentType.indexOf("text/") == 0 ) {
					contentType += ";charset=" + RequestUtil.get().getEncoding();
				}
				println(CONTENT_TYPE +':'+contentType);
			}
			println("");
			headers = null;
		}
	}

	private void println(String msg) throws IOException {
		out.write(msg.getBytes());
		out.write('\r');
		out.write('\n');
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
