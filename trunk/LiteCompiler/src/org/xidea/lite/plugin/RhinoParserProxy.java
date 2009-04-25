package org.xidea.lite.plugin;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Node;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;

public class RhinoParserProxy implements Parser<Node> {

	private Scriptable base;
	private Function parse;
	private int type;

	public RhinoParserProxy(Context context, Scriptable base, Function parse,
			int type) {
		this.base = base;
		this.parse = parse;
		this.type = type;
	}

	@Override
	public void parse(ParseContext context, ParseChain chain, Node node) {
		if (type == 0 || node.getNodeType() == type) {
			Object[] args = new Object[] { context, chain, node };
			RhinoContext.call(base, parse, args);
		}else{
			chain.process(node);
		}
	}

}
