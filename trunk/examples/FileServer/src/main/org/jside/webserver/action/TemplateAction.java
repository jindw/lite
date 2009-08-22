package org.jside.webserver.action;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;

import org.jside.webserver.HttpUtil;
import org.jside.webserver.RequestContext;
import org.xidea.lite.TemplateEngine;

public class TemplateAction extends TemplateEngine {
	protected URL root;
	private String contentType = "text/html;charset=UTF-8";
	private Class<? extends Object> resourceLoader = TemplateAction.class;
	private static TemplateAction instance;

	public static TemplateAction getInstance() {
		if (instance == null) {
			try {
				instance = new HotTemplateAction(new File("web"));
			} catch (Throwable w) {
				instance = new TemplateAction();
			}
		}
		return instance;
	}

	protected TemplateAction() {
		super(null);
	}

	public void execute() throws IOException {
		RequestContext context = RequestContext.get();
		reset(context);
		OutputStreamWriter out = new OutputStreamWriter(context
				.getOutputStream(), "UTF-8");
		render(context.getRequestURI(), context.getValueStack(), out);
		HttpUtil.printResource(out, contentType);
	}

	public void reset(RequestContext requestContext) {
		URL newRoot = requestContext.getResource("/");
		if (newRoot == null) {
			if (root != null) {
				root = newRoot;
				reset(newRoot);
			}
		} else {
			if (!newRoot.equals(root)) {
				reset(newRoot);
			}
		}
	}

	public void reset(URL newRoot) {
		templateMap.clear();
		root = newRoot;
	}

	protected URL getResource(String pagePath) {
		try {
			if (root != null) {
				if (root.getProtocol().equals("file")) {
					File file = new File(URLEncoder.encode(root.getFile(),
							"UTF-8"), pagePath);

					if (file.exists()) {
						return file.toURI().toURL();
					}

				} else {
					return new URL(root, pagePath);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return resourceLoader.getResource(pagePath);
	}

}