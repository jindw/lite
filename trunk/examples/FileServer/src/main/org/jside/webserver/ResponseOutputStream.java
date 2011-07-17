package org.jside.webserver;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ResponseOutputStream extends FilterOutputStream {
	private static final Log log = LogFactory.getLog(ResponseOutputStream.class);
	private static final String CHARSET = "charset=";
	static final String CONTENT_TYPE = "Content-Type";
	private String httpVersion = "HTTP/1.1";
	String status = "200 OK";
	ArrayList<String> headers = new ArrayList<String>();
	private RequestContext context;

	public ResponseOutputStream(RequestContext context,OutputStream out) {
		super(out);
		this.context = context;
		headers.add("Server:JSA Server");
		headers.add("Date:" + new Date());
		headers.add("Connection:close");
	}

	void setMimeType(String mimeType) {
		setHeader(CONTENT_TYPE + ':' + mimeType+";charset="+context.getEncoding());
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
					context.setEncoding(charset);
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
						h += ";charset=" + context.getEncoding();
					}
					length2flag = 0;
				}
				println(h);
			}
			if (length2flag > 0) {
				String uri = context.getRequestURI();
				String contentType = RequestUtil.getMimeType(uri) ;
				if (contentType.indexOf("text/") == 0 ) {
					contentType += ";charset=" + context.getEncoding();
				}
				println(CONTENT_TYPE +':'+contentType);
			}
			println("");
			headers = null;
		}
	}

	private void println(String msg) throws IOException {
		try{
			out.write(msg.getBytes(context.getEncoding()));
			out.write('\r');
			out.write('\n');
		}catch (java.net.SocketException e) {
			log.debug("web 数据流输出失败",e);
		}
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
		try{
			out.write(b, off, len);
		}catch (java.net.SocketException e) {
			log.debug("web 数据流输出失败",e);
		}
	}

	public void write(int b) throws IOException {
		this.beforeWrite();
		try{
			out.write(b);
		}catch (java.net.SocketException e) {
			log.debug("web 数据流输出失败",e);
		}
	}
}
