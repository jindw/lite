package org.xidea.lite.parser;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.xidea.el.ExpressionFactory;

/**
 * @see ParseContextImpl
 */
public interface ParseContext extends ResultContext, XMLContext {

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

	public String addGlobalObject(Class<? extends Object> impl,String key);
	public String addGlobalObject(Object object,String key);


}