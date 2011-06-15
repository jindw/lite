package org.xidea.lite.parse;

import java.util.List;
import java.util.Map;

import org.xidea.el.ExpressionFactory;

/**
 * 要设计为继承安全，这个接口不能滥用
 * 而且，实现不能直接调用this。否则容易形成孤岛
 * @author jindw
 */
public interface ResultContext {

	public Object parseEL(String el);

	/**
	 * 自定义表达式解析器
	 * 
	 * @param expressionFactory
	 */
	public void setExpressionFactory(ExpressionFactory expressionFactory);

	/**
	 * 记录一下当前位置，reset的参考位置
	 * 
	 * @return
	 */
	public int mark();

	/**
	 * @param mark
	 * @return 经过优化后的一维结果集
	 */
	public List<Object> reset(int mark);
	/**
	 * 获取指定位置的节点类别
	 * @return
	 */
	public int getType(int offset);
	/**
	 * 添加静态文本（不编码）
	 * 
	 * @param text
	 */
	public void append(String text);

	/**
	 * 添加模板指令
	 * 
	 * @param instruction
	 */
	public void appendAll(List<Object> instruction);

	/**
	 * @param el
	 */
	public void appendEL(Object el);

	public void appendXA(String name, Object el);

	public void appendXT(Object el);

	public void appendIf(Object testEL);

	/**
	 * @see org.xidea.lite.impl.ResultContextImpl#appendElse(Object)
	 * @param testEL
	 */
	public void appendElse(Object testEL);

	public void appendFor(String var, Object listEL, String status);

	public int appendEnd();

	public void appendVar(String name, Object valueEL);

	public void appendCapture(String varName);

	public void appendPlugin(String pluginClass,  Map<String, Object> config);

	public String allocateId();
	/**
	 * @return 经过优化后的树形结果集
	 */
	public List<Object> toList();
	
}