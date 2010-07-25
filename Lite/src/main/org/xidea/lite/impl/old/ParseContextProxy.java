package org.xidea.lite.impl.old;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xidea.el.ExpressionFactory;
import org.xidea.lite.Plugin;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.ParserHolder;
import org.xidea.lite.parse.ResultContext;
import org.xidea.lite.parse.TextParser;
import org.xml.sax.SAXException;

public class ParseContextProxy implements ParserHolder, ResultContext,
		ParseConfig {
	protected ResultContext resultContext;
	protected ParserHolder parserHolder;
	protected ParseConfig config;

	protected ParseContextProxy() {
	}

	public ParseContextProxy(ParseContext parent) {
		// 需要重设 ParseChain 的context
		this.parserHolder = parent;
		this.resultContext = parent;
	}



	public URI createURI(String path) {
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


	public String allocateId() {
		return resultContext.allocateId();
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

	public void appendXA(String name, Object el) {
		resultContext.appendXA(name, el);
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

	public void appendXT(Object el) {
		resultContext.appendXT(el);
	}

	public void appendPlugin(String clazz, Object el) {
		resultContext.appendPlugin(clazz, el);
	}

	public int mark() {
		return resultContext.mark();
	}

	public List<Object> reset(int mark) {
		return resultContext.reset(mark);
	}

	public int getDepth() {
		return resultContext.getDepth();
	}

	public int getType(int offset) {
		return resultContext.getType(offset);
	}

	public boolean isReserveSpace() {
		return resultContext.isReserveSpace();
	}

	public void setReserveSpace(boolean keepSpace) {
		resultContext.setReserveSpace(keepSpace);
	}

	public Document loadXML(URI uri) throws SAXException, IOException {
		return ParseUtil.parse(uri, (ParseContext) this);
	}

	public Node transform(Node doc, Node xslt)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException,
			IOException {
		return ParseUtil.transform(doc, xslt);
	}


	public void addNodeParser(NodeParser<? extends Object> nodeParser) {
		parserHolder.addNodeParser(nodeParser);
	}

	public ParseChain getTopChain() {
		return parserHolder.getTopChain();
	}

	public List<Object> toList() {
		return resultContext.toList();
	}

//	public String toResult() {
//		return resultContext.toResult();
//	}

	/**
	 * 自定义表达式解析器
	 * 
	 * @param expressionFactory
	 */
	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		resultContext.setExpressionFactory(expressionFactory);
	}

	public Object parseEL(String eltext) {
		return resultContext.parseEL(eltext);
	}

	public void addResource(URI resource) {
		resultContext.addResource(resource);

	}

	public URI getCurrentURI() {
		return resultContext.getCurrentURI();
	}

	public Collection<URI> getResources() {
		return resultContext.getResources();
	}

	public void setCurrentURI(URI currentURI) {
		resultContext.setCurrentURI(currentURI);
	}

	public <T> T getAttribute(Object key) {
		return resultContext.getAttribute(key);
	}

	public void setAttribute(Object key, Object value) {
		resultContext.setAttribute(key, value);
	}

	public int getTextType() {
		return resultContext.getTextType();
	}

	public void setTextType(int textType) {
		resultContext.setTextType(textType);
	}

	public String getDecotatorPage(String path) {
		if (config != null) {
			return config.getDecotatorPage(path);
		}
		return null;
	}

	public Map<String, String> getFeatrueMap(String path) {
		if (config != null) {
			return config.getFeatrueMap(path);
		}
		return null;
	}

	public NodeParser<? extends Object>[] getNodeParsers(String path) {
		return ((ParseConfigImpl)config).getNodeParsers(path);
	}

	public URI getRoot() {
		return config.getRoot();
	}

	public void addExtension(String namespace, String packageName) {
		parserHolder.addExtension(namespace, packageName);

	}

	public InputStream openStream(URI uri) {
		return ParseUtil.openStream(uri);
	}

	public TextParser[] getTextParsers() {
		return parserHolder.getTextParsers();
	}

	public void addTextParser(TextParser textParser) {
		parserHolder.addTextParser(textParser);
		
	}

	public Map<String, List<String>> getExtensions(String path) {
		return null;
	}
}
