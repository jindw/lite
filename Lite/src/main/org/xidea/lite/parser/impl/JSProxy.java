package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.parser.NodeParser;
import org.xidea.lite.parser.ResultTranslator;
import org.xidea.lite.parser.TextParser;

public abstract class JSProxy {
	private static Log log = LogFactory.getLog(JSProxy.class);
	private static ClassLoader loader = JSProxy.class.getClassLoader();

	public static JSProxy newProxy() {
		try {
			return RhinoProxy.class.newInstance();
		} catch (Throwable e) {
			try {
				return Java6Proxy.class.newInstance();
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	private boolean jsiAvailable = false;

	protected abstract Object eval(String source, String pathInfo);

	public abstract <T> T wrapToJava(Object thiz, Class<T> clasz);

	public abstract Object invoke(Object thiz, String methodName,
			Object... args);


	public abstract String compress(String source);

	public <T> T createObject(Class<T> clasz,String... scripts){
		Object o = null;
		for (String s:scripts) {
			o = eval(s);
		}
		return wrapToJava(o, clasz);
	}
	public TextParser createTextParser(String... scripts){
		Object o = null;
		for (String s:scripts) {
			o = eval(s);
		}
		Object fn = eval("{wrap:function(o){o.getPriority||this.getPriority;return o},getPriority:function(){return o.priority || 1;}}");
		return wrapToJava(invoke(fn, "wrap", o), TextParser.class);
	}
	@SuppressWarnings("unchecked")
	public NodeParser<Object> createNodeParser(String... scripts){
		Object o = null;
		for (String s:scripts) {
			o = eval(s);
		}
		Object fn = eval("{wrap:function(o){return o instanceof Function?{parse:o}:o}}");
		return wrapToJava(invoke(fn, "wrap", o), NodeParser.class);
	}
	
	public ResultTranslator createJSTranslator(String id) {
		Object translator = null;
		try {
			if (this.isJSIAvailable()) {
				translator = this
						.eval("new ($import('org.xidea.lite:ResultTranslator',null))('"
								+ id + "')");
			} else {
				URL compressed = loader
						.getResource("org/xidea/lite/template.js");

				this.eval(compressed);
				translator = this.eval("new ResultTranslator('" + id + "')");

			}
			return this.wrapToJava(translator, ResultTranslator.class);
		} catch (Exception e) {
			log.error("初始化Rhino JS引擎失败", e);
			throw new RuntimeException(e);
		}
	}
	public boolean isJSIAvailable() {
		return jsiAvailable;
	}

	@SuppressWarnings("unchecked")
	protected void initialize() {
		eval("window = this;");
		URL boot = loader.getResource("boot.js");
		if (boot != null) {
			try {
				eval(boot);
				jsiAvailable = (Boolean) eval("!!(this.$import && this.$JSI)");
			} catch (Exception e) {
				log.debug("尝试JSI启动编译脚本失败", e);
			}
		}
	}

	public Object eval(URL resource) throws IOException {
		InputStreamReader in = new InputStreamReader(resource.openStream(),
				"utf-8");
		try {
			return eval(in, resource.toString());
		} finally {
			in.close();
		}
	}

	public Object eval(Reader in, String pathInfo) throws IOException {
		StringWriter out = new StringWriter();
		int count;
		char[] cbuf = new char[1024];
		while ((count = in.read(cbuf)) > -1) {
			out.write(cbuf, 0, count);
		}
		return eval(out.toString(), pathInfo);
	}

	public Object eval(String source) {
		return this.eval(source, "source:" + source);
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