package org.xidea.lite.parse;

public interface ParseChain {
	public void next(Object node);
	public ParseChain getSubChain(int index);
	public int getSubIndex();
	public NodeParser<? extends Object>[] getNodeParsers();
}
