package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.el.ExpressionFactory;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;
import org.xidea.lite.parser.XMLContext;
import org.xml.sax.SAXException;

public class ParseContextImpl implements ParseContext {
	private static final long serialVersionUID = 1L;
	protected ResourceContextImpl resourceContext;
	protected XMLContext xmlContext;
	protected ResultContextImpl resultContext;
	protected ParseHolderImpl parserHolder;
	protected Map<String, String> featrues;
	protected ParseContextImpl() {
	}
	protected ParseContextImpl(ParseContextImpl parent){
		this.resourceContext = parent.resourceContext;
		this.xmlContext = parent.xmlContext;
		this.parserHolder = new ParseHolderImpl(this,parent.parserHolder);
		this.featrues = parent.featrues;
		this.resultContext = new ResultContextImpl();
	}
	public ParseContextImpl(URL base) {
		initialize(base, null, null, null);
	}

	public ParseContextImpl(URL base, Map<String, String> featrues,
			Parser<? extends Object>[] parsers, InstructionParser[] ips) {
		initialize(base, featrues, parsers, ips);
	}

	@SuppressWarnings("unchecked")
	protected void initialize(URL base, Map<String, String> featrues,
			Parser<? extends Object>[] parsers, InstructionParser[] ips) {
		resourceContext = new ResourceContextImpl(base);
		resultContext = new ResultContextImpl();
		xmlContext = new XMLContextImpl(this);
		parserHolder = new ParseHolderImpl(this,parsers, ips);
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
			this.featrues = featrues;
		} else {
			this.featrues = new HashMap<String, String>();
		}

	}

	public ParseContext createClientContext(String name){
		return new ClientContextImpl(this,name);
	}

	public InstructionParser[] getInstructionParsers() {
		return parserHolder.getInstructionParsers();
	}

	public void parseText(String source, int defaultType) {
		int type = resourceContext.getSourceType();
		resourceContext.setSourceType(defaultType);
		parse(source);
		resourceContext.setSourceType(type);
	}

	public void parse(Object source) {
		if (source instanceof Node || source instanceof String) {
			getTopChain().process(source);
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
		} else if (source instanceof URL) {
			try {
				//parse(loadXML((URL) source));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			getTopChain().process(source);
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

	public Object parseEL(String eltext) {
		return resultContext.parseEL(eltext);
	}

	public void clearPreviousText() {
		resultContext.clearPreviousText();
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
	public String toJSON() {
		return resultContext.toJSON();
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

	public int getSourceType() {
		return resourceContext.getSourceType();
	}

	public Document loadXML(URL url) throws SAXException, IOException {
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

	public void addInstructionParser(InstructionParser iparser) {
		parserHolder.addInstructionParser(iparser);
	}

	public void addNodeParser(Parser<? extends Node> iparser) {
		parserHolder.addNodeParser(iparser);
	}
	public ParseChain getTopChain() {
		return parserHolder.getTopChain();
	}





}