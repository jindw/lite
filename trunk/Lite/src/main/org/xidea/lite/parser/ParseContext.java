package org.xidea.lite.parser;

import java.net.URL;
import java.util.List;
import java.util.Set;

public interface ParseContext {
	

	public void append(String text);

	public void appendIndent();

	public void appendAll(List<Object> items);

	public void removeLastEnd();

	/**
	 * 在XMLParser中判断平设置，Core标签将缩进做了回逄1�7处理
	 * 
	 * @return
	 */
	public int getDepth();

	public void setDepth(int depth);

	public boolean isFormat();

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

	/**
	 * 添加（记录）解析相关的资源
	 * @param resource
	 */
	public void addResource(URL resource);
	public Set<URL> getResources();



	@SuppressWarnings("unchecked")
	public List<Object> toResultTree();

	public String addGlobalInvocable(Class<? extends Object> impl);

	public void appendIf(Object testEL);

	public void appendAttribute(Object el, String name);

	public void appendEnd();

	public void appendElse(Object testEL);

	public void appendCaptrue(String varName);

	public void appendFor(String var, Object itemsEL, String status);

	public void appendVar(Object valueEL, String name);

}