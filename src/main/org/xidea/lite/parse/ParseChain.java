package org.xidea.lite.parse;

public interface ParseChain {
	public void next(Object node);
	public ParseChain getPreviousChain();
	public NodeParser<? extends Object>[] getNodeParsers();
}
