package org.jside.webserver.action;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.jside.webserver.HttpUtil;
import org.jside.webserver.RequestContext;
import org.xidea.lite.TemplateEngine;

public class TemplateAction extends TemplateEngine {
	private String contentType = "text/html;charset=";
	private Object root;

	public static TemplateAction create(URI root) {
		TemplateAction ta;
		try {
			ta = new HotTemplateAction(root);
		} catch (Throwable w) {
			ta = new TemplateAction(root);
		}
		return ta;
	}

	public TemplateAction(URI root) {
		super(root);
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void execute() throws IOException {
		RequestContext context = RequestContext.get();
		reset(context);
		OutputStreamWriter out = new OutputStreamWriter(context
				.getOutputStream(), context.getEncoding());
		if(contentType!=null){
			if(contentType.endsWith("=")){
				context.setContentType(contentType+context.getEncoding());
			}else{
				context.setContentType(contentType);
			}
		}
		render(context.getRequestURI(), context.getValueStack(), out);
	}

	protected void reset(RequestContext requestContext) {
		URL newRoot = requestContext.getResource("/");
		if (newRoot != null) {
			if (!newRoot.equals(root)) {
				root = newRoot;
				templateMap.clear();
			}
		}
	}

	protected URI getResource(String pagePath) {
		try {
			if (baseURI == null) {
				RequestContext context = RequestContext.get();
				URL url = context.getResource(pagePath);
				if (url != null) {
					URI uri = url.toURI();
					uri = toExistResource(uri);
					if (uri != null) {
						return uri;
					}
				}
			}else{
				if(pagePath.startsWith("/")){
					pagePath = pagePath.substring(1);
				}
				URI uri = new URL(baseURI.toURL(), pagePath).toURI();
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
		File file = HttpUtil.getFile(uri.toURL());
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