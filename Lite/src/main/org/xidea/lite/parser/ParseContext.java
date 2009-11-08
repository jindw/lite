package org.xidea.lite.parser;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.xidea.el.ExpressionFactory;
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
	 * 设置translator，同时更新featrueMap（结果翻译起对某些特征可能不支持）
	 * @param translator
	 */
	public void setResultTranslator(ResultTranslator translator);

	/**
	 * 自定义表达式解析器
	 * 
	 * @param expressionFactory
	 */
	public void setExpressionFactory(ExpressionFactory expressionFactory);

	public Object parseEL(String eltext);

	/**
	 * @return 经过结果转换后的代码
	 */
	public String toCode();

	/**
	 * 获取当前正在解析的模版URI
	 * @return
	 */
	public URI getCurrentURI();

	/**
	 * 获取当前正在解析的模版URI
	 * 同事将该url记录在资源列表中
	 * @return
	 */
	public void setCurrentURI(URI currentURI);
	/**
	 * 添加（记录）解析相关的资源
	 * @param resource
	 */
	public void addResource(URI resource);
	public Collection<URI> getResources();

	/**
	 * 记录一下编译上下文状态
	 * @param key
	 * @param value
	 */
	public void setAttribute(Object key, Object value);
	public Object getAttribute(Object key);

	/**
	 * 当前代码类型；
	 * 直接使用Template中的常量定义：
	 * Template.XML_TEXT_TYPE
	 * Template.XML_ATTRIBUTE_TYPE
	 * Template.EL_TYPE
	 * @return
	 */
	public int getTextType();
	public void setTextType(int textType);
}