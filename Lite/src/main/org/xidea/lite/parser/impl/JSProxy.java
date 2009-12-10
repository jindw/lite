package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
			} catch (Throwable e1) {
				throw new RuntimeException("您的JDK不支持js，请补充rhino jar包。",e1);
			}
		}
	}

	private boolean jsiAvailable = false;
	
	protected boolean primitiveToJS = false;
	

	public abstract Object eval(String source, String pathInfo,Map<String, Object> varMap);

	public abstract Object invoke(Object thiz, String methodName,
			Object... args);

	public abstract <T> T wrapToJava(Object thiz, Class<T> clasz);

	public abstract String compress(String source);
	
	public Object eval(String source, String fileName) {
		return eval(source, fileName, null);
	}
	private Object eval(String... scripts) {
		Object o = null;
		for (String s:scripts) {
			o = eval(s);
		}
		return o;
	}

	public Object eval(URL resource) throws IOException {
		InputStream stream = resource.openStream();
		try {
			String text = ParseUtil.loadText(stream,"utf-8");
			return eval(text, resource.toString());
		} finally {
			stream.close();
		}
	}

	public Object eval(Reader in, String pathInfo) throws IOException {
		String text = ParseUtil.loadText(in);
		return eval(text, pathInfo);
	}



	public Object eval(String source) {
		return this.eval(source, "source:" + source);
	}

	public <T> T createObject(Class<T> clasz,String... scripts){
		Object o = eval(scripts);
		return wrapToJava(o, clasz);
	}
	public TextParser createTextParser(Object o){
		HashMap<String, Object> varMap = new HashMap<String, Object>();
		varMap.put("impl", o);
		eval(
				"if(impl instanceof Function){impl.parse=impl,impl.findStart=impl}" +
				"if(!impl.getPriority) {" +
				"impl.getPriority=function(){" +
				"return impl.priority == null? 1 : impl.priority;" +
				"}};",this.getClass().toString(),varMap);
		return wrapToJava(o, TextParser.class);
	}
	@SuppressWarnings("unchecked")
	public NodeParser<? extends Object> createNodeParser(Object o){
		HashMap<String, Object> varMap = new HashMap<String, Object>();
		varMap.put("impl", o);
		eval(
				"if(impl instanceof Function){impl.parse=impl};"
				,this.getClass().toString(),varMap);
		return wrapToJava(o, NodeParser.class);
	}
	public ResultTranslator createJSTranslator(String id) {
		Object translator = null;
		try {
			if (this.isJSIAvailable()) {
				translator = this
						.eval("new ($import('org.xidea.lite:Translator',null))('"
								+ id + "')");
			} else {
				URL compressed = loader
						.getResource("org/xidea/lite/template.js");

				this.eval(compressed);
				translator = this.eval("new Translator('" + id + "')");
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