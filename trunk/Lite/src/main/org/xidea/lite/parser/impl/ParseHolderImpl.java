package org.xidea.lite.parser.impl;

import java.util.ArrayList;

import org.xidea.lite.parser.TextParser;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.NodeParser;
import org.xidea.lite.parser.ParserHolder;

public class ParseHolderImpl implements ParserHolder {
	private static final long serialVersionUID = 1L;

	protected static NodeParser<?>[] DEFAULT_PARSER_LIST = { new HTMLNodeParser(),
			new CoreXMLNodeParser(), new DefaultXMLNodeParser(),new InputStreamNodeParser(), new TextNodeParser() };
	protected static TextParser[] DEFAULT_TEXT_PARSER_LIST = { ELParser.EL };
	protected ParseChainImpl topChain;
	protected TextParser[] ips ;

	private ParseContext context;

	public ParseHolderImpl(ParseContext context,
			NodeParser<? extends Object>[] parsers, TextParser[] ips) {
		initialize(context, parsers, ips);
	}

	@SuppressWarnings("unchecked")
	public ParseHolderImpl(ParseContext context,ParserHolder parentHolder) {
		ParseChainImpl chain = (ParseChainImpl) parentHolder.getTopChain();
		ArrayList<NodeParser> cs = new ArrayList<NodeParser>();
		do{
			cs.add(chain.parser);
			chain = chain.next;
		}while(chain!=null);
		initialize(context, cs.toArray(new NodeParser[cs.size()]), parentHolder.getTextParsers());
	}

	protected void initialize(ParseContext context,
			NodeParser<? extends Object>[] parsers, TextParser[] ips) {
		this.context = context;
		this.topChain = buildTopChain(parsers ==null? DEFAULT_PARSER_LIST:parsers);
		this.ips = ips == null?DEFAULT_TEXT_PARSER_LIST:ips;
	}

	public void addTextParser(TextParser iparser) {
		int length = ips.length;
		TextParser[] ips2 = new TextParser[length + 1];
		System.arraycopy(this.ips, 0, ips2, 0, length);
		ips2[length] = iparser;
		this.ips = ips2;
	}

	public void addNodeParser(NodeParser<? extends Object> iparser) {
		ParseChainImpl chain = new ParseChainImpl(context, iparser);
		topChain.insertBefore(chain);
		topChain = chain;
	}

	@SuppressWarnings("unchecked")
	private ParseChainImpl buildTopChain(NodeParser[] parsers) {
		ParseChainImpl current = this.topChain = new ParseChainImpl(context,
				parsers[0]);
		for (int i = 1; i < parsers.length; i++) {
			ParseChainImpl chain = new ParseChainImpl(context, parsers[i]);
			chain.insertBefore(current);
			current = chain;
		}
		return topChain;
	}

	public TextParser[] getTextParsers() {
		return ips;
	}

	public ParseChain getTopChain() {
		return topChain;
	}

}