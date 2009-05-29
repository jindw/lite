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
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;
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
	protected String id;
	protected ResultTranslator translator;
	
	protected ParseContextImpl() {
	}

	public ParseContextImpl(URL base, Map<String, String> featrues,
			Parser<? extends Object>[] parsers, InstructionParser[] ips) {
		initialize(base, featrues, parsers, ips);
	}

	public ParseContextImpl(ParseContext parent, String fn,
			ResultTranslator translator) {
		initializeFromParent(parent);
		initializeTranslator(parent, fn, translator);
	}

	protected void initializeTranslator(ParseContext parent, String fn,
			ResultTranslator translator) {
		this.id = fn;
		this.translator = translator;
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
			Parser<? extends Object>[] parsers, InstructionParser[] ips) {
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

	public InstructionParser[] getInstructionParsers() {
		return parserHolder.getInstructionParsers();
	}

	public String getFeatrue(String key) {
		return resultContext.getFeatrue(key);
	}

	public Map<String, String> getFeatrueMap() {
		return resultContext.getFeatrueMap();
	}
	// delegate methods...

	public void addResource(URL resource) {
		resourceContext.addResource(resource);
	}

	public URL createURL(String file, URL parentURL) {
		return resourceContext.createURL(file, parentURL);
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

	public void appendAdvice(Class<? extends Object> clazz, Object el) {
		resultContext.appendAdvice(clazz, el);
	}

	public int mark() {
		return resultContext.mark();
	}

	public Object parseEL(String eltext) {
		return resultContext.parseEL(eltext);
	}


	public List<Object> reset(int mark) {
		return resultContext.reset(mark);
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		resultContext.setExpressionFactory(expressionFactory);
	}

	public int findBeginType() {
		return resultContext.findBeginType();
	}

	public int findBegin() {
		return resultContext.findBegin();
	}

	public int getDepth() {
		return resultContext.getDepth();
	}

	public int getType(int offset) {
		return resultContext.getType(offset);
	}

	public List<Object> toList() {
		return resultContext.toList();
	}
	public String toCode() {
		if(translator==null){
			return resultContext.toCode();
		}else{
			return translator.translate(this,id);
		}
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

	public int getTextType() {
		return resourceContext.getTextType();
	}

	public Document loadXML(URL url) throws SAXException, IOException {
		return xmlContext.loadXML(url);
	}

	public DocumentFragment selectNodes(Node doc, String xpath)
			throws XPathExpressionException {
		return xmlContext.selectNodes(doc, xpath);
	}

	public Node transform(URL parentURL, Node doc, String xslt)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException,
			IOException {
		return xmlContext.transform(parentURL, doc, xslt);
	}

	public void addInstructionParser(InstructionParser iparser) {
		parserHolder.addInstructionParser(iparser);
	}

	public void addNodeParser(Parser<? extends Node> iparser) {
		parserHolder.addNodeParser(iparser);
	}

	public ParseChain getTopChain() {
		return parserHolder.getTopChain();
	}

	public void setTextType(int textType) {
		resourceContext.setTextType(textType);
	}


}