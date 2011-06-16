package org.xidea.lite.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.json.JSONDecoder;
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
	private HashSet<URI> resources = new HashSet<URI>();
	private HashMap<Object, Object> attributeMap = new HashMap<Object, Object>();
	
	protected ParseConfig config;
	protected ResultContext resultContext;
	/**
	 * createNew 复制
	 */
	private URI currentURI = URI.create("lite:///");
	/**
	 * createNew 重置
	 */
	protected int textType = 0;
	private boolean preserveSpace;
	
	

	
	protected ParseContextProxy(ParseConfig config,Map<String, String> featureMap) {
		this.config = config;
		this.featureMap = featureMap;
		this.resultContext = new ResultContextImpl();
	}

	ParseContextProxy(ParseContextProxy parent) {
		// 需要重设 ParseChain 的context
		this.config = parent;
		this.featureMap =  parent.getFeatureMap();
		this.resultContext = parent;
		this.resources = parent.resources;
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

	public int getTextType() {
		return textType;
	}

//	public void setTextType(int textType) {
//		this.textType = textType;
//	}

	public boolean isReserveSpace() {
		return preserveSpace;
	}

	public void setReserveSpace(boolean keepSpace) {
		this.preserveSpace = keepSpace;
	}
	public final URI createURI(String path) {
		try {
			// TODO
			URI parent = this.getCurrentURI();
			return parent.resolve(path);
		} catch (Exception e) {
			log.error("uri 创建异常."+path,e);
			throw new RuntimeException(e);
		}
	}

	public final InputStream openStream(URI uri) {
		if("lite".equals(uri.getScheme())){
			String path = uri.getPath();
			if(path.startsWith("/")){
				path = path.substring(1);
			}
			uri = config.getRoot().resolve(path);
		}
		return ParseUtil.openStream(uri);
	}


	public final String loadText(URI uri) throws IOException {
		return ParseUtil.loadText(uri, (ParseContext) this);
	}

	public final Document loadXML(URI uri) throws SAXException, IOException {
		try{
			return ParseUtil.parse(uri, (ParseContext) this);
		}catch (SAXException e) {
			log.error("XML 解析失败："+uri,e);
			throw e;
		}catch (IOException e) {
			log.error("模板读取失败："+uri,e);
			throw e;
		}catch (RuntimeException e) {
			log.error("XML装载失败："+uri,e);
			throw e;
		}
	}

//	public final String getDecotatorPage(String path) {
//		return config.getDecotatorPage(path);
//	}

	public final Map<String, String> getFeatureMap(String path) {
		return config.getFeatureMap(path);
	}


	public final URI getRoot() {
		return config.getRoot();
	}

	public Map<String, List<String>> getExtensions(String path) {
		return config.getExtensions(path);
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


	public final void appendPlugin(String pluginClazz,  Map<String, Object> config) {
		resultContext.appendPlugin(pluginClazz, config);
	}
	public final void appendPlugin(String pluginClazz,  String config) {
		Map<String, Object> map = JSONDecoder.decode(config);
		resultContext.appendPlugin(pluginClazz, map);
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
		resources.add(resource);
	}

	public void setCurrentURI(URI currentURI) {
		if (currentURI != null) {
			this.addResource(currentURI);
			this.currentURI = currentURI;
		}
	}

	public final Collection<URI> getResources() {
		Collection<URI> result = new ArrayList<URI>(resources);
		result.addAll(config.getResources());
		return result;
	}
}
