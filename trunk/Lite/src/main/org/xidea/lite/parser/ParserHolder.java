package org.xidea.lite.parser;

import org.w3c.dom.Node;
import org.xidea.lite.parser.impl.ParseContextImpl;

/**
 * @see ParseContextImpl
 */
public interface ParserHolder{

	/**
	 * 获得当前的指令解析器列表
	 * @return
	 */
	public InstructionParser[] getInstructionParsers();
	/**
	 * 添加指令解析器
	 * @param iparser
	 */
	public void addInstructionParser(InstructionParser iparser);
	/**
	 * 添加节点解析器
	 * @param iparser
	 */
	public void addNodeParser(Parser<? extends Node> iparser);
	
	/**
	 * 获取当前context的顶级 parseChain对象
	 * @param iparser
	 */
	public ParseChain getTopChain();
}