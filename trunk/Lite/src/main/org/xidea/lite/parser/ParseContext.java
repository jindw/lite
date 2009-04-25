package org.xidea.lite.parser;

import org.xidea.lite.parser.impl.ParseContextImpl;

/**
 * @see ParseContextImpl
 */
public interface ParseContext extends ResourceContext,ResultContext, XMLContext {

	/**
	 * 记录一下编译上下文特征变量，该对象不可被修改
	 * @param featrues {url,value}
	 */
	public void setFeatrue(String key, String value);
	public String getFeatrue(String key);
	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public void parse(Object source);
	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public void parseText(String source,int defaultType);

	public InstructionParser[] getInstructionParsers();
	
	public void addInstructionParser(InstructionParser iparser);
	public void addNodeParser(NodeParser<? extends Object> iparser);
}