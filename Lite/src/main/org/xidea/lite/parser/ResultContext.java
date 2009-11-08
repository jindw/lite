package org.xidea.lite.parser;

import java.util.List;

import org.xidea.lite.Plugin;

/**
 * 要设计为继承安全，这个接口不能滥用
 * 而且，实现不能直接调用this。否则容易形成孤岛
 * @author jindw
 */
public interface ResultContext {
	public static final Object END_INSTRUCTION = new Object[0];
	
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
	 * 获取结束节点,开始为0
	 * @see org.xidea.lite.parser.impl.ResultContextImpl#getDepth()
	 * @return
	 */
	public int getDepth();
	/**
	 * 获取结束当前状态的开始节点位置
	 * @return
	 */
	public int findBegin();
	/**
	 * 获取结束当前状态的开始节点位置
	 * @return
	 */
	public int findBeginType();
	
	/**
	 * 获取指定位置的节点类别
	 * @return
	 */
	public int getType(int offset);


	/**
	 * 设置translator，同时更新featrueMap（结果翻译起对某些特征可能不支持）
	 * @param translator
	 */
	public void setResultTranslator(ResultTranslator translator);

	/**
	 * @return 经过优化后的树形结果集
	 */
	public List<Object> toList();

	/**
	 * @return 经过结果转换后的代码
	 */
	public String toCode();
	/**
	 * 添加静态文本（不编码）
	 * 
	 * @param text
	 */
	public void append(String text);

	public void append(String text, boolean encode, char escapeQute);
	

	/**
	 * 添加一段呆编译的中间代码
	 * @param item
	 */
//	public void append(ResultItem item);

	/**
	 * 添加模板指令
	 * 
	 * @param text
	 */
	public void appendAll(List<Object> instruction);

//	public void clearPreviousText();

	public void appendEL(Object el);

	public void appendAttribute(String name, Object el);

	public void appendXmlText(Object el);

	public void appendIf(Object testEL);

	/**
	 * @see org.xidea.lite.parser.impl.ResultContextImpl#appendElse(Object)
	 * @param testEL
	 */
	public void appendElse(Object testEL);

	public void appendFor(String var, Object itemsEL, String status);

	public int appendEnd();

	public void appendVar(String name, Object valueEL);

	public void appendCaptrue(String varName);

	public void appendPlugin(Class<? extends Plugin> class1, Object propertiesEL);

	public String addGlobalObject(Class<? extends Object> impl, String key);

	public String allocateId();
	
	
}