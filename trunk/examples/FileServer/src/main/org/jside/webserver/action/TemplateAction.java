package org.jside.webserver.action;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jside.webserver.RequestContext;
import org.jside.webserver.RequestUtil;
import org.xidea.lite.TemplateEngine;

@SuppressWarnings("unchecked")
public class TemplateAction {
	private static Log log = LogFactory.getLog(TemplateAction.class);
	private static Constructor<TemplateEngine> DEFAULT_HOT_ENGINE;
	protected Constructor<TemplateEngine> hotEngine = DEFAULT_HOT_ENGINE;

	static {
		try {
			String hotClass = "org.xidea.lite.impl.HotTemplateEngine";
			DEFAULT_HOT_ENGINE = (Constructor<TemplateEngine>) Class.forName(
					hotClass).getConstructor(URI.class, URI.class);
		} catch (Throwable w) {
			DEFAULT_HOT_ENGINE = null;
		}
	}
	private String mimeType = "text/html";
	private TemplateEngine engine;
	private URI base;

	public TemplateAction(URI base) {
		this.base = base;
	}

	public TemplateAction(URI base,Constructor<TemplateEngine> hotEngine) {
		this.hotEngine = hotEngine;
		this.base = base;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}


	public void execute() throws IOException {
		RequestContext context = RequestUtil.get();
		if (mimeType != null) {
			context.setMimeType(mimeType);
		}
		OutputStreamWriter out = new OutputStreamWriter(
				context.getOutputStream(), context.getEncoding());
		if (engine.getClass() != TemplateEngine.class) {
			URI newRoot = context.getResource("/");
			if (newRoot != null) {
				if (base == null || !newRoot.equals(base)) {
					engine = null;
					base = newRoot;
				}
			}
		}
		render(context.getRequestURI(), context.getContextMap(), out);
	}

	public void render(String path, Object context, Writer out)
			throws IOException {
		if (engine == null) {
			try {
				if (hotEngine != null) {
					engine = hotEngine.newInstance(base,
							base.resolve("./WEB-INF/lite.xml"));
				}
			} catch (Throwable w) {
				log.warn("热加载模板实现装载失败", w);
			}
			if(engine == null){
				engine = new TemplateEngine(base);
			}
		}
		engine.render(path, context, out);
	}
}