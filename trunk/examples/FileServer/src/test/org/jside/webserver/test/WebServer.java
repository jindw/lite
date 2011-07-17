package org.jside.webserver.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Map;

import org.jside.webserver.CGIEnvironment;
import org.jside.webserver.CGIRunner;
import org.jside.webserver.MutiThreadWebServer;
import org.jside.webserver.RequestContext;
import org.jside.webserver.RequestUtil;
import org.xidea.el.ExpressionFactory;
import org.xidea.jsi.web.JSIService;
import org.xidea.lite.impl.HotTemplateEngine;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;

public class WebServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path = "../Lite2/web";
		if(args!=null && args.length>0){
			path = args[0];
		}
		final File root = new File(path);
		final URI base = root.toURI();
		final JSIService js = new JSIService(){
			protected void addHeader(Object[] context, String key ,String value) {
				RequestContext request = (RequestContext) context[0];
				request.setResponseHeader(key+':'+value);
			}
		};
		js.addLib(new File(root,"WEB-INF/lib"));
		js.addSource(new File(root,"scripts"));
		js.addSource(new File(root,"WEB-INF/classes"));
		final ParseConfig config = new ParseConfigImpl(base, base.resolve("WEB-INF/lite.xml"));
		final HotTemplateEngine ht = new HotTemplateEngine(config);
		MutiThreadWebServer mtws = new MutiThreadWebServer(base){
			public void processRequest(RequestContext context) throws Exception {
				String uri = context.getRequestURI();
				String rp = CGIEnvironment.toRealPath(base, uri);
				if(rp.endsWith(".php") ){
					Map<String, String> envp= new CGIEnvironment(context).toMap(null);
					CGIRunner cr = new CGIRunner(context, "php-cgi.exe", 
							envp,
							new File(new File(base),rp).getParentFile()
					, null);
					cr.run();
				} else {
					String prefix = "/scripts/";
					if(uri.startsWith(prefix)){
						js.service(uri.substring(prefix.length()), context.getParams(), context.getOutputStream(), context);
					}else if(uri.endsWith(".xhtml")){
						OutputStream os = context.getOutputStream();
						Map<String, String> fm = config.getFeatureMap(uri);
						String encoding = fm.get(ParseContext.FEATURE_ENCODING);
						context.setEncoding(encoding);
						String mimeType = fm.get(ParseContext.FEATURE_MIME_TYPE);
						context.setMimeType(mimeType == null?"text/html":mimeType);
						OutputStreamWriter out = new OutputStreamWriter(os,encoding);
						Object data = loadData(root, uri);
						ht.render(uri, data, out);
						out.flush();
					}else{
						RequestUtil.printResource();
					}
				}
			}
		};
		mtws.start();
	}

	private static Object loadData(final File root, String uri) throws IOException {
		String jsonpath = uri.replaceFirst(".\\w+$", ".json");
		Object data = new Object();
		if(jsonpath.endsWith(".json")){
			File df = new File(root,jsonpath);
			if(df.exists()){
				String source = ParseUtil.loadTextAndClose(new FileInputStream(df));
				data = ExpressionFactory.getInstance().create(source).evaluate(data);
			}
		}
		return data;
	}

}
