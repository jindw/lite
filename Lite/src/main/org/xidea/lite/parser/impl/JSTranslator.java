package org.xidea.lite.parser.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.parser.ResultContext;
import org.xidea.lite.parser.ResultTranslator;

public abstract class JSTranslator implements ResultTranslator, ExpressionFactory {
	private static Log log = LogFactory.getLog(JSTranslator.class);
	private static JSTranslator instance;

	private boolean compress;
	private String path;
	private String id = "";
	
	public static JSTranslator newInstance(String id,boolean compress,String pathInfo) {
		if (instance == null) {
			try {
				instance = new RhinoJSTranslator();
			} catch (NoClassDefFoundError e) {
				try {
					instance = new Java6JSTranslator();
				} catch (NoClassDefFoundError e2) {
					log.error("找不到您的JS运行环境，不能为您编译前端js", e2);
				}
			}
			instance.id = id;
			instance.compress = compress;
			instance.path = pathInfo;
		};
		return instance;
	}


	public Expression create(Object el) {
		throw new UnsupportedOperationException();
	}

	public Object parse(String expression) {
		return expression;
	}

	protected abstract String buildJS(String script) throws Exception;

	public abstract String compress(String source);

	public String translate(ResultContext context) {
		try {
			String script = getTranslateScript(context);
			String result = this.buildJS(script);
			if (compress) {
				result = compress(result);
			}
			return result;
		} catch (Exception e) {
			log.warn("生成js代码失败："+id+"@"+path, e);
			return "function " + id + "(){alert('生成js代码失败：'+"
					+ JSONEncoder.encode(e.getMessage()) + ")}";

		}

	}

	private String getTranslateScript(ResultContext context) {
		String liteJSON = JSONEncoder.encode(context.toList());
		String featrueJSON = JSONEncoder.encode(context.getFeatrueMap());
		String script = "transformer.transform(" + liteJSON + ",'" + id
				+ "'," + featrueJSON + ")+''";
		return script;
	}
}