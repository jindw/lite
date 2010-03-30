package org.jside.webserver.action;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.jside.webserver.RequestContext;
import org.jside.webserver.RequestUtil;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parser.impl.HotTemplateEngine;
import org.xidea.lite.parser.impl.ResourceContextImpl;

public class TemplateAction extends ResourceContextImpl {
	static Constructor<TemplateEngine> hotEngine;
	static{
		try {
			String hotClass = "org.xidea.lite.parser.impl.HotTemplateEngine";
			Constructor<TemplateEngine> hotEngine = (Constructor<TemplateEngine>)Class.forName(hotClass).getConstructor(URI.class,URI.class);
		} catch (Throwable w) {
			hotEngine = null;
		}
	}
	private String contentType = "text/html;charset=";
	private TemplateEngine engine ;
	public TemplateAction(URI base) {
		super(base);

		
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	protected void reset(RequestContext requestContext) {
		URI newRoot = requestContext.getResource("/");
		if (newRoot != null) {
			if (!newRoot.equals(base)) {
				base = newRoot;
				engine = null;
			}
		}
		if(engine == null){
			try {
				engine = hotEngine.newInstance(base,requestContext.getResource("/WEB-INF/lite.xml"));
			} catch (Throwable w) {
				engine = new TemplateEngine(base);
			}
		}
	}
	public URI createURI(String path, URI parentURI) {
		if(parentURI==null){
			URI result = getResource(path);
			if(result!=null){
				return result;
			}
			parentURI = base;
		}
		return super.createURI(path, parentURI);
	}

	public void execute() throws IOException {
		RequestContext context = RequestContext.get();
		reset(context);
		OutputStreamWriter out = new OutputStreamWriter(context
				.getOutputStream(), context.getEncoding());
		if (contentType != null) {
			if (contentType.endsWith("=")) {
				context.setContentType(contentType + context.getEncoding());
			} else {
				context.setContentType(contentType);
			}
		}
		render(context.getRequestURI(), context.getValueStack(), out);
	}

	public void render(String path,Object context, Writer out) throws IOException {
		engine.render(path, context, out);
	}

	protected URI getResource(String pagePath) {
		try {
			if (base == null) {
				RequestContext context = RequestContext.get();
				URI uri = context.getResource(pagePath);
				if (uri != null) {
					uri = toExistResource(uri);
					if (uri != null) {
						return uri;
					}
				}
			} else {
				if(pagePath.length() == 0){
					return base;
				}
				if (pagePath.startsWith("/")) {
					pagePath = pagePath.substring(1);
				}
				URI uri = base.resolve(pagePath);
				uri = toExistResource(uri);
				if (uri != null) {
					return uri;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	private URI toExistResource(URI uri) throws MalformedURLException {
		File file = null;
		if (uri.getScheme().equals("file")) {
			file = RequestUtil.getFile(uri.toURL());
		//}else if (uri.getScheme().equals("classpath")) {
		}
		if (file != null) {
			if (file.exists()) {
				return uri;
			}
		} else {
			return uri;
		}
		return null;
	}

}