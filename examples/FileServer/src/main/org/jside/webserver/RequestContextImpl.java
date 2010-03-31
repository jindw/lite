package org.jside.webserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
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

	private String encoding;
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
	public BufferedReader getInput(){
		return cin;
	}

	RequestContextImpl(WebServer server, InputStream sin, OutputStream out) {
		this.push(server);
		this.push(this);
		this.encoding = server.getEncoding();
		this.server = server;
//		URI base = server.getWebBase();
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


	public String getMethod() {
		return method;
	}

	public String getEncoding() {
		return encoding;
	}
	public String getQuery() {
		return query;
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
					addStrings(paramsMap, URLDecoder.decode(name, encoding),
							value == null?"":URLDecoder.decode(value, encoding));
				} catch (Exception e) {
					log.info("解析失败: "+query+"\n"+name+"="+value,e);
				}

			}
		}
	}

	@Override
	public URI getResource(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return server.getWebBase().resolve(path);
	}
	@Override
	public InputStream openStream(URI url){
		try {
			if("file".equals(url.getScheme())){
				File file = new File(URLDecoder.decode(url.getPath(),"UTF-8"));
				if(file.exists()){
					return new FileInputStream(file);
				}
				return null;
			}
			String host = findHeader("Host");
			return getClassResource(host);
		} catch (IOException e) {
			log.warn(e);
			return null;
		}
	}
	private InputStream getClassResource(String host) {
		int p = host.indexOf(':');
		if(p>0){
			host = host.substring(0,p);
		}
		String[] data = host.split("[\\.]");
		StringBuilder buf = new StringBuilder();
		p = data.length;
		while (p-- > 0) {
			buf.append(data[p]);
			buf.append('/');
		}
		buf.append("web/");
		String name = buf.toString();
		return server.getClass().getClassLoader().getResourceAsStream(name);
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

