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
	protected URL root = this.getClass().getResource("./");
	private String contentType = "text/html;charset=UTF-8";
	private boolean fromWeb = true;

	public static TemplateAction create(URL root, boolean fromWeb) {
		TemplateAction ta;
		try {
			ta = new HotTemplateAction(root);
		} catch (Throwable w) {
			ta = new TemplateAction(root);
		}
		ta.fromWeb = fromWeb;
		return ta;
	}

	public TemplateAction(URL root) {
		super((URI) null);
		this.reset(root);
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void execute() throws IOException {
		RequestContext context = RequestContext.get();
		reset(context);
		OutputStreamWriter out = new OutputStreamWriter(context
				.getOutputStream(), "UTF-8");
		context.setContentType(contentType);
		render(context.getRequestURI(), context.getValueStack(), out);
//		HttpUtil.printResource(out, contentType);
	}

	public void reset(RequestContext requestContext) {
		URL newRoot = requestContext.getResource("/");
		if (newRoot != null) {
			if (!newRoot.equals(root)) {
				reset(newRoot);
			}
		}
	}

	public URL getRoot() {
		return root;
	}

	public void reset(URL newRoot) {
		templateMap.clear();
		root = newRoot;
	}

	protected URI getResource(String pagePath) {
		try {
			if (fromWeb) {
				RequestContext context = RequestContext.get();
				URL url = context.getResource(pagePath);
				if (url != null) {
					URI uri = url.toURI();
					uri = toExistResource(uri);
					if (uri != null) {
						return uri;
					}
				}
			}
			if (root != null) {
				URI uri = new URL(root, pagePath).toURI();
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