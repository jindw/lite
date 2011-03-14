package org.jside.webserver;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Encapsulates the CGI environment and rules to derive that environment from
 * the servlet container and request information.
 * 
 * <pre>
 * 
 * SERVER_PROTOCOL=HTTP/1.1
 * REQUEST_METHOD=GET
 * AUTH_TYPE=
 * SERVER_SOFTWARE=TOMCAT
 * SERVER_NAME=localhost
 * GATEWAY_INTERFACE=CGI/1.1
 * REMOTE_ADDR=0:0:0:0:0:0:0:1
 * REMOTE_HOST=0:0:0:0:0:0:0:1
 * QUERY_STRING=
 * HTTP_CONNECTION=keep-alive
 * SERVER_PORT=8080
 * CONTENT_TYPE=
 * CONTENT_LENGTH=
 * REQUEST_URI=/lite2/cgi-bin/demo/index.php
 * PATH_INFO=
 * SCRIPT_NAME=/lite2/cgi-bin/demo/index.php
 * SCRIPT_FILENAME=D:\workspace\Lite2\web\WEB-INF\cgi\demo\index.php
 * REMOTE_USER=
 * REMOTE_IDENT=
 * 
 * HTTP_COOKIE=run=1294303284087;
 * HTTP_HOST=localhost:8080
 * HTTP_ACCEPT=text/html,application/xhtml+xml,application/xml;q=0.9,*\/*;q=0.8, 
 * HTTP_USER_AGENT=Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13, 
 * HTTP_ACCEPT_ENCODING=gzip,deflate, 
 * HTTP_ACCEPT_LANGUAGE=zh-cn,zh;q=0.5, 
 * HTTP_ACCEPT_CHARSET=GB2312,utf-8;q=0.7,*;q=0.7, 
 * HTTP_KEEP_ALIVE=115
 * </pre>
 * 
 * @version $Revision: 896371 $, $Date: 2010-01-06 11:30:07 +0100 (Wed, 06 Jan
 *          2010) $
 * @since Tomcat 4.0
 * 
 * 
 */
public class CGIEnvironment {
	private static final Log log = LogFactory.getLog(CGIEnvironment.class);
	public String serverSoftware = "JSA";
	public String gatewayInterface = "CGI/1.1";
	public String serverProtocol = "HTTP/1.1";
	public String serverName = "localhost";
	public String serverPort = "80";
	public String requestMethod = "GET";
	public String requestUri = "/";
	public String pathInfo = "";
	/*-
	 * PATH_TRANSLATED must be determined after PATH_INFO (and the
	 * implied real cgi-script) has been taken into account.
	 *
	 * The following example demonstrates:
	 *
	 * servlet info   = /servlet/cgigw/dir1/dir2/cgi1/trans1/trans2
	 * cgifullpath    = /servlet/cgigw/dir1/dir2/cgi1
	 * path_info      = /trans1/trans2
	 * webAppRootDir  = servletContext.getRealPath("/")
	 *
	 * path_translated = servletContext.getRealPath("/trans1/trans2")
	 *
	 * That is, PATH_TRANSLATED = webAppRootDir + sPathInfoCGI
	 * (unless sPathInfoCGI is null or blank, then the CGI
	 * specification dictates that the PATH_TRANSLATED metavariable
	 * SHOULD NOT be defined.
	 *
	 * if (sPathInfoCGI != null && !("".equals(sPathInfoCGI))) {
	 * 	sPathTranslatedCGI = context.getRealPath(sPathInfoCGI);
	 * } else {
	 * 	sPathTranslatedCGI = null;
	 * }
	 * if (sPathTranslatedCGI == null || "".equals(sPathTranslatedCGI)) {
	 * 	// NOOP
	 * } else {
	 * 	envp.put("PATH_TRANSLATED", nullsToBlanks(sPathTranslatedCGI));
	 * }
	 */
	public String pathTranslated = null;
	public String scriptName = null;
	public String scriptFilename = null;
	public String queryString = null;
	public String remoteHost = null;
	public String remoteAddr = null;
	public String authType = "";
	public String remoteUser = "";// envp.put("REMOTE_USER",
	// nullsToBlanks(req.getRemoteUser()));
	public String remoteIdent = "";// envp.put("REMOTE_IDENT", ""); // not
	// necessary for full compliance

	public String contentType = "";
	public String contentLength = "";
	private RequestContext context;

	public CGIEnvironment(WebServer server,RequestContextImpl context) {
		this.context = context;
		
		//serverName = "localhost";
		serverPort = String.valueOf(server.getPort());
		requestMethod = context.getMethod();
		requestUri = context.getRequestURI();
		//pathInfo = "";
		//pathTranslated = null;
		scriptName = requestUri;
		scriptFilename = new File(context.getResource(requestUri)).getAbsolutePath();
		queryString = context.getQuery();
		remoteAddr = context.getRemoteAddr().getHostAddress();
		remoteHost = context.getRemoteAddr().getHostName();
		String key = context.getRequestHeader("Content-Length");
		if(key != null){
			contentLength = key;
		}
		key = context.getRequestHeader("Content-Type");
		if(key != null){
			contentType = key;
		}
		
		
	}

	public void appendTo(Map<String, String> envp) {
		Field[] fileds = this.getClass().getFields();
		for(Field f : fileds){
			if(f.getType() == String.class){
				StringBuffer key = new StringBuffer(f.getName());
				for(int i=key.length();i-->0;){
					char c = key.charAt(i);
					if(Character.isUpperCase(c)){
						key.insert(i, '_');
					}else{
						key.setCharAt(i, Character.toUpperCase(c));
					}
				}
				try {
					String value = (String)f.get(this);
					if(value != null){
						envp.put(key.toString(),value);
					}
				} catch (Exception e) {
					log.error(e);
				}
			}
			
		}
		List<String> headers = ((RequestContextImpl) context)
				.getRequestHeaders();
		for (String header : headers) {
			int p = header.indexOf(':');
			if (p > 0) {
				String key = header.substring(0, p);
				String value = header.substring(p + 1).trim();
				// REMIND: rewrite multiple headers as if received as single
				// REMIND: change character set
				// REMIND: I forgot what the previous REMIND means
				if ("AUTHORIZATION".equalsIgnoreCase(key)
						|| "PROXY_AUTHORIZATION".equalsIgnoreCase(key)) {
					// NOOP per CGI specification section 11.2
				} else {
					envp.put("HTTP_" + key.replace('-', '_'), value);
				}
			}
		}
	}

	public boolean isValid() {
		return true;
	}

}
