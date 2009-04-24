package org.xidea.lite.parser;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.xidea.lite.parser.impl.ParseContextImpl;
import org.xml.sax.SAXException;

/**
 * @see ParseContextImpl
 */
public interface ParseContext extends ResourceContext,ResultContext, XMLContext {

	/**
	 * 记录一下编译上下文特征变量，该对象不可被修改
	 * @param featrues {url,value}
	 */
	public void setFeatrue(String key, String value);
	public String getFeatrue(String key);
	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public void parse(Object source);
	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public void parse(Object source,int defaultType);
}