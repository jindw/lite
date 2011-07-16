package org.xidea.lite.tools;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public interface ResourceManager {
	/**
	 * 在资源处理过程中添加关联文件。
	 * @param relationPath
	 */
	public void addRelation(String relationPath);
	/**
	 * 直接读取文件内容
	 * @return
	 */
	public byte[] getRawBytes(String path) throws IOException;
	/**
	 * 将指定绝对路径的资源经过过滤处理后，做为字节流返回。
	 * 当启用缓存时，在缓存失效或者没有缓存的时候，需要先清理缓存。
	 * 并在资源处理过程中由处理插件添加关联文件。
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public byte[] getFilteredBytes(String path) throws IOException;
	public String getFilteredText(String path) throws IOException;
	
	/**
	 * 
	 * 将指定绝对路径的资源经过过滤处理后，做xml容错，并加上lite 附加资源信息，做为org.w3c.Document 对象返回。
	 * 
	 * 当启用缓存时，在缓存失效或者没有缓存的时候，需要先清理缓存。
	 * 并在资源处理过程中由处理插件添加关联文件。
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	public Document getFilteredDocument(String path) throws IOException, SAXException;
	/**
	 * 获取文件编码信息
	 * @return
	 */
	public String getEncoding(String path);
	public File getRoot();
	
	/**
	 * 
	 */
	public void addByteFilter(FilterPlugin<byte[]> filter);
	public void addStringFilter(FilterPlugin<String> filter);
	public void addDocumentFilter(FilterPlugin<Document> filter);
//	/**
//	 * 列出资源搜索结果
//	 * @param includes
//	 * @param excludes
//	 * @return
//	 */
//	public String[] list(String[] includes,String[] excludes);
}
