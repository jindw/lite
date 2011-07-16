package org.xidea.lite.parse;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;


import org.w3c.dom.Document;
import org.xidea.lite.impl.ParseContextImpl;
import org.xml.sax.SAXException;

/**
 * @see ParseContextImpl
 */
public interface ParseContext extends ResultContext, ParserHolder {
	public String NS_CORE = "http://www.xidea.org/lite/core";
	// 默认值：link|input|meta|img|br|hr
//	public String FEATURE_HTML_LEAF = "http://www.xidea.org/lite/features/html-leaf";
	// 默认值为空
	public String FEATURE_HTML_TRIM = "http://www.xidea.org/lite/features/html-trim";
	// 默认值为空
	public String FEATURE_HTML_JAVASCRIPT_COMPRESSOR = "http://www.xidea.org/lite/features/html-javascript-compressor";
	// 默认值为utf-8
	public String FEATURE_ENCODING = "http://www.xidea.org/lite/features/output-encoding";
	// 默认值为 text/html
	public String FEATURE_MIME_TYPE = "http://www.xidea.org/lite/features/output-mime-type";
	// 默认值为 null,可以采用相对路径模式：layout='./layout.xhtml'
	public String FEATURE_LAYOUT = "http://www.xidea.org/lite/features/config-layout";

	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public void parse(Object source);

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
	public <T> T getAttribute(Object key);
	/**
	 * 当前代码类型；
	 * 直接使用Template中的常量定义：
	 * Template.XT_TYPE
	 * Template.XA_TYPE
	 * Template.EL_TYPE
	 * @return
	 */
	public int getTextType();
	/**
	 * 是否保留空白（默认为false）
	 * @return
	 */
	public boolean isReserveSpace();
	public void setReserveSpace(boolean keepSpace);
	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public List<Object> parseText(String text, int textType);

	/**
	 * 如果file相于根目录（/path/...），以base作为根目录处理 否则以parentURI，或者base作为parent直接new
	 * URL处理。
	 * 
	 * @param file
	 * @param parentURI
	 * @see org.xidea.lite.impl.ParseContextImpl#createURI
	 * @return
	 */
	public URI createURI(String file);

	/**
	 * 装载指定XML文档。
	 * loadXML 一般不会设计为采用{@link ParseContext#loadText(URI)} 的结果，
	 * 因为xml的编码有自己的规范。
	 * @param uri
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document loadXML(URI uri) throws SAXException, IOException;
	/**
	 * 装载指定资源文本。
	 * 
	 * @param uri
	 * @return
	 * @throws IOException
	 */
	public String loadText(URI uri) throws IOException;
	/**
	 * 记录一下编译上下文特征变量，该对象不可被修改
	 * 
	 */
	public String getFeature(String key);

	/**
	 * 只读，外部不允许修改
	 * @return featureMap {url,value}
	 */
	public Map<String, String> getFeatureMap();
}