package org.xidea.lite.servlet;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parser.impl.DecoratorContextImpl;

public class ServletTemplateEngine extends TemplateEngine {
	private static final Log log = LogFactory.getLog(ServletTemplateEngine.class);
	private ServletConfig config;
	private ServletContext context;

	public ServletTemplateEngine(ServletConfig config) {
		//this.parser = new XMLParser(transformerFactory,xpathFactory);
		this.config = config;
		this.context = config.getServletContext();
		this.featrues = buildFeatrueMap(config);
		
		try {
			String decoratorPath = getParam("decoratorMapping",DEFAULT_DECORATOR_MAPPING);
			this.decoratorMapper = new DecoratorContextImpl(context.getResourceAsStream(decoratorPath));
		} catch (Exception e) {
			log.error("装载页面装饰配置信息失败", e);
		}
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
	protected URL getResource(String path) throws MalformedURLException {
		return new File(context.getRealPath(path)).toURI().toURL();
	}
}
