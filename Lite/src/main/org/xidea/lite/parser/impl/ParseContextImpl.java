package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.w3c.dom.Document;
import org.xidea.lite.parser.ResourceContext;
import org.xidea.lite.parser.TextParser;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.NodeParser;
import org.xidea.lite.parser.ResultTranslator;
import org.xml.sax.SAXException;

/**
 * 不要较差调用，交叉调用，用this代替，确保继承安全
 * 取消final 之后，容易引发一个短路bug，等到发现之后再修复吧。
 * @author jindw
 */
public class ParseContextImpl extends ParseContextProxy implements ParseContext {
	private static final long serialVersionUID = 1L;

	protected final Map<String, String> featrueMap= new HashMap<String, String>();
	
	protected ParseContextImpl() {
	}
	public ParseContextImpl(ResourceContext base){
		initialize(base, null, null, null);
	}
	public ParseContextImpl(ResourceContext base, Map<String, String> featrues,
			NodeParser<? extends Object>[] parsers, TextParser[] ips) {
		initialize(base, featrues, parsers, ips);
	}

	public ParseContextImpl(ParseContext parent, 
			ResultTranslator translator) {
		super(parent);
		//需要重设 ParseChain 的context
		this.parserHolder = new ParseHolderImpl(this,parent);
		this.resultContext = new ResultContextImpl(this);
		this.featrueMap.putAll(parent.getFeatrueMap());
		this.setResultTranslator(translator);
	}


	protected void initialize(ResourceContext base, Map<String, String> featrues,
			NodeParser<? extends Object>[] parsers, TextParser[] ips) {
		resourceContext = base;
		xmlContext = new XMLContextImpl(this);
		resultContext = new ResultContextImpl(this);
		parserHolder = new ParseHolderImpl(this, parsers, ips);
		initializeFeatrues(featrues);
	}

	protected void initializeFeatrues(Map<String, String> newFeatrues) {
		if (newFeatrues != null) {
			String v = newFeatrues.get("compress");
			if (v != null) {
				xmlContext.setCompress("true".equalsIgnoreCase(v));
			}
			v = newFeatrues.get("reserveSpace");
			if (v != null) {
				xmlContext.setReserveSpace("true".equalsIgnoreCase(v));
			}
			v = newFeatrues.get("format");
			if (v != null) {
				xmlContext.setFormat("true".equalsIgnoreCase(v));
			}
			featrueMap.clear();
			featrueMap.putAll(newFeatrues);
		}
	}

	public String getFeatrue(String key) {
		return featrueMap.get(key);
	}

	public Map<String, String> getFeatrueMap() {
		return featrueMap;
	}

	public Document loadXML(String path) throws SAXException, IOException{
		if(path.startsWith("<")){
			path = "data:text/xml;charset=utf-8,"+URLEncoder.encode(path, "UTF-8");
		}
		return loadXML(URI.create(path));
	}

	public void parse(Object source) {
		getTopChain().process(source);
	}

	public List<Object> parseText(String text, int defaultType) {
		int type = this.getTextType();
		int mark = this.mark();
		List<Object> result;
		try {
			this.setTextType(defaultType);
			parse(text);
		} finally {
			this.setTextType(type);
			result = this.reset(mark);
		}
		return result;
	}
}