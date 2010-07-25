package org.xidea.lite.parse;

import org.w3c.dom.Node;

public interface ExtensionParser extends NodeParser<Node> , TextParser{
	public void addExtensionPackage(String namespace,String packageName);
	public void addExtensionObject(String namespace,Object object);

}
