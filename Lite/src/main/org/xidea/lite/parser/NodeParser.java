package org.xidea.lite.parser;


public abstract interface NodeParser<T extends Object>{
	/**
     * @public
     * @return <Array> result
     */
	public abstract void parse(ParseContext context,ParseChain chain,T node);
}
