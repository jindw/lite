package org.xidea.lite.parser;

import java.util.List;
import java.util.Map;

import org.xidea.lite.parser.impl.ParseContextImpl;

/**
 * @see ParseContextImpl
 */
public interface ParseContext extends ParserHolder,ResourceContext,ResultContext, XMLContext {

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
	/**
	 * 创建一个解析客户端模板的解析上下文对象
	 * 该上下文对象不能有任何特征，避免客户端无法支持
	 * @param fn 函数名称
	 * @return
	 */
//	public ParseContext createClientContext(String fn);

}