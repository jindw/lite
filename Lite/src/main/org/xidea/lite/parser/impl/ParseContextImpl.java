package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Plugin;
//import org.xidea.lite.parser.ResultItem;
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

/**
 * 不要较差调用，交叉调用，用this代替，确保继承安全
 * 取消final 之后，容易引发一个短路bug，等到发现之后再修复吧。
 * @author jindw
 */
public class ParseContextImpl implements ParseContext {
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(ParseContextImpl.class);
	protected ResourceContext resourceContext;
	protected XMLContext xmlContext;
	protected ResultContext resultContext;
	protected ParserHolder parserHolder;
	protected final Map<String, String> featrueMap;
	private ExpressionFactory expressionFactory = ExpressionFactoryImpl.getInstance();
	private ResultTranslator translator;
	private URI currentURI;
	private int textType = 0;

	private HashMap<Object, Object> attributeMap = new HashMap<Object, Object>();
	private HashSet<URI> resources = new HashSet<URI>();

	
	protected ParseContextImpl() {
		featrueMap = new HashMap<String, String>();
	}

	public ParseContextImpl(URI base, Map<String, String> featrues,
			NodeParser<? extends Object>[] parsers, TextParser[] ips) {
		this();
		initialize(base, featrues, parsers, ips);
	}

	public ParseContextImpl(ParseContext parent, 
			ResultTranslator translator) {
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
		this.resultContext = new ResultContextImpl(this);
		this.featrueMap = new HashMap<String, String>(parent.getFeatrueMap());
		this.setResultTranslator(translator);
	}


	@SuppressWarnings("unchecked")
	protected void initialize(URI base, Map<String, String> featrues,
			NodeParser<? extends Object>[] parsers, TextParser[] ips) {
		resourceContext = new ResourceContextImpl(base);
		resultContext = new ResultContextImpl(this);
		xmlContext = new XMLContextImpl(this);
		parserHolder = new ParseHolderImpl(this, parsers, ips);
		initializeFeatrues(featrues);
	}

	protected void initializeFeatrues(Map<String, String> newFeatrues) {
		if (newFeatrues != null) {
			String v = newFeatrues.get("compress");
			if (v != null) {
				xmlContext.setCompress("true".equalsIgnoreCase(v));
			}
			v = newFeatrues.get("reserveSpace");
			if (v != null) {
				xmlContext.setReserveSpace("true".equalsIgnoreCase(v));
			}
			v = newFeatrues.get("format");
			if (v != null) {
				xmlContext.setFormat("true".equalsIgnoreCase(v));
			}
			featrueMap.clear();
			featrueMap.putAll(newFeatrues);
		}
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}

	public Object parseEL(String expression) {
		return expressionFactory.parse(expression);
	}
	public List<Object> parseText(String text, int defaultType) {
		int type = this.getTextType();
		int mark = this.mark();
		List<Object> result;
		try {
			this.setTextType(defaultType);
			parse(text);
		} finally {
			this.setTextType(type);
			result = this.reset(mark);
		}
		return result;
	}

	public void parse(Object source) {
		getTopChain().process(source);
	}

	public TextParser[] getTextParsers() {
		return parserHolder.getTextParsers();
	}


	public String getFeatrue(String key) {
		return featrueMap.get(key);
	}

	public Map<String, String> getFeatrueMap() {
		return featrueMap;
	}

	// delegate methods...


	public URI createURI(String file, URI parentURI) {
		return resourceContext.createURI(file, parentURI);
	}



	public InputStream openInputStream(URI uri) {
		return resourceContext.openInputStream(uri);
	}

	public String addGlobalObject(Class<? extends Object> impl, String key) {
		return resultContext.addGlobalObject(impl, key);
	}
	public String allocateId(){
		return resultContext.allocateId();
	}

	public void append(String text) {
		resultContext.append(text);
	}
//	public final void append(ResultItem text) {
//		resultContext.append(text);
//	}

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

	public int appendEnd() {
		return resultContext.appendEnd();
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

	public void appendPlugin(Class<? extends Plugin> clazz, Object el) {
		resultContext.appendPlugin(clazz, el);
	}

	public int mark() {
		return resultContext.mark();
	}


	public List<Object> reset(int mark) {
		return resultContext.reset(mark);
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

	public Document loadXML(URI url) throws SAXException, IOException {
		return xmlContext.loadXML(url);
	}

	public DocumentFragment selectNodes(Node doc, String xpath)
			throws XPathExpressionException {
		return xmlContext.selectNodes(doc, xpath);
	}

	public Node transform(URI parentURI, Node doc, String xslt)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException,
			IOException {
		return xmlContext.transform(parentURI, doc, xslt);
	}

	public void addTextParser(TextParser iparser) {
		parserHolder.addTextParser(iparser);
	}

	public void addNodeParser(NodeParser<? extends Node> iparser) {
		parserHolder.addNodeParser(iparser);
	}

	public ParseChain getTopChain() {
		return parserHolder.getTopChain();
	}


	public void setAttribute(Object key, Object value) {
		this.attributeMap.put(key, value);
	}

	public Object getAttribute(Object key) {
		return this.attributeMap.get(key);
	}

	public int getTextType() {
		return textType;
	}

	public void setTextType(int textType) {
		this.textType = textType;
	}

	public URI getCurrentURI() {
		return currentURI;
	}

	public Set<URI> getResources() {
		return resources;
	}

	public void addResource(URI resource) {
		resources.add(resource);
	}

	public void setCurrentURI(URI currentURI) {
		if (currentURI != null) {
			resources.add(currentURI);
		}
		this.currentURI = currentURI;
	}


	public void setResultTranslator(ResultTranslator translator) {
		Collection<String> featrueKeys = featrueMap.keySet();
		Collection<String> support = Collections.emptyList();
		try{
			support = translator.getSupportFeatrues();
		}catch (Exception e) {
			log.warn("未指定支持的特征，设空处理",e);
		}
		if (support == null) {
			featrueMap.clear();
		}else{
			for (String key : featrueKeys) {
				if (!support.contains(key)) {
					featrueMap.remove(key);
				}
			}
		}
		this.translator = translator;
	}

	public List<Object> toList() {
		return resultContext.toList();
	}

	public String toCode() {
		if(translator==null){
			return JSONEncoder.encode(this.toList());
		}else{
			return translator.translate(this);
		}
	}


}