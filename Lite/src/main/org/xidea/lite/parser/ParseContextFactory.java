package org.xidea.lite.parser;


public interface ParseContextFactory {

	/**
	 * 创建一个解析客户端模板的解析上下文对象
	 * 该上下文对象不能有任何特征，避免客户端无法支持
	 * @param fn 函数名称
	 * @return
	 */
	public ParseContext createContext(ParseContext parent);
	public ParseContext createContext(ParseContext parent,ResultTransformer transformer);

}
