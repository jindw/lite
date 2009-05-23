package org.xidea.lite.parser;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import org.xidea.lite.parser.impl.ParseContextImpl;

/**
 * @see ParseContextImpl
 */
public interface ResourceContext{

	/**
	 * 获取当前正在解析的模版URL
	 * @return
	 */
	public URL getCurrentURL();

	/**
	 * 获取当前正在解析的模版URL
	 * 同事将该url记录在资源列表中
	 * @return
	 */
	public void setCurrentURL(URL currentURL);
	/**
	 * 如果file相于根目录（/path/...），以base作为根目录处理
	 * 否则以parentURL，或者base作为parent直接new URL处理。
	 * @see org.xidea.lite.parser.impl.ParseContextImpl#createURL
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
	public Collection<URL> getResources();

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