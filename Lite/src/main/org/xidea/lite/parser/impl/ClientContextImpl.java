package org.xidea.lite.parser.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.json.JSONEncoder;


public class ClientContextImpl extends ParseContextImpl {
	private static Log log = LogFactory.getLog(CoreXMLParser.class);
	private final static ExpressionFactory DEFAULT_CLIENT_FACTORY= new ExpressionFactory() {
		public Expression create(Object el) {
			throw new UnsupportedOperationException();
		}

		public Object parse(String expression) {
			return expression;
		}
	} ;
	private static JSBuilder DEFAULT_JS_BUILDER;

	static{
		try {
			DEFAULT_JS_BUILDER = new RhinoJSBuilder();
		} catch (NoClassDefFoundError e) {
			try {
				DEFAULT_JS_BUILDER = new Java6JSBuilder();
			} catch (NoClassDefFoundError e2) {
				log.error("找不到您的JS运行环境，不能为您编译前端js", e);

			}
		}
	}
	private ExpressionFactory clientFactory=DEFAULT_CLIENT_FACTORY;
	private JSBuilder jsBuilder = DEFAULT_JS_BUILDER;
	private String name;

	
	ClientContextImpl(ParseContextImpl parent,String fn){
		super(parent);
		this.name = fn;
		this.resultContext.setExpressionFactory(clientFactory);
	}

	@Override
	public String getFeatrue(String key) {
		return null;
	}

	public List<Object> toList() {
		List<Object> result = resultContext.toList();
		String js = jsBuilder.buildJS(result,name);
		if (this.isCompress()) {
			js = jsBuilder.compress(js);
		}
		result = new ArrayList<Object>();
		result.add(js);
		return result;
	}


	public String toJSON() {
		return JSONEncoder.encode(toList());
	}

}
