package org.xidea.lite.parser;

import java.util.List;

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
	 * 创建一个解析客户端模板的解析上下文对象
	 * 该上下文对象不能有任何特征，避免客户端无法支持
	 * @param fn 函数名称
	 * @return
	 */
//	public ParseContext createClientContext(String fn);

}