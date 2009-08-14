package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.parser.ResultContext;
import org.xidea.lite.parser.ResultTranslator;

public abstract class JSTranslator implements ResultTranslator,
		ExpressionFactory {
	private static Log log = LogFactory.getLog(JSTranslator.class);

	protected Set<String> supportFeatrues = new HashSet<String>();
	
	protected boolean compress;
	protected String path = "<file>";
	protected String id = "";

	public static JSTranslator newTranslator(String id, boolean compress,
			String pathInfo) {
		JSTranslator instance = null;
		try {
			instance = new RhinoJSTranslator();
		} catch (NoClassDefFoundError e) {
			try {
				instance = new Java6JSTranslator();
			} catch (NoClassDefFoundError e2) {
				log.error("找不到您的JS运行环境，不能为您编译前端js", e2);
				throw e2;
			}
		}
		instance.id = id;
		instance.compress = compress;
		instance.path = pathInfo;
		instance.initializeTranslator();
		return instance;
	}

	public Set<String> getSupportFeatrues() {
		return supportFeatrues;
	}

	@SuppressWarnings("unchecked")
	private void initializeTranslator() {
		try {
			ClassLoader loader = JSTranslator.class.getClassLoader();
			InputStream boot = loader.getResourceAsStream("boot.js");
			if (boot != null) {
				try {
					eval(new InputStreamReader(boot, "utf-8"));
					eval("$import('org.xidea.lite:ResultTranslator')");
				} catch (Exception e) {
					log.debug("尝试JSI启动编译脚本失败", e);
				}
			}
			if (boot == null) {
				eval("var window = this;");
				InputStream compressed = loader
						.getResourceAsStream("org/xidea/lite/template.js");
				eval(new InputStreamReader(compressed, "utf-8"));
			}
			eval("var transformer = new ResultTranslator();");
			supportFeatrues = new HashSet<String>(
					(Collection) JSONDecoder
							.decode((String) eval("uneval(transformer.getSupportFeatrues())")));
		} catch (Exception e) {
			log.error("初始化Rhino JS引擎失败", e);
		}
	}
	
	

	public Object eval(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		int count;
		char[] cbuf = new char[1024];
		while ((count = in.read(cbuf)) > -1) {
			out.write(cbuf, 0, count);
		}
		return eval(out.toString());
	}

	private String buildJS(String script) {
		String code = (String) eval(script);
		eval("function x(){" + code + "}");
		return code;
	}

	protected abstract Object eval(String source);

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
			log.warn("生成js代码失败：" + id + "@" + path, e);
			return "function " + id + "(){alert('生成js代码失败：'+"
					+ JSONEncoder.encode(e.getMessage()) + ")}";
		}
	}

	private String getTranslateScript(ResultContext context) {
		String liteJSON = JSONEncoder.encode(context.toList());
		String featrueJSON = JSONEncoder.encode(context.getFeatrueMap());
		String script = "transformer.transform(" + liteJSON + ",'" + id + "',"
				+ featrueJSON + ")+''";
		return script;
	}

	// mock other
	public Expression create(Object el) {
		throw new UnsupportedOperationException();
	}

	public Object parse(String expression) {
		return expression;
	}

	public void error(String message, String sourceName, int line,
			String lineSource, int lineOffset) {
		log.error(message + ":" + sourceName + ":" + lineOffset + "," + line
				+ "@" + lineSource);
	}

	public void warning(String message, String sourceName, int line,
			String lineSource, int lineOffset) {
		log.warn(message + ":" + sourceName + ":" + lineOffset + "," + line
				+ "@" + lineSource);
	}
}