package org.xidea.el.script;

import java.io.Reader;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;

/**
 * @see ExpressionFactoryImpl
 */
public class ExpressionEngine extends AbstractScriptEngine {
	private ScriptEngineFactory factory = null;

	public ExpressionEngine(ExpressionEngineFactory factory) {
		this.factory = factory;
	}

	public ExpressionEngine() {
		this.factory = new ExpressionEngineFactory();
	}

	/**
	 * 获得系统默认的表达式工厂(包含ECMA262 标准扩展的表达式工厂,状态(内置对象,运算符扩展)不允许修改)
	 * 
	 * @return
	 */
	public static ExpressionFactory getExpressionFactory() {
		return ExpressionFactoryImpl.getInstance();
	}

	public Bindings createBindings() {
		Bindings b = new SimpleBindings();
		return b;
	}

	public Object eval(String script, ScriptContext context)
			throws ScriptException {
		Map<String, Object> proxy = context == this.context? engineProxy:new MapProxy(context);
		return getExpressionFactory().create(script).evaluate(proxy);
	}

	public Object eval(Reader reader, ScriptContext context)
			throws ScriptException {
		return null;
	}

	public ScriptEngineFactory getFactory() {
		return factory;
	}

	private Map<String, Object> engineProxy = new MapProxy(context){
		public Object get(Object key) {
			return ExpressionEngine.this.get(key.toString());
		}

		public Object put(String key, Object value) {
			ExpressionEngine.this.put(key, value);
			return null;
		}
	};
}
class MapProxy extends AbstractMap<String, Object>{
	private ScriptContext context;
	MapProxy(ScriptContext context){
		this.context = context;
	}
	public Object get(Object key) {
		return this.context.getAttribute(key.toString());
	}

	public Object put(String key, Object value) {
		this.context.setAttribute(key, value,ScriptContext.ENGINE_SCOPE);
		return null;
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		throw new UnsupportedOperationException();
	}
};