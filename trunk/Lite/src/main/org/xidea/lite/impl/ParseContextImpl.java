package org.xidea.lite.impl;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.lite.parse.ExtensionParser;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.TextParser;

/**
 * 不要较差调用，交叉调用，用this代替，确保继承安全 取消final 之后，容易引发一个短路bug，等到发现之后再修复吧。
 * 
 * @author jindw
 */
public class ParseContextImpl extends ParseContextProxy implements ParseContext {
	private static final long serialVersionUID = 1L;
	private static NodeParser<?>[] DEFAULT_PARSER_LIST = { new TextNodeParser(),new DefaultXMLNodeParser(),null};
	
	private ParseChain topChain;
	private final ExtensionParser  extensionParser;
	//尾部优先原则
	private NodeParser<? extends Object>[] nodeParsers;
	private TextParser[] textParsers;
//	private Object currentNode = null;
//	private Object contextInfo = new Object(){//移动到extensionParser?
//		public String toString(){
//			String pos = ParseUtil.getNodePosition(currentNode instanceof Node?(Node)currentNode:extensionParser.getCurrentNode());
//			if(pos == null){
//				pos= String.valueOf(getCurrentURI());
//			}
//			return pos;
//		}
//	};
	public ParseContextImpl(ParseConfig config, String path) {
		super(config,config.getFeatureMap(path));
		this.extensionParser = new ExtensionParserImpl();
		this.nodeParsers = DEFAULT_PARSER_LIST.clone();
		nodeParsers[2] = extensionParser;
		textParsers = new TextParser[]{extensionParser};
	}

	public ParseContext createNew() {
		return new ParseContextImpl(this);
	}
	private ParseContextImpl(ParseContextProxy parent) {
		super(parent);
		this.setCurrentURI(parent.getCurrentURI());
		this.resultContext = new ResultContextImpl();
		// 需要重设 ParseChain 的context
		this.textParsers = parent.getTextParsers();
		this.nodeParsers = parent.getTopChain().getNodeParsers();
		ExtensionParser ep = null;
		for(NodeParser<?> n :nodeParsers){
			if(n instanceof ExtensionParser){
				ep = (ExtensionParser)n;
			}
		}
		this.extensionParser = ep;
	}
//	public ParseContext create(ParseContext parent){
//		return new ParseContextImpl(parent);
//	}
	public final ParseChain getTopChain() {
		if(topChain == null){//nodeParsers != topChain.getNodeParsers()
			topChain = new ParseChainImpl(this, nodeParsers, nodeParsers.length-1);
		}
		return topChain;
	}

	public final void addExtension(String namespace, Object packageName) {
		extensionParser.addExtension(namespace, packageName);
	}
	
	public ExtensionParser getExtensionParser() {
		return extensionParser;
	}

	public void addNodeParser(NodeParser<? extends Object> nodeParser) {
		int length = this.nodeParsers.length;
		NodeParser<?>[] ips2 = new NodeParser<?>[length + 1];
		System.arraycopy(this.nodeParsers, 0, ips2, 0, length);
		ips2[length] = nodeParser;
		this.nodeParsers = ips2;
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

	public void parse(Object node) {
//		currentNode = node;
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
				} catch (RuntimeException e) {
					throw (RuntimeException)e;
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
		int oldType = this.getTextType();
		int mark = this.mark();
		List<Object> result;
		try {
			this.textType = defaultType;
			parse(text);
		} finally {
			this.textType = oldType;
			result = this.reset(mark);
		}
		return result;
	}

}