package org.jside.webserver.action;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import org.xidea.lite.Template;
import org.xidea.lite.parser.impl.HotTemplateEngine;

public class HotTemplateAction extends TemplateAction {
	private HotTemplateEngine engine;

	protected HotTemplateAction(File file) throws MalformedURLException {
		if(file!=null){
			reset(file.toURI().toURL());
		}
	}


	@Override
	public Template getTemplate(String path) {
		return engine.getTemplate(path);
	}


	@Override
	public void reset(URL newRoot) {
		super.root = newRoot;
		if(newRoot.getProtocol().equals("file")){
			try {
				this.engine = new HotTemplateEngine(new File(URLDecoder.decode(newRoot.getFile(),"UTF-8")));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
	}

}