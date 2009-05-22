package org.xidea.lite.parser.impl;

import java.lang.reflect.Method;

import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;
@SuppressWarnings("unchecked")
public class ParseChainImpl implements ParseChain {
	//private static Log log = LogFactory.getLog(ParseChainImpl.class);
	ParseChainImpl pre;
	ParseChainImpl next;
	ParseContext context;
	Parser parser;
	private Class<?> nodeType = Object.class;

	ParseChainImpl(ParseContext context, Parser<? extends Object> parser) {
		this.context = context;
		this.parser = parser;
		try {
			//System.out.println(java.util.Arrays.asList(parser.getClass().getTypeParameters()));
			Method[] methods = parser.getClass().getMethods();
			for (Method method : methods) {
				if ("parse".equals(method.getName())) {
					Class<?>[] types = method.getParameterTypes();
					if (types.length == 3 && types[1] == ParseContext.class
							&& types[2] == ParseChain.class) {
						if(nodeType.isAssignableFrom(types[1])){
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

	public void process(Object node) {
		if (next != null) {
			if (nodeType.isInstance(node)) {
				parser.parse(node, context, next);
			} else {
				next.process(node);
			}
		} else {
			parser.parse(node, context, next);
		}
	}


	void insertBefore(ParseChainImpl chain) {
		if (pre != null) {
			pre.next = chain;
		}
		chain.pre = pre;
		chain.next = this;
		pre = chain;
	}

}
