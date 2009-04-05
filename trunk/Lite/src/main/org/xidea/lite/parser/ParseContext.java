package org.xidea.lite.parser;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.xidea.el.ExpressionFactory;

/**
 * @see ParseContextImpl
 */
public interface ParseContext {

	public static final Object END_INSTRUCTION = new Object[0];

	/**
	 * 在XMLParser中判断平设置，Core标签将缩进做了回逄1�7处理
	 * 
	 * @return
	 */
	public int getDepth();

	public void setDepth(int depth);

	public boolean isFormat();
	
	public void setFormat(boolean format);
	
	public boolean isCompress();
	
	public void setCompress(boolean format);

	public boolean isReserveSpace();

	public void setReserveSpace(boolean keepSpace);
	
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

	public List<Object> toResultTree();
	public int mark();
	public List<Object> reset(int mark);
	
	public String addGlobalObject(Class<? extends Object> impl,String key);
	public String addGlobalObject(Object object,String key);

	public void append(String text);
	public void append(String text,boolean encode,char quteChar);
	public void appendIndent();
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