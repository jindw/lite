package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xidea.el.ExpressionFactory;
import org.xidea.lite.parser.ResultItem;
import org.xidea.lite.parser.TextParser;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.NodeParser;
import org.xidea.lite.parser.ParserHolder;
import org.xidea.lite.parser.ResourceContext;
import org.xidea.lite.parser.ResultContext;
import org.xidea.lite.parser.ResultTranslator;
import org.xidea.lite.parser.XMLContext;
import org.xml.sax.SAXException;

public class ParseContextImpl implements ParseContext {
	private static final long serialVersionUID = 1L;
	protected ResourceContext resourceContext;
	protected XMLContext xmlContext;
	protected ResultContext resultContext;
	protected ParserHolder parserHolder;
	
	protected ParseContextImpl() {
	}

	public ParseContextImpl(URL base, Map<String, String> featrues,
			NodeParser<? extends Object>[] parsers, TextParser[] ips) {
		initialize(base, featrues, parsers, ips);
	}

	public ParseContextImpl(ParseContext parent, 
			ResultTranslator translator) {
		initializeFromParent(parent);
		initializeTranslator(parent, translator);
	}

	protected void initializeTranslator(ParseContext parent, 
			ResultTranslator translator) {
		if (translator instanceof ExpressionFactory) {
			this.resultContext
					.setExpressionFactory((ExpressionFactory) translator);
		}
		HashMap<String, String> oldFeatrues = new HashMap<String, String>(
				parent.getFeatrueMap());
		Set<String> support = translator.getSupportFeatrues();
		if (support != null) {
			Map<String, String> newFeatrues = this.getFeatrueMap();
			for (String key : oldFeatrues.keySet()) {
				if (support.contains(key)) {
					newFeatrues.put(key, oldFeatrues.get(key));
				}
			}
		}
	}



	protected void initializeFromParent(ParseContext parent) {
		if(parent instanceof ParseContextImpl){
			ParseContextImpl parent2 = (ParseContextImpl)parent;
			this.resourceContext = parent2.resourceContext;
			this.xmlContext = parent2.xmlContext;
		}else{
			this.resourceContext = parent;
			this.xmlContext = parent;
		}
		//需要重设 ParseChain 的context
		this.parserHolder = new ParseHolderImpl(this,parent);
		this.resultContext = new ResultContextImpl(parent);
	}


	@SuppressWarnings("unchecked")
	protected void initialize(URL base, Map<String, String> featrues,
			NodeParser<? extends Object>[] parsers, TextParser[] ips) {
		resourceContext = new ResourceContextImpl(base);
		resultContext = new ResultContextImpl();
		xmlContext = new XMLContextImpl(this);
		parserHolder = new ParseHolderImpl(this, parsers, ips);
		initializeFeatrues(featrues);

	}

	protected void initializeFeatrues(Map<String, String> featrues) {
		if (featrues != null) {
			String v = featrues.get("compress");
			if (v != null) {
				xmlContext.setCompress("true".equalsIgnoreCase(v));
			}
			v = featrues.get("reserveSpace");
			if (v != null) {
				xmlContext.setReserveSpace("true".equalsIgnoreCase(v));
			}
			v = featrues.get("format");
			if (v != null) {
				xmlContext.setFormat("true".equalsIgnoreCase(v));
			}
			getFeatrueMap().putAll(featrues);
		}
	}

	public List<Object> parseText(String text, int defaultType) {
		int type = resourceContext.getTextType();
		int mark = this.mark();
		List<Object> result;
		try {
			resourceContext.setTextType(defaultType);
			parse(text);
		} finally {
			resourceContext.setTextType(type);
			result = this.reset(mark);
		}
		return result;
	}

	public void parse(Object source) {
		getTopChain().process(source);
	}

	public final TextParser[] getTextParsers() {
		return parserHolder.getTextParsers();
	}

	public final String getFeatrue(String key) {
		return resultContext.getFeatrue(key);
	}

	public final Map<String, String> getFeatrueMap() {
		return resultContext.getFeatrueMap();
	}
	// delegate methods...

	public final void addResource(URL resource) {
		resourceContext.addResource(resource);
	}

	public final URL createURL(String file, URL parentURL) {
		return resourceContext.createURL(file, parentURL);
	}

	public final Object getAttribute(Object key) {
		return resourceContext.getAttribute(key);
	}

	public final URL getCurrentURL() {
		return resourceContext.getCurrentURL();
	}

	public final InputStream getInputStream(URL url) {
		return resourceContext.getInputStream(url);
	}

	public final Collection<URL> getResources() {
		return resourceContext.getResources();
	}

	public final void setAttribute(Object key, Object value) {
		resourceContext.setAttribute(key, value);
	}

	public final void setCurrentURL(URL currentURL) {
		resourceContext.setCurrentURL(currentURL);
	}

	public final String addGlobalObject(Class<? extends Object> impl, String key) {
		return resultContext.addGlobalObject(impl, key);
	}

	public final String addGlobalObject(Object object, String key) {
		return resultContext.addGlobalObject(object, key);
	}

	public final void append(String text) {
		resultContext.append(text);
	}
	public final void append(ResultItem text) {
		resultContext.append(text);
	}

	public final void append(String text, boolean encode, char escapeQute) {
		resultContext.append(text, encode, escapeQute);
	}

	public final void appendAll(List<Object> instruction) {
		resultContext.appendAll(instruction);
	}

	public final void appendAttribute(String name, Object el) {
		resultContext.appendAttribute(name, el);
	}

	public final void appendCaptrue(String varName) {
		resultContext.appendCaptrue(varName);
	}

	public final void appendEL(Object el) {
		resultContext.appendEL(el);
	}

	public final void appendElse(Object testEL) {
		resultContext.appendElse(testEL);
	}

	public final void appendEnd() {
		resultContext.appendEnd();
	}

	public final void appendFor(String var, Object itemsEL, String status) {
		resultContext.appendFor(var, itemsEL, status);
	}

	public final void appendIf(Object testEL) {
		resultContext.appendIf(testEL);
	}

	public final void appendVar(String name, Object valueEL) {
		resultContext.appendVar(name, valueEL);
	}

	public final void appendXmlText(Object el) {
		resultContext.appendXmlText(el);
	}

	public final void appendAdvice(Class<? extends Object> clazz, Object el) {
		resultContext.appendAdvice(clazz, el);
	}

	public final int mark() {
		return resultContext.mark();
	}

	public final Object parseEL(String eltext) {
		return resultContext.parseEL(eltext);
	}


	public final List<Object> reset(int mark) {
		return resultContext.reset(mark);
	}

	public final void setExpressionFactory(ExpressionFactory expressionFactory) {
		resultContext.setExpressionFactory(expressionFactory);
	}

	public final int findBeginType() {
		return resultContext.findBeginType();
	}

	public final int findBegin() {
		return resultContext.findBegin();
	}

	public final int getDepth() {
		return resultContext.getDepth();
	}

	public final int getType(int offset) {
		return resultContext.getType(offset);
	}

	public final List<Object> toList() {
		return resultContext.toList();
	}
	public final String toCode() {
		return resultContext.toCode();
	}

	public final void beginIndent() {
		xmlContext.beginIndent();
	}

	public final void endIndent() {
		xmlContext.endIndent();
	}

	public final boolean isCompress() {
		return xmlContext.isCompress();
	}

	public final boolean isFormat() {
		return xmlContext.isFormat();
	}

	public final boolean isReserveSpace() {
		return xmlContext.isReserveSpace();
	}

	public final void setCompress(boolean compress) {
		xmlContext.setCompress(compress);
	}

	public final void setFormat(boolean format) {
		xmlContext.setFormat(format);
	}

	public final void setReserveSpace(boolean keepSpace) {
		xmlContext.setReserveSpace(keepSpace);
	}

	public final int getTextType() {
		return resourceContext.getTextType();
	}

	public final Document loadXML(URL url) throws SAXException, IOException {
		return xmlContext.loadXML(url);
	}

	public final DocumentFragment selectNodes(Node doc, String xpath)
			throws XPathExpressionException {
		return xmlContext.selectNodes(doc, xpath);
	}

	public final Node transform(URL parentURL, Node doc, String xslt)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException,
			IOException {
		return xmlContext.transform(parentURL, doc, xslt);
	}

	public final void addTextParser(TextParser iparser) {
		parserHolder.addTextParser(iparser);
	}

	public final void addNodeParser(NodeParser<? extends Node> iparser) {
		parserHolder.addNodeParser(iparser);
	}

	public final ParseChain getTopChain() {
		return parserHolder.getTopChain();
	}

	public final void setTextType(int textType) {
		resourceContext.setTextType(textType);
	}

	public void setResultTranslator(ResultTranslator translator) {
		resultContext.setResultTranslator(translator);
	}


}