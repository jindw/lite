package org.xidea.lite.parser;


public abstract interface NodeParser<T extends Object>{
	/**
     * @public
     */
	public abstract void parse(T node,ParseContext context,ParseChain chain);
}
