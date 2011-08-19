package org.xidea.lite.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xidea.el.ExpressionFactory;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.ResultContext;
import org.xml.sax.SAXException;

abstract public class ParseContextProxy implements ParseContext {
	private static Log log = LogFactory.getLog(ParseContextProxy.class);
	

	/**
	 * createNew 共享
	 */
	private final Map<String, String> featureMap;
	private final ArrayList<URI> resources;
	private final HashMap<Object, Object> attributeMap ;
	/**
	 * createNew 复制
	 */
	private URI currentURI = URI.create("lite:///");
	
	protected ParseConfig config;
	protected ResultContext resultContext;
	
	

	
	protected ParseContextProxy(ParseConfig config,Map<String, String> featureMap) {
		this.config = config;
		this.featureMap = featureMap;
		this.attributeMap = new HashMap<Object, Object>();
		this.resources = new ArrayList<URI>();
		this.resultContext = new ResultContextImpl();
	}

	ParseContextProxy(ParseContextProxy parent) {
		// 需要重设 ParseChain 的context
		this.config = parent.config;
		this.featureMap =  parent.getFeatureMap();
		this.resultContext = parent;
		this.attributeMap = parent.attributeMap;
		this.resources = parent.resources;
	}

	public ParseConfig getConfig(){
		return this.config;
	}

	public String getFeature(String key) {
		return featureMap.get(key);
	}

	public Map<String, String> getFeatureMap() {
		return featureMap;
	}
	public void setAttribute(Object key, Object value) {
		this.attributeMap.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(Object key) {
		return (T)this.attributeMap.get(key);
	}


//	public void setTextType(int textType) {
//		this.textType = textType;
//	}
//
//	public boolean isReserveSpace() {
//		return preserveSpace;
//	}
//
//	public void setReserveSpace(boolean keepSpace) {
//		this.preserveSpace = keepSpace;
//	}
	public final URI createURI(String path) {
		try {
			// TODO
			URI parent = this.getCurrentURI();
//			System.out.println("1"+parent);
//			System.out.println("2"+path);
//			System.out.println("3"+parent.resolve(path));
			//new RuntimeException().printStackTrace();
			return parent.resolve(path);
		} catch (Exception e) {
			log.error("uri 创建异常."+path,e);
			throw new RuntimeException(e);
		}
	}

	public final String loadText(URI uri) throws IOException {
		return config.loadText(uri);
	}

	public final Document loadXML(URI uri) throws SAXException, IOException {
		return config.loadXML(uri);
	}

	public final String allocateId() {
		return resultContext.allocateId();
	}

	public final void append(String text) {
		resultContext.append(text);
	}

	public final void appendAll(List<Object> instruction) {
		resultContext.appendAll(instruction);
	}

	public final void appendXA(String name, Object el) {
		resultContext.appendXA(name, el);
	}

	public final void appendXT(Object el) {
		resultContext.appendXT(el);
	}

	public final void appendCapture(String varName) {
		resultContext.appendCapture(varName);
	}

	public final void appendEL(Object el) {
		resultContext.appendEL(el);
	}

	public final void appendElse(Object testEL) {
		resultContext.appendElse(testEL);
	}

	public final int appendEnd() {
		return resultContext.appendEnd();
	}

	public final void appendFor(String var, Object itemsEL, String status) {
		resultContext.appendFor(var, itemsEL, status);
	}

	public final void appendIf(Object testEL) {
		resultContext.appendIf(testEL);
	}

	public final void appendVar(String name, Object valueEL) {
		resultContext.appendVar(name, valueEL);
	}


	public final void appendPlugin(String pluginClazz,  String config) {
		resultContext.appendPlugin(pluginClazz, config);
	}

	public final int mark() {
		return resultContext.mark();
	}

	public final List<Object> reset(int mark) {
		return resultContext.reset(mark);
	}


	public final int getType(int offset) {
		return resultContext.getType(offset);
	}

	public final List<Object> toList() {
		return resultContext.toList();
	}

	/**
	 * 自定义表达式解析器
	 * 
	 * @param expressionFactory
	 */
	public final void setExpressionFactory(ExpressionFactory expressionFactory) {
		resultContext.setExpressionFactory(expressionFactory);
	}

	public final Object parseEL(String eltext) {
		return resultContext.parseEL(eltext);
	}


	public URI getCurrentURI() {
		return currentURI;
	}


	public void addResource(URI resource) {
		if(!resources.contains(resource)){
			resources.add(resource);
		}
	}

	public void setCurrentURI(URI currentURI) {
		if (currentURI != null) {
			this.addResource(currentURI);
			this.currentURI = currentURI;
		}
	}

	public final Collection<URI> getResources() {
		ArrayList<URI> result = new ArrayList<URI>(config.getResources());
		result.addAll(resources);
		return result;
	}
}
