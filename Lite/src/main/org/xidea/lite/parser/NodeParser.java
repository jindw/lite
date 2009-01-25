package org.xidea.lite.parser;

import org.w3c.dom.Node;

public interface NodeParser {
	public static final Object[] END = new Object[0];
	/**
	 * 
	 * @param node
	 * @param context
	 * @return 如果返回 null，说明处理结束了
	 */
	public Node parseNode(Node node, ParseContext context);
}
