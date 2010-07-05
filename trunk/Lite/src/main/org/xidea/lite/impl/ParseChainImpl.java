package org.xidea.lite.impl;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseContext;

@SuppressWarnings("unchecked")
public class ParseChainImpl implements ParseChain {
	// private static Log log = LogFactory.getLog(ParseChainImpl.class);
	ParseChainImpl pre;
	ParseChainImpl next;
	ParseContext context;
	NodeParser parser;
	private Class<?> nodeType = Object.class;

	ParseChainImpl(ParseContext context, NodeParser<? extends Object> parser) {
		this.context = context;
		this.parser = parser;
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

	public void process(Object node) {
		if (nodeType.isInstance(node)) {
			parser.parse(node, context, next);
		} else {
			if (next != null) {
				next.process(node);
			} else {
				if (node instanceof URL) {
					try {
						node = ((URL)node).toURI();
					} catch (URISyntaxException e) {
						throw new RuntimeException(e);
					}
				}
				if (node instanceof URI) {
					try {
						URI uri = (URI)node;
						context.setCurrentURI(uri);
						Document doc = context.loadXML(uri);
						if(doc == null){
							InputStream in = context.openStream(uri);
							try{
								context.parse(in);
							}finally{
								try{
									in.close();
								}catch (Exception e) {
								}
							}
						}else{
							context.parse(doc);
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}else if (node instanceof NodeList) {
					NodeList list = (NodeList) node;
					int len = list.getLength();
					for (int i = 0; i < len; i++) {
						context.parse(list.item(i));
					}
				} else if (node instanceof NamedNodeMap) {
					NamedNodeMap list = (NamedNodeMap) node;
					int len = list.getLength();
					for (int i = 0; i < len; i++) {
						context.parse(list.item(i));
					}
				}else {
					throw new RuntimeException("找不到数据类型对应的解析器"+node);
				}
				
			}
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
