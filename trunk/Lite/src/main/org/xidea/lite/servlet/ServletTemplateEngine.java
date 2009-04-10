package org.xidea.lite.servlet;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.TemplateEngine;
import org.xidea.lite.parser.DecoratorMapper;
import org.xidea.lite.parser.XMLParser;

public class ServletTemplateEngine extends TemplateEngine {
	private static final Log log = LogFactory.getLog(ServletTemplateEngine.class);
	private ServletConfig config;
	private ServletContext context;

	public ServletTemplateEngine(ServletConfig config) {
		String transformerFactory = config.getInitParameter("transformerFactory");
		String xpathFactory = config.getInitParameter("xpathFactory");
		this.parser = new XMLParser(transformerFactory,xpathFactory);
		this.config = config;
		this.context = config.getServletContext();
		try {
			String decoratorPath = getParam("decoratorMapping",DEFAULT_DECORATOR_MAPPING);
			this.decoratorMapper = new DecoratorMapper(context
					.getResourceAsStream(decoratorPath));
		} catch (Exception e) {
			log.error("装载页面装饰配置信息失败", e);
		}
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
