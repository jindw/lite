package org.jside.webserver.action;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;

import org.jside.webserver.RequestUtil;
import org.xidea.lite.Template;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.impl.DecoratorContextImpl;
import org.xidea.lite.parser.impl.HotTemplateEngine;

public class HotTemplateAction extends TemplateAction {

	private HotTemplateEngine engine = new HotTemplateEngine((URI) null, null) {

		@Override
		protected URI getResource(String path) {
			return HotTemplateAction.this.getResource(path);
		}

		private URI decoratorConfig;

		@Override
		protected Template createTemplate(String path, ParseContext parseContext) {
			URI config = getResource("/WEB-INF/decorators.xml");
			if (config != null && !config.equals(decoratorConfig)) {
				File file = RequestUtil.getFile(config);
				decoratorContext = new DecoratorContextImpl(config, file);
				decoratorConfig = config;
			}
			return super.createTemplate(path, parseContext);
		}

		@Override
		protected ParseContext createParseContext() {
			ParseContext context = super.createParseContext();
			context.getFeatrueMap().putAll(featrueMap);
			return context;
		}
	};

	public HotTemplateAction(URI file) throws MalformedURLException {
		super(file);
	}

	@Override
	public Template getTemplate(String path) {
		return engine.getTemplate(path);
	}

}