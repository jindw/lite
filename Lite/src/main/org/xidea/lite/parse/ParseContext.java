package org.xidea.lite.parse;

import java.util.List;
import java.util.Map;

import org.xidea.lite.impl.ParseContextImpl;

/**
 * @see ParseContextImpl
 */
public interface ParseContext extends ParserHolder,ResourceContext,ResultContext,ParseConfig,XMLContext {
	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public void parse(Object source);

	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public List<Object> parseText(String text,int textType);
	/**
	 * 记录一下编译上下文特征变量，该对象不可被修改
	 * @param featrues {url,value}
	 */
	public String getFeatrue(String key);
	/**
	 * 获得特征表的直接引用，外部的修改也将直接影响解析上下文的特征表
	 * @return
	 */
	public Map<String, String> getFeatrueMap();


}