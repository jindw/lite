package org.xidea.lite.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.Template;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.impl.DecoratorContextImpl;
import org.xidea.lite.parser.impl.HotTemplateEngine;

public class ServletTemplateEngine extends HotTemplateEngine {
	private static final Log log = LogFactory.getLog(ServletTemplateEngine.class);
	private ServletConfig config;
	private ServletContext context;
	private boolean autocompile = true;

	public ServletTemplateEngine(ServletConfig config) {
		super((URI)null,null);
		//this.parser = new XMLParser(transformerFactory,xpathFactory);
		this.config = config;
		this.context = config.getServletContext();
		this.featrues = buildFeatrueMap(config);
		this.autocompile = getParam("autocompile", "true").equals("true");
		try {
			String decoratorPath = getParam("decoratorMapping",DEFAULT_DECORATOR_MAPPING);
			File file = new File(context.getRealPath(decoratorPath));
			this.decoratorContext = new DecoratorContextImpl(file.toURI(),file);
		} catch (Exception e) {
			log.error("装载页面装饰配置信息失败", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Template createTemplate(String path, ParseContext parseContext){
		if(autocompile){
			return super.createTemplate(path, parseContext);
		}else{
			try {
				File file = new File(context.getRealPath("/WEB-INF/litecached/"+path.replace('/', '.')));
				List<Object> list = JSONDecoder.decode(loadText(file));
				parseContext.addResource(file.toURI());
				return new Template((List<Object>)list.get(1));
			} catch (IOException e) {
				log.error(e);
				throw new RuntimeException(e);
			}
		}
	}

	private String loadText(File file) throws IOException {
		InputStreamReader in = new InputStreamReader(new FileInputStream(file),"utf-8");
		StringBuilder buf = new StringBuilder();
		char[] cbuf = new char[1024];
		int c;
		while((c = in.read(cbuf))>=0){
			buf.append(cbuf, 0, c);
		}
		return buf.toString();
	}

	protected Map<String, String> buildFeatrueMap(ServletConfig config) {
		@SuppressWarnings("unchecked")
		Enumeration<String> names = config.getInitParameterNames();
		HashMap<String, String> featrues = new HashMap<String, String>();
		while(names.hasMoreElements()){
			String name = names.nextElement();
			featrues.put(name,config.getInitParameter(name));
		}
		return featrues;
	}

	private String getParam(String name,String  defaultValue) {
		String decoratorPath = config.getInitParameter(name);
		if (decoratorPath == null) {
			decoratorPath = defaultValue;
		}
		return decoratorPath;
	}

	@Override
	protected URI getResource(String path){
		return new File(context.getRealPath(path)).toURI();
	}
}
