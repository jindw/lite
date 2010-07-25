package org.xidea.lite.impl.old;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;

/**
 * 不要较差调用，交叉调用，用this代替，确保继承安全
 * 取消final 之后，容易引发一个短路bug，等到发现之后再修复吧。
 * @author jindw
 */
public class ParseContextImpl extends ParseContextProxy implements ParseContext {
	private static final long serialVersionUID = 1L;

	protected final Map<String, String> featrueMap;

	public ParseContextImpl(ParseConfig config,String path) {
		featrueMap= new HashMap<String, String>();
		NodeParser<? extends Object>[] parsers = null;
		if(config !=null && path!=null){
			Map<String, String> f = config.getFeatrueMap(path);
			if(f != null){
				this.featrueMap.putAll(f);
			}
			parsers = ((ParseConfigImpl)config).getNodeParsers(path);
		}
		this.config = config;
		this.resultContext = new ResultContextImpl(this);
		this.parserHolder = new ParseHolderImpl(this, parsers);
	}
	public ParseContext createNew(){
		return new ParseContextImpl(this);
	}

	public ParseContextImpl(ParseContext parent) {
		super(parent);
		//需要重设 ParseChain 的context
		this.parserHolder = new ParseHolderImpl(this,parent);
		this.resultContext = new ResultContextImpl(this);
		if(parent instanceof ParseContextImpl){
			this.featrueMap = ((ParseContextImpl)parent).getFeatrueMap();
		}else{
			this.featrueMap = new HashMap<String, String>();
		}
	}

	public String getFeatrue(String key) {
		return featrueMap.get(key);
	}
	/**
	 * 获得特征表的直接引用，外部的修改也将直接影响解析上下文的特征表
	 * @return
	 */
	public Map<String, String> getFeatrueMap() {
		return featrueMap;
	}

	public void parse(Object source) {
		getTopChain().next(source);
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