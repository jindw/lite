package org.jside.webserver.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.jside.webserver.WebServer;


abstract class ResponseAdaptor extends RequestAdaptor implements HttpServletResponse{

	public ResponseAdaptor(WebServer server) {
		super(server);
	}

	public void addCookie(Cookie arg0) {
	}

	public void addDateHeader(String arg0, long arg1) {
		addHeader(arg0, new Date(arg1).toString());
	}

	public void addHeader(String key, String value) {
		base().addResponseHeader(key+":"+value);
	}

	public void addIntHeader(String key, int value) {
		addHeader(key,""+value);
		
	}

	public boolean containsHeader(String key) {
		return getHeader(key) != null;
	}

	public String encodeRedirectURL(String arg0) {
		return null;
	}

	public String encodeRedirectUrl(String arg0) {
		return null;
	}

	public String encodeURL(String arg0) {
		return null;
	}

	public String encodeUrl(String arg0) {
		return null;
	}

	public void sendError(int arg0) throws IOException {
		
	}

	public void sendError(int arg0, String arg1) throws IOException {
		
	}

	public void sendRedirect(String arg0) throws IOException {
		
	}

	public void setDateHeader(String key, long value) {
		setHeader(key,new Date( value).toString());
		
	}

	public void setHeader(String key, String value) {
		base().setResponseHeader(key+":"+value);
		
	}

	public void setIntHeader(String key, int value) {
		setHeader(key,""+value);
		
	}

	public void setStatus(int arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setStatus(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	public void flushBuffer() throws IOException {
		base().getOutputStream().flush();
	}

	public int getBufferSize() {
		return 0;
	}

	public Locale getLocale() {
		return null;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		return new ServletOutputStream(){
			@Override
			public void write(int b) throws IOException {
				base().getOutputStream().write(b);
			}
			public void write(byte b[], int off, int len) throws IOException {
				base().getOutputStream().write(b,off,len);
			}
			
		};
	}

	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(new OutputStreamWriter(base().getOutputStream(),"UTF-8"),true);
	}

	public boolean isCommitted() {
		return base().isAccept();
	}

	public void reset() {
		
	}

	public void resetBuffer() {
		// TODO Auto-generated method stub
		
	}

	public void setBufferSize(int arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setCharacterEncoding(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setContentLength(int arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setContentType(String arg0) {
		base().setMimeType(arg0);
	}

	public void setLocale(Locale arg0) {
		// TODO Auto-generated method stub
		
	}


}
