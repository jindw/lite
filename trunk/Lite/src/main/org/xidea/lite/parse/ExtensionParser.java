package org.xidea.lite.parse;

import org.w3c.dom.Node;

public interface ExtensionParser extends NodeParser<Node> , TextParser{
	public void addExtension(String namespace,Object object);

}
