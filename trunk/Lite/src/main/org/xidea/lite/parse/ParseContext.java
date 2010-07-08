package org.xidea.lite.parse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.XMLContextImpl;
import org.xml.sax.SAXException;

/**
 * @see ParseContextImpl
 */
public interface ParseContext extends ResultContext,ParseConfig,ParserHolder {
	public String NS_CORE = "http://www.xidea.org/ns/lite/core";
	public String FEATRUE_ENCODING = "http://www.xidea.org/featrues/lite/output-encoding";
	public String FEATRUE_MIME_TYPE = "http://www.xidea.org/featrues/lite/output-mime-type";

	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public void parse(Object source);

	/**
	 * 给出文件内容或url，解析模版源文件
	 */
	public List<Object> parseText(String text,int textType);
	/**
	 * 如果file相于根目录（/path/...），以base作为根目录处理
	 * 否则以parentURI，或者base作为parent直接new URL处理。
	 * @param file
	 * @param parentURI
	 * @see org.xidea.lite.impl.ParseContextImpl#createURI
	 * @see org.xidea.lite.impl.ResourceContextImpl#createURI
	 * @return
	 */
	public URI createURI(String file);

	public InputStream openStream(URI url);

	/**
	 * 装载指定文档。数据源需要从ResourceContext中获取资源数据
	 * @see ResourceContext#openStream(parentURI)
	 * @param createURI
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document loadXML(URI parentURI) throws SAXException, IOException;

	/**
	 * @param doc
	 * @param xpath
	 * @return
	 * @throws XPathExpressionException
	 */
	public DocumentFragment selectNodes(Node doc,String xpath)
			throws XPathExpressionException;

	/**
	 * @param parentURI
	 * @param doc
	 * @param xslt
	 * @see XMLContextImpl#transform(URI, Node, Node)
	 * @return
	 * @throws TransformerConfigurationException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws IOException
	 */
	public Node transform(Node doc, Node xslt)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException,
			IOException;
	/**
	 * 记录一下编译上下文特征变量，该对象不可被修改
	 * @param featrues {url,value}
	 */
	public String getFeatrue(String key);
}