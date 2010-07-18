package org.xidea.lite.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xidea.el.ExpressionFactory;
import org.xidea.lite.Plugin;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.ResultContext;
import org.xidea.lite.parse.TextParser;
import org.xml.sax.SAXException;

abstract public class ParseContextProxy implements ParseContext {
	private static Log log = LogFactory.getLog(ParseContextProxy.class);
	protected static NodeParser<?>[] DEFAULT_PARSER_LIST = {new ExtensionParserImpl(), new DefaultXMLNodeParser(), new TextNodeParser() };
	
	protected ResultContext resultContext;
	protected ParseConfig config;

	protected ParseChain topChain;
	protected NodeParser<? extends Object>[] nodeParsers = DEFAULT_PARSER_LIST;
	private TextParser[] textParsers;
	
	protected ParseContextProxy(ParseConfig config) {
		this.config = config;
		nodeParsers = nodeParsers.clone();
		ExtensionParserImpl ext = new ExtensionParserImpl();
		nodeParsers[0] = ext;
		textParsers = new TextParser[]{ext};
	}

	public ParseContextProxy(ParseContext parent) {
		// 需要重设 ParseChain 的context
		this.config = parent;
		this.resultContext = parent;
		this.textParsers = parent.getTextParsers();
		nodeParsers = nodeParsers.clone();
		nodeParsers[0] = new ExtensionParserImpl();
	}


	public final URI createURI(String path) {
		try {
			// TODO
			URI parent = this.getCurrentURI();
			if (parent == null) {
				parent = config.getRoot();
			}
			if (path.startsWith("/")) {
				if (parent == null
						|| parent.toString().startsWith(
								config.getRoot().toString())) {
					String prefix = config.getRoot().getRawPath();
					if (prefix != null) {
						int p = prefix.lastIndexOf('/');
						if (p > 0) {
							path = prefix.substring(0, p) + path;
						}
					}
				}
			}
			return parent.resolve(path);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public final InputStream openStream(URI uri) {
		return ParseUtil.openStream(uri);
	}


	public final Document loadXML(URI uri) throws SAXException, IOException {
		return ParseUtil.parse(uri, (ParseContext) this);
	}

	public final String getDecotatorPage(String path) {
		if (config != null) {
			return config.getDecotatorPage(path);
		}
		return null;
	}

	public final Map<String, String> getFeatrueMap(String path) {
		if (config != null) {
			return config.getFeatrueMap(path);
		}
		return null;
	}


	public final URI getRoot() {
		return config.getRoot();
	}

	public Map<String, List<String>> getExtensions(String path) {
		return config.getExtensions(path);
	}

	public final ParseChain getTopChain() {
		if(topChain == null || nodeParsers != topChain.getNodeParsers()){
			topChain = new ParseChainImpl(this, nodeParsers, 0);
		}
		return topChain;
	}

	public void addNodeParser(NodeParser<? extends Object> nodeParser) {
		
	}
	public final TextParser[] getTextParsers() {
		return textParsers;
	}
	public void addTextParser(TextParser iparser) {
		int length = textParsers.length;
		TextParser[] ips2 = new TextParser[length + 1];
		System.arraycopy(this.textParsers, 0, ips2, 0, length);
		ips2[length] = iparser;
		this.textParsers = ips2;
	}

	public final void addExtension(String namespace, String packageName) {
		//parserHolder.addExtension(namespace, packageName);
	}


	public final String allocateId() {
		return resultContext.allocateId();
	}

	public final void append(String text) {
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

	public final int appendEnd() {
		return resultContext.appendEnd();
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

	public final void appendPlugin(Class<? extends Plugin> clazz, Object el) {
		resultContext.appendPlugin(clazz, el);
	}

	@SuppressWarnings("unchecked")
	public final void appendPlugin(String clazz, Object el) {
		try {
			resultContext.appendPlugin((Class<? extends Plugin>) Class.forName(clazz), el);
		} catch (ClassNotFoundException e) {
			log.error(e);
		}
	}

	public final int mark() {
		return resultContext.mark();
	}

	public final List<Object> reset(int mark) {
		return resultContext.reset(mark);
	}

	public final int getDepth() {
		return resultContext.getDepth();
	}

	public final int getType(int offset) {
		return resultContext.getType(offset);
	}

	public final boolean isReserveSpace() {
		return resultContext.isReserveSpace();
	}

	public final void setReserveSpace(boolean keepSpace) {
		resultContext.setReserveSpace(keepSpace);
	}

	public final List<Object> toList() {
		return resultContext.toList();
	}

	public final String toCode() {
		return resultContext.toCode();
	}

	/**
	 * 自定义表达式解析器
	 * 
	 * @param expressionFactory
	 */
	public final void setExpressionFactory(ExpressionFactory expressionFactory) {
		resultContext.setExpressionFactory(expressionFactory);
	}

	public final Object parseEL(String eltext) {
		return resultContext.parseEL(eltext);
	}

	public final void addResource(URI resource) {
		resultContext.addResource(resource);

	}

	public final URI getCurrentURI() {
		return resultContext.getCurrentURI();
	}

	public final Collection<URI> getResources() {
		return resultContext.getResources();
	}

	public final void setCurrentURI(URI currentURI) {
		resultContext.setCurrentURI(currentURI);
	}

	public final <T> T getAttribute(Object key) {
		return resultContext.getAttribute(key);
	}

	public final void setAttribute(Object key, Object value) {
		resultContext.setAttribute(key, value);
	}

	public final int getTextType() {
		return resultContext.getTextType();
	}

	public final void setTextType(int textType) {
		resultContext.setTextType(textType);
	}

}
