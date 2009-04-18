package org.xidea.lite.parser;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xidea.el.ExpressionFactory;

/**
 * @see ParseContextImpl
 */
public interface ParseContext {

	public static final Object END_INSTRUCTION = new Object[0];


	/**
	 * 如果compress为真，或者 reserveSpace为真 则该属性失效
	 * @param format
	 */
	public boolean isFormat();
	public void setFormat(boolean format);
	/**
	 * 如果 reserveSpace为真 则该属性失效
	 * @return
	 */
	public boolean isCompress();
	public void setCompress(boolean compress);

	/**
	 * 该属性为真时，compress 和 format都将失效
	 * @return
	 */
	public boolean isReserveSpace();
	public void setReserveSpace(boolean keepSpace);

	/**
	 * 在XMLParser中判断平设置，Core标签将缩进做了回逄1�7处理
	 * 
	 * @return
	 */
	public int getDepth();
	public void beginIndent();
	/**
	 * @see ParseContextImpl#endIndent()
	 */
	public void endIndent();

	/**
	 * 记录一下编译上下文特征变量，该对象不可被修改
	 * @param featrues {url,value}
	 */
	public void setFeatrueMap(Map<String, String> featrues);
	public String getFeatrue(String key);
	
	/**
	 * 记录一下编译上下文状态
	 * @param key
	 * @param value
	 */
	public void setAttribute(Object key, Object value);
	public Object getAttribute(Object key);

	/**
	 * 获取当前正在解析的模版URL
	 * @return
	 */
	public URL getCurrentURL();
	public void setCurrentURL(URL currentURL);
	/**
	 * 如果file相于根目录（/path/...），以base作为根目录处理
	 * 否则以parentURL，或者base作为parent直接new URL处理。
	 * @see org.xidea.lite.parser.ParseContextImpl#createURL
	 * @param parentURL
	 * @param file
	 * @return
	 */
	public URL createURL(URL parentURL, String file);
	public InputStream getInputStream(URL url);
	/**
	 * 添加（记录）解析相关的资源
	 * @param resource
	 */
	public void addResource(URL resource);
	public Set<URL> getResources();
	/**
	 * 自定义表达式解析器
	 * @param expressionFactory
	 */
	public void setExpressionFactory(ExpressionFactory expressionFactory);
	public Object optimizeEL(String eltext);
	/**
	 * @return 经过优化后的树形结果集
	 */
	public List<Object> toResultTree();
	public int mark();
	/**
	 * @param mark
	 * @return 经过优化后的一维结果集
	 */
	public List<Object> reset(int mark);
	
	public String addGlobalObject(Class<? extends Object> impl,String key);
	public String addGlobalObject(Object object,String key);

	public void append(String text);
	public void append(String text,boolean encode,char quteChar);
	public void appendAll(List<Object> items);
	public void removeLastEnd();
	public void appendEL(Object testEL);
	public void appendAttribute(String name, Object el);
	public void appendXmlText(Object el);
	public void appendIf(Object testEL);
	public void appendElse(Object testEL);
	public void appendEnd();
	public void appendVar(String name, Object valueEL);
	public void appendCaptrue(String varName);
	public void appendFor(String var, Object itemsEL, String status);
}