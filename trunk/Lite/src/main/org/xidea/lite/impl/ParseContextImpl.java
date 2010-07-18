package org.xidea.lite.impl;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;

/**
 * 不要较差调用，交叉调用，用this代替，确保继承安全 取消final 之后，容易引发一个短路bug，等到发现之后再修复吧。
 * 
 * @author jindw
 */
public class ParseContextImpl extends ParseContextProxy implements ParseContext {
	private static final long serialVersionUID = 1L;

	protected final Map<String, String> featrueMap;

	public ParseContextImpl(ParseConfig config, String path) {
		super(config);
		featrueMap = new HashMap<String, String>();
		if (config != null && path != null) {
			Map<String, String> f = config.getFeatrueMap(path);
			if (f != null) {
				this.featrueMap.putAll(f);
			}
			// super.nodeParsers = config.getNodeParsers(path);
		}
		this.resultContext = new ResultContextImpl(this);
	}

	public ParseContextImpl(ParseContext parent) {
		super(parent);
		// 需要重设 ParseChain 的context
		this.resultContext = new ResultContextImpl(this);
		if (parent instanceof ParseContextImpl) {
			this.featrueMap = ((ParseContextImpl) parent).getFeatrueMap();
		} else {
			this.featrueMap = new HashMap<String, String>();
		}
	}

	public String getFeatrue(String key) {
		return featrueMap.get(key);
	}

	/**
	 * 获得特征表的直接引用，外部的修改也将直接影响解析上下文的特征表
	 * 
	 * @return
	 */
	protected Map<String, String> getFeatrueMap() {
		return featrueMap;
	}

	public void parse(Object node) {
		ParseChain topChain = getTopChain();
		if (node instanceof Node || node instanceof String) {
			topChain.next(node);
		} else {
			if (node instanceof URL) {
				try {
					node = ((URL) node).toURI();
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
			}
			if (node instanceof URI) {
				try {
					URI uri = (URI) node;
					this.setCurrentURI(uri);
					Document doc = this.loadXML(uri);
					if (doc == null) {
						InputStream in = this.openStream(uri);
						try {
							topChain.next(in);
						} finally {
							try {
								in.close();
							} catch (Exception e) {
							}
						}
					} else {
						topChain.next(doc);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else if (node instanceof NodeList) {
				NodeList list = (NodeList) node;
				int len = list.getLength();
				for (int i = 0; i < len; i++) {
					topChain.next(list.item(i));
				}
			} else if (node instanceof NamedNodeMap) {
				NamedNodeMap list = (NamedNodeMap) node;
				int len = list.getLength();
				for (int i = 0; i < len; i++) {
					topChain.next(list.item(i));
				}
			} else {
				topChain.next(node);
			}
		}

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
}