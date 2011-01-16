package org.xidea.lite.parse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;


import org.w3c.dom.Document;
import org.xidea.lite.impl.ParseContextImpl;
import org.xml.sax.SAXException;

/**
 * @see ParseContextImpl
 */
public interface ParseContext extends ResultContext, ParseConfig, ParserHolder {
	public String NS_CORE = "http://www.xidea.org/lite/core";
	// 默认值：link|input|meta|img|br|hr
//	public String FEATRUE_HTML_LEAF = "http://www.xidea.org/lite/featrues/html-leaf";
	// 默认值为空
	public String FEATRUE_HTML_TRIM = "http://www.xidea.org/lite/featrues/html-trim";
	// 默认值为空
	public String FEATRUE_HTML_JAVASCRIPT_COMPRESSOR = "http://www.xidea.org/lite/featrues/html-javascript-compressor";
	// 默认值为utf-8
	public String FEATRUE_ENCODING = "http://www.xidea.org/lite/featrues/output-encoding";
	// 默认值为 text/html
	public String FEATRUE_MIME_TYPE = "http://www.xidea.org/lite/featrues/output-mime-type";
	// 默认值为 null,推荐用通配符模式：layout='${dir}/layout.xhtml'
	public String FEATRUE_LAYOUT = "http://www.xidea.org/lite/featrues/config-layout";

	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public void parse(Object source);

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

	public InputStream openStream(URI url);

	/**
	 * 装载指定XML文档。
	 * 
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

	
	public ParseContext createNew();
	/**
	 * 记录一下编译上下文特征变量，该对象不可被修改
	 * 
	 * @param featrues
	 *            {url,value}
	 */
	public String getFeatrue(String key);

	/**
	 * 只读，外部不允许修改
	 */
	public Map<String, String> getFeatrueMap();
}