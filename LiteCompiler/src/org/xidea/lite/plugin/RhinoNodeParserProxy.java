package org.xidea.lite.plugin;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Node;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.NodeParser;

public class RhinoNodeParserProxy implements NodeParser<Node> {

	private Scriptable base;
	private Function parse;
	private int type;

	public RhinoNodeParserProxy(Context context, Scriptable base, Function parse,
			int type) {
		this.base = base;
		this.parse = parse;
		this.type = type;
	}

	@Override
	public void parse(Node node,ParseContext context, ParseChain chain) {
		if (type == 0 || node.getNodeType() == type) {
			Object[] args = new Object[] {node, context, chain };
			RhinoContext.call(base, parse, args);
		}else{
			chain.process(node);
		}
	}

}
