package org.xidea.lite.impl;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.TextParser;

public class ParseChainImpl extends ParseContextProxy implements ParseChain {
	private static Log log = LogFactory.getLog(ParseChainImpl.class);
	private ParseContextProxy context;
	private ParseChainImpl next;
	private NodeParser<?>[] parsers;

	@SuppressWarnings("unchecked")
	private NodeParser parser;
	private Class<?> nodeType = Object.class;
	private int index = 0;
	private int subIndex = -1;
	private List<ParseChainImpl> subChains = null;

	ParseChainImpl getNext() {
		if (next == null && index > 0) {
			next = new ParseChainImpl(context, parsers, index - 1);
			next.initialize();
		}
		return next;
	}

	public ParseChain getSubChain(int subIndex) {
		if (subChains == null) {
			subChains = new ArrayList<ParseChainImpl>();
		}
		int i = subChains.size();
		ParseChainImpl subChain =null;
		for (;i <= subIndex; i++) {
			subChain = new ParseChainImpl(context, parsers, index);
			subChain.nodeType = this.nodeType;
			subChain.subIndex = i;
			subChain.subChains = subChains;
			subChains.add(subChain);
		}
		if (subChain == null) {
			subChain = subChains.get(subIndex);
		}
		return subChain;
	}

	public void next(Object node) {
		ParseChainImpl next;
		if (subIndex > 0) {
			next = (ParseChainImpl)getSubChain(this.subIndex - 1);
		} else {
			next = getNext();
		}
		if (next != null) {
			next.doParse(node);
		} else {
			log.warn("找不到相关解析器:" + node);
		}
	}
	@SuppressWarnings("unchecked")
	public void doParse(Object node){
		if (nodeType.isInstance(node)) {
			parser.parse(node, context, this);
		} else {
			next(node);
			
		}
	}
	public int getSubIndex() {
		return subIndex;
	}

	private ParseChainImpl(ParseContextProxy context,
			NodeParser<? extends Object>[] parsers, int index) {
		super(context);
		this.context = context;
		this.setCurrentURI(context.getCurrentURI());
		this.parsers = parsers;
		this.index = index;
		if(parsers.length>index){
			this.parser = parsers[index];
		}
	}

	static ParseChainImpl createTop(ParseContextProxy context,
			NodeParser<? extends Object>[] parsers) {
		return new ParseChainImpl(context, parsers, parsers.length);
	}

	private void initialize() {
		Method[] methods = this.parser.getClass().getMethods();
		for (Method method : methods) {
			if ("parse".equals(method.getName())) {
				Class<?>[] types = method.getParameterTypes();
				if (types.length == 3 && types[1] == ParseContext.class
						&& types[2] == ParseChain.class) {
					if (this.nodeType.isAssignableFrom(types[1])) {
						this.nodeType = types[0];
					}

				}
			}
		}
	}


	/* 覆盖代理 */
	@Override
	public URI getCurrentURI() {
		return context.getCurrentURI();
	}

	public int getTextType() {
		return context.getTextType();
	}

//	@Override
//	public boolean isReserveSpace() {
//		return context.isReserveSpace();
//	}

	@Override
	public void setCurrentURI(URI currentURI) {
		context.setCurrentURI(currentURI);
	}

//	@Override
//	public void setReserveSpace(boolean keepSpace) {
//		context.setReserveSpace(keepSpace);
//	}

	// @Override
	// public void setTextType(int textType) {
	// context.setTextType(textType);
	// }
	/* 支持代理 */
	public void parse(Object source) {
		context.parse(source);
	}

	public List<Object> parseText(String text, int textType) {
		return context.parseText(text, textType);
	}

	public NodeParser<? extends Object>[] getNodeParsers() {
		return parsers;
	}

	public TextParser[] getTextParsers() {
		return context.getTextParsers();
	}

	public void addTextParser(TextParser textParser) {
		context.addTextParser(textParser);
	}

	public void addExtension(String namespace, Object packageObject) {
		context.addExtension(namespace, packageObject);

	}

	public void addNodeParser(NodeParser<? extends Object> nodeParser) {
		context.addNodeParser(nodeParser);

	}

	public ParseChain getTopChain() {
		return context.getTopChain();
	}
	// public ParseContext createNew() {
	// return context.createNew();
	// }

}
