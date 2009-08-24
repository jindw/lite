package org.jside.webserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RequestContextImpl extends RequestContext {
	private static final Log log = LogFactory.getLog(RequestContextImpl.class);
	private static final String CONTENT_LENGTH = "Content-Length";
	private static final String ISO_8859_1 = "ISO-8859-1";

	private static final Pattern QUERY_PATTERN = Pattern
			.compile("([^=&]+)(?:=([^&]+))?");
	private ArrayList<Object> valueStack = new ArrayList<Object>();

	private String encoding = null;
	private String requestURI = "/";
	private ArrayList<String> headers = new ArrayList<String>();
	private Map<String, String[]> paramsMap;
	private Map<String, String> paramMap;
	private BufferedReader cin;
	private ResponseOutputStream out;
	private WebServer server;
	private String requestLine;
	private String method;
	private String query;
	private URL base;
	public BufferedReader getInput(){
		return cin;
	}

	RequestContextImpl(WebServer server, InputStream sin, OutputStream out) {
		this.push(server);
		this.push(this);
		base = server.getWebBase();
		try {
			this.cin = new BufferedReader(
					new InputStreamReader(sin, ISO_8859_1));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		this.out = new ResponseOutputStream(out);
		this.server = server;
		try {
			requestLine = cin.readLine();
			String[] rls = requestLine.split("[\\s]");
			method = rls[0];
			requestURI = rls[1];
			if(log.isDebugEnabled()){
				log.debug("process:"+requestURI);
			}
			int p = requestURI.indexOf('?');
			if (p > 0) {
				query = requestURI.substring(p + 1);
				requestURI = requestURI.substring(0, p);
			}
			parseHeaders(cin);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Object> getApplication() {
		return server.getApplication();
	}

	public String getRequestURI() {
		return requestURI;
	}

	@Override
	public String getQuery() {
		return query;
	}

	public String getMethod() {
		return method;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public Map<String, String[]> getParams() {
		if (paramsMap == null) {
			paramsMap = new LinkedHashMap<String, String[]>();
			parseParams(query);
			String contentLength = findHeader(CONTENT_LENGTH);
			if (contentLength != null) {
				try {
					String post = getPost(cin, Integer.parseInt(contentLength));
					parseParams(post);
				} catch (Exception e) {
					log.warn(e);
				}
			}
		}
		return paramsMap;
	}

	public Map<String, String> getParam() {
		if (paramMap == null) {
			paramMap = new StringMap(getParams());
		}
		return paramMap;
	}

	public String findHeader(String key) {
		return findHeader(headers, key);
	}

	static String findHeader(List<String> headers, String key) {
		for (String h : headers) {
			int p = h.indexOf(':');
			if (key.equalsIgnoreCase(h.substring(0, p))) {
				return h.substring(p + 1).trim();
			}
		}
		return null;
	}

	public OutputStream getOutputStream() {
		return out;
	}

	public String getParameter(String name) {
		String[] values = getParams().get(name);
		return values != null ? values[0] : null;
	}

	public void dispatch(String path) {
		String preuri = this.requestURI;
		this.requestURI = path;
		try {
			server.processRequest(this);
		} catch (Exception e) {
			log.warn("重定向处理失败...", e);
		} finally {
			this.requestURI = preuri;
			try {
				this.getOutputStream().flush();
			} catch (IOException e) {
			}
		}

	}

	private String parseHeaders(BufferedReader in) {
		try {
			while (true) {
				String line = in.readLine();
				if (line == null) {
					break;
				} else if (line.length() == 0) {
					break;
				} else {
					this.headers.add(line);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	private void addStrings(Map<String, String[]> map, String key, String value) {
		String[] values = map.get(key);
		if (values == null) {
			values = new String[] { value };
		} else {
			String[] values2 = new String[values.length + 1];
			System.arraycopy(values, 0, values2, 0, values.length);
			values2[values.length] = value;
			values = values2;
		}
		map.put(key, values);
	}

	private String getPost(Reader in, int contentLength) throws IOException {
		StringBuffer buf = new StringBuffer();
		int b;
		while (contentLength > 0 && (b = in.read()) > -1) {
			contentLength--;
			buf.append((char) b);
		}
		return buf.toString();
	}

	private void parseParams(String query) {
		if (query != null) {
			Matcher matcher = QUERY_PATTERN.matcher(query);
			while (matcher.find()) {
				String name = matcher.group(1);
				String value = matcher.group(2);
				try {
					String encoding = this.getEncoding();
					if (encoding == null) {
						encoding = "UTF-8";
					}
					addStrings(paramsMap, URLDecoder.decode(name, encoding),
							value == null?"":URLDecoder.decode(value, encoding));
				} catch (Exception e) {
					log.info("解析失败: "+query+"\n"+name+"="+value,e);
				}

			}
		}
	}

	@Override
	public URL getResource(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		try {
			URL url = new URL(base , path);
			if(!url.getProtocol().equals("file") || new File(URLDecoder.decode(url.getFile(),"UTF-8")).exists()){
				return url;
			}
			String host = findHeader("Host");
			int p = host.indexOf(':');
			if(p>0){
				host = host.substring(0,p);
			}
			String jsideHome = System.getProperty("jside.home");
			if(jsideHome != null){
				File hostFile = new File(new File(jsideHome,host),path);
				if(hostFile.exists()){
					return hostFile.toURI().toURL();
				}
			}
			String name = toResourceRoot(host);
			return server.getClass().getClassLoader().getResource(name);
		} catch (IOException e) {
			log.warn(e);
			return null;
		}
	}
	private String toResourceRoot(String host) {
		String[] data = host.split("[\\.]");
		StringBuilder buf = new StringBuilder();
		int p = data.length;
		while (p-- > 0) {
			buf.append(data[p]);
			buf.append('/');
		}
		buf.append("web/");
		String name = buf.toString();
		return name;
	}

	@Override
	public Object[] getValueStack() {
		return valueStack.toArray();
	}

	@Override
	public Object pop() {
		return valueStack.remove(valueStack.size() - 1);
	}

	@Override
	public void push(Object value) {
		this.valueStack.add(value);
	}

	@Override
	public void addHeader(String value) {
		out.addHeader(value);
	}

	@Override
	public void setHeader(String value) {
		out.setHeader(value);
	}

	@Override
	public void setStatus(int status, String message) {
		if(message!=null){
			try {
				message = URLEncoder.encode(message,"UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
		}
		out.status = status + " " + message;
	}

	@Override
	public void setContentType(String contentType) {
		out.setContentType(contentType);
	}

	@Override
	public boolean isAccept() {
		return out.headers == null;
	}

}

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

//	OutputStream getBase() {
//		return out;
//	}

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
			contentType = extIndex > 0 ? HttpUtil.getContentType(uri
					.substring(extIndex + 1)) : "text/html";
			setContentType(contentType);
		}
		String encoding = context.getEncoding();
		if (contentType.indexOf("text/") >= 0 && encoding != null) {
			if (contentType.indexOf("charset=") < 0) {
				contentType += ";charset=" + encoding;
				setContentType(contentType);
			}
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
		out.write(b, off, len);
	}

	public void write(int b) throws IOException {
		this.beforeWrite();
		out.write(b);
	}
}