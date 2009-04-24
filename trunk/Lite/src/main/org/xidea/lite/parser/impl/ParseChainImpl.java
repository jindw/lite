package org.xidea.lite.parser.impl;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;

public class ParseChainImpl implements ParseChain {
	private static Log log = LogFactory.getLog(ParseChainImpl.class);
	private ParseChainImpl pre;
	private ParseChainImpl next;
	private ParseContext context;
	private Parser parser;
	private Class<?> nodeType = Object.class;

	ParseChainImpl(ParseContext context, Parser<? extends Object> parser) {
		this.context = context;
		this.parser = parser;
		try {
			Method[] methods = parser.getClass().getMethods();
			for (Method method : methods) {
				if ("parse".equals(method.getName())) {
					Class<?>[] types = method.getParameterTypes();
					if (types.length == 3 && types[0] == ParseContext.class
							&& types[1] == ParseChain.class) {
						if(nodeType.isAssignableFrom(types[2])){
							nodeType = types[2];
						}

					}
				}
			}
			// ("parse", ParseContext.class,ParseChain.class,Object.class);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void process(Object node) {
		if (next != null) {
			if (nodeType.isInstance(node)) {
				parser.parse(context, next, node);
			} else {
				next.process(node);
			}
		} else {
			parser.parse(context, next, node);
			//log.warn("unprocessed node:" + node);
		}
	}

	void remove() {

	}

	ParseChainImpl getNextSibling() {
		return next;
	}

	void insertBefore(ParseChainImpl chain) {
		if (pre != null) {
			pre.next = chain;
		}
		chain.pre = pre;
		chain.next = this;
		pre = chain;
	}

	void insertAfter(ParseChainImpl chain) {
		if (next != null) {
			next.pre = chain;
		}
		chain.next = next;
		chain.pre = this;
		next = chain;
	}
}
