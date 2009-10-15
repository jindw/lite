package org.jside.webserver.action;

import java.net.MalformedURLException;
import java.net.URI;

import org.xidea.lite.Template;
import org.xidea.lite.parser.impl.HotTemplateEngine;

public class HotTemplateAction extends TemplateAction {
	private HotTemplateEngine engine = new HotTemplateEngine((URI)null,null){
		@Override
		protected URI getResource(String path) {
			return HotTemplateAction.this.getResource(path);
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