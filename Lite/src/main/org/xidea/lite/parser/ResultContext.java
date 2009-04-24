package org.xidea.lite.parser;

import java.util.List;

import org.xidea.el.ExpressionFactory;

public interface ResultContext {
	public static final Object END_INSTRUCTION = new Object[0];
	/**
	 * @return 经过优化后的树形结果集
	 */
	public List<Object> toResultTree();

	/**
	 * 记录一下当前位置，reset的参考位置
	 * @return
	 */
	public int mark();
	/**
	 * @param mark
	 * @return 经过优化后的一维结果集
	 */
	public List<Object> reset(int mark);

	/**
	 * 添加静态文本（不编码）
	 * @param text
	 */
	public void append(String text);
	public void append(String text,boolean encode,char escapeQute);
	/**
	 * 添加模板指令
	 * @param text
	 */
	public void appendAll(List<Object> instruction);
	public void removeLastEnd();
	public void appendEL(Object el);
	public void appendAttribute(String name, Object el);
	public void appendXmlText(Object el);
	public void appendIf(Object testEL);
	public void appendElse(Object testEL);
	public void appendFor(String var, Object itemsEL, String status);
	public void appendEnd();
	public void appendVar(String name, Object valueEL);
	public void appendCaptrue(String varName);
	
	

	public String addGlobalObject(Class<? extends Object> impl,String key);
	public String addGlobalObject(Object object,String key);

	/**
	 * 自定义表达式解析器
	 * @param expressionFactory
	 */
	public void setExpressionFactory(ExpressionFactory expressionFactory);
	public Object optimizeEL(String eltext);
}