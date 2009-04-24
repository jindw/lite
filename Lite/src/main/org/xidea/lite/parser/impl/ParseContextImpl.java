package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.el.ExpressionFactory;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;
import org.xidea.lite.parser.ResourceContext;
import org.xidea.lite.parser.ResultContext;
import org.xidea.lite.parser.XMLContext;
import org.xml.sax.SAXException;

public class ParseContextImpl implements ParseContext {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unchecked")
	protected static Parser[] DEFAULT_PARSER_LIST = { 
		new HTMLNodeParser(),new CoreXMLNodeParser(),
			new DefaultXMLNodeParser(),new TextParser() };

	protected ResourceContext resourceContext;
	protected XMLContext xmlContext;
	protected ResultContext resultContext;

	private Map<String, String> featrues;
	protected ParseChainImpl topChain;

	protected ParseContextImpl() {
	}

	@SuppressWarnings("unchecked")
	public ParseContextImpl(URL base) {
		this(base, new HashMap<String, String>(), DEFAULT_PARSER_LIST);
	}

	public ParseContextImpl(URL base, Map<String, String> featrues,
			Parser<? extends Object>[] parsers) {
		this.featrues = featrues;
		if (parsers != null) {
			ParseChainImpl current = topChain = new ParseChainImpl(this,
					parsers[0]);
			for (int i = 1; i < parsers.length; i++) {
				ParseChainImpl chain = new ParseChainImpl(this, parsers[i]);
				current.insertAfter(chain);
				current = chain;

			}
		}
		resourceContext = new ResourceContextImpl(base);
		resultContext = new ResultContextImpl(new ArrayList<Object>());
		xmlContext = new XMLContextImpl(this);
	}

	public void parse(Object source, int defaultType) {
		int type = xmlContext.getELType();
		((XMLContextImpl)xmlContext).setELType(defaultType);
		parse(source);
		((XMLContextImpl)xmlContext).setELType(type);
	}

	public void parse(Object source) {
		if (source instanceof Node || source instanceof String) {
			topChain.process(source);
		} else if (source instanceof NodeList) {
			NodeList list = (NodeList) source;
			for (int i = 0; i < list.getLength(); i++) {
				parse(list.item(i));
			}
		} else if (source instanceof NamedNodeMap) {
			NamedNodeMap list = (NamedNodeMap) source;
			for (int i = 0; i < list.getLength(); i++) {
				parse(list.item(i));
			}
		} else {
			topChain.process(source);
		}
	}

	public String getFeatrue(String key) {
		return featrues.get(key);
	}

	public void setFeatrue(String key, String value) {
		this.featrues.put(key, value);
	}

	// delegate methods...
	public void addResource(URL resource) {
		resourceContext.addResource(resource);
	}

	public URL createURL(URL parentURL, String file) {
		return resourceContext.createURL(parentURL, file);
	}

	public Object getAttribute(Object key) {
		return resourceContext.getAttribute(key);
	}

	public URL getCurrentURL() {
		return resourceContext.getCurrentURL();
	}

	public InputStream getInputStream(URL url) {
		return resourceContext.getInputStream(url);
	}

	public Collection<URL> getResources() {
		return resourceContext.getResources();
	}

	public void setAttribute(Object key, Object value) {
		resourceContext.setAttribute(key, value);
	}

	public void setCurrentURL(URL currentURL) {
		resourceContext.setCurrentURL(currentURL);
	}

	public String addGlobalObject(Class<? extends Object> impl, String key) {
		return resultContext.addGlobalObject(impl, key);
	}

	public String addGlobalObject(Object object, String key) {
		return resultContext.addGlobalObject(object, key);
	}

	public void append(String text) {
		resultContext.append(text);
	}

	public void append(String text, boolean encode, char escapeQute) {
		resultContext.append(text, encode, escapeQute);
	}

	public void appendAll(List<Object> instruction) {
		resultContext.appendAll(instruction);
	}

	public void appendAttribute(String name, Object el) {
		resultContext.appendAttribute(name, el);
	}

	public void appendCaptrue(String varName) {
		resultContext.appendCaptrue(varName);
	}

	public void appendEL(Object el) {
		resultContext.appendEL(el);
	}

	public void appendElse(Object testEL) {
		resultContext.appendElse(testEL);
	}

	public void appendEnd() {
		resultContext.appendEnd();
	}

	public void appendFor(String var, Object itemsEL, String status) {
		resultContext.appendFor(var, itemsEL, status);
	}

	public void appendIf(Object testEL) {
		resultContext.appendIf(testEL);
	}

	public void appendVar(String name, Object valueEL) {
		resultContext.appendVar(name, valueEL);
	}

	public void appendXmlText(Object el) {
		resultContext.appendXmlText(el);
	}

	public int mark() {
		return resultContext.mark();
	}

	public Object optimizeEL(String eltext) {
		return resultContext.optimizeEL(eltext);
	}

	public void removeLastEnd() {
		resultContext.removeLastEnd();
	}

	public List<Object> reset(int mark) {
		return resultContext.reset(mark);
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		resultContext.setExpressionFactory(expressionFactory);
	}

	public List<Object> toResultTree() {
		return resultContext.toResultTree();
	}

	public void beginIndent() {
		xmlContext.beginIndent();
	}

	public void endIndent() {
		xmlContext.endIndent();
	}

	public boolean isCompress() {
		return xmlContext.isCompress();
	}

	public boolean isFormat() {
		return xmlContext.isFormat();
	}

	public boolean isReserveSpace() {
		return xmlContext.isReserveSpace();
	}

	public void setCompress(boolean compress) {
		xmlContext.setCompress(compress);
	}

	public void setFormat(boolean format) {
		xmlContext.setFormat(format);
	}

	public void setReserveSpace(boolean keepSpace) {
		xmlContext.setReserveSpace(keepSpace);
	}

	public int getELType() {
		return xmlContext.getELType();
	}

	public Node loadXML(URL url) throws SAXException, IOException {
		return xmlContext.loadXML(url);
	}

	public Node selectNodes(String xpath, Node doc)
			throws XPathExpressionException {
		return xmlContext.selectNodes(xpath, doc);
	}

	public Node transform(URL parentURL, Node doc, String xslt)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException,
			IOException {
		return xmlContext.transform(parentURL, doc, xslt);
	}

}