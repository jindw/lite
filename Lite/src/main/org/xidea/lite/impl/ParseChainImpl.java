package org.xidea.lite.impl;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseContext;

@SuppressWarnings("unchecked")
public class ParseChainImpl extends ParseContextProxy implements ParseChain {
	private static Log log = LogFactory.getLog(ParseChainImpl.class);
	private ParseContext context;
	private ParseChainImpl pre;
	private ParseChainImpl next;
	@SuppressWarnings("rawtypes")
	private NodeParser[] parsers;
	@SuppressWarnings("rawtypes")
	private NodeParser parser; 
	private Class<? extends Object> nodeType = Object.class;
	private int index = 0;

	ParseChain getNext(){
		if(next == null && index+1<parsers.length){
			next = new ParseChainImpl(context,parsers,index+1);
		}
		return next;
	}
	ParseChainImpl(ParseContext context, NodeParser<? extends Object>[] parsers,int index) {
		super(context);
		this.context = context;
		this.parsers = parsers;
		this.index  = index;
		this.parser = parsers[index];
		try {
			// System.out.println(java.util.Arrays.asList(parser.getClass().getTypeParameters()));
			Method[] methods = parser.getClass().getMethods();
			for (Method method : methods) {
				if ("parse".equals(method.getName())) {
					Class<?>[] types = method.getParameterTypes();
					if (types.length == 3 && types[1] == ParseContext.class
							&& types[2] == ParseChain.class) {
						if (nodeType.isAssignableFrom(types[1])) {
							nodeType = types[0];
						}

					}
				}
			}
			// ("parse", ParseContext.class,ParseChain.class,Object.class);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void next(Object node) {
		ParseChain next = getNext();
		if (nodeType.isInstance(node)) {
			parser.parse(node, context, next);
		} else {
			if (next != null) {
				next.next(node);
			} else {
				log.warn("找不到相关解析器:"+node);
			}
		}

	}

	public ParseChain getPreviousChain() {
		return pre;
	}

	public void parse(Object source) {
		context.parse(source);
	}

	public List<Object> parseText(String text, int textType) {
		return context.parseText(text, textType);
	}

	public String getFeatrue(String key) {
		return context.getFeatrue(key);
	}
	public NodeParser<? extends Object>[] getNodeParsers() {
		return parsers;
	}

}
