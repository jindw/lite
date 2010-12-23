package org.xidea.lite.parse;

import org.xidea.lite.impl.ParseContextImpl;

/**
 * @see ParseContextImpl
 * 要设计为继承安全，这个接口不能滥用
 * 而且，实现不能直接调用this。否则容易形成孤岛
 * @author jindw
 */
public interface ParserHolder{

	/**
	 * 获得当前的指令解析器列表
	 * @return
	 */
	public TextParser[] getTextParsers();
	/**
	 * 添加指令解析器
	 * @param textParser
	 */
	public void addTextParser(TextParser textParser);
	/**
	 * 添加节点解析器
	 * @param nodeParser
	 */
	public void addNodeParser(NodeParser<? extends Object> nodeParser);
	/**
	 * 添加指令解析器
	 * @param textParser
	 */
	public void addExtension(String namespace,String packageName);
	/**
	 * 获取当前context的顶级 parseChain对象
	 * @param iparser
	 */
	public ParseChain getTopChain();
}