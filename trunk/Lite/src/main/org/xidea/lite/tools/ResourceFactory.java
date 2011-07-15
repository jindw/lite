package org.xidea.lite.tools;

import java.io.InputStream;

public interface ResourceFactory {
	/**
	 * 在资源处理过程中添加关联文件。
	 * @param currentPath
	 * @param relationPath
	 */
	public void addRelation(String currentPath,String relationPath);
	/**
	 * 当启用缓存时，在缓存失效或者没有缓存的时候，需要先清理缓存，并在资源处理过程中由处理插件添加关联文件。
	 * @return
	 */
	public InputStream getResourceAsStream(String path);
	/**
	 * 直接读取文件内容
	 * @return
	 */
	public InputStream getRawAsStream(String path);
	/**
	 * 获取文件编码信息
	 * @return
	 */
	public String getEncoding(String path);
//	/**
//	 * 列出资源搜索结果
//	 * @param includes
//	 * @param excludes
//	 * @return
//	 */
//	public String[] list(String[] includes,String[] excludes);
}
