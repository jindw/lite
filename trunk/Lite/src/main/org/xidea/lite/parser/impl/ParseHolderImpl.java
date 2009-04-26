package org.xidea.lite.parser.impl;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;
import org.xidea.lite.parser.ParserHolder;

public class ParseHolderImpl implements ParserHolder {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	protected static Parser[] DEFAULT_PARSER_LIST = { new HTMLParser(),
			new CoreXMLParser(), new DefaultXMLParser(), new TextParser() };
	protected static InstructionParser[] DEFAULT_IPARSER_LIST = { ELParser.EL };
	protected ParseChainImpl topChain;
	protected InstructionParser[] ips ;

	private ParseContext context;

	@SuppressWarnings("unchecked")
	public ParseHolderImpl(ParseContext context,
			Parser<? extends Object>[] parsers, InstructionParser[] ips) {
		initialize(context, parsers, ips);
	}

	@SuppressWarnings("unchecked")
	public ParseHolderImpl(ParseContext context, ParseHolderImpl parentHolder) {
		ParseChainImpl chain = parentHolder.topChain;
		ArrayList<Parser> cs = new ArrayList<Parser>();
		do{
			cs.add(chain.parser);
			chain = chain.next;
		}while(chain!=null);
		initialize(context, cs.toArray(new Parser[cs.size()]), parentHolder.ips);
	}

	@SuppressWarnings("unchecked")
	protected void initialize(ParseContext context,
			Parser<? extends Object>[] parsers, InstructionParser[] ips) {
		this.context = context;
		this.topChain = buildTopChain(parsers ==null? DEFAULT_PARSER_LIST:parsers);
		this.ips = ips == null?DEFAULT_IPARSER_LIST:ips;
	}

	public void addInstructionParser(InstructionParser iparser) {
		int length = ips.length;
		InstructionParser[] ips2 = new InstructionParser[length + 1];
		System.arraycopy(this.ips, 0, ips2, 0, length);
		ips2[length] = iparser;
		this.ips = ips2;
	}

	public void addNodeParser(Parser<? extends Node> iparser) {
		ParseChainImpl chain = new ParseChainImpl(context, iparser);
		topChain.insertBefore(chain);
		topChain = chain;
	}

	@SuppressWarnings("unchecked")
	private ParseChainImpl buildTopChain(Parser[] parsers) {
		ParseChainImpl current = this.topChain = new ParseChainImpl(context,
				parsers[0]);
		for (int i = 1; i < parsers.length; i++) {
			ParseChainImpl chain = new ParseChainImpl(context, parsers[i]);
			chain.insertBefore(current);
			current = chain;
		}
		return topChain;
	}

	public InstructionParser[] getInstructionParsers() {
		return ips;
	}

	public ParseChain getTopChain() {
		return topChain;
	}

}