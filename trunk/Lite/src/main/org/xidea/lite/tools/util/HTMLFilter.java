package org.xidea.lite.tools.util;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.lite.impl.ParseUtil;



public class HTMLFilter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(HTMLFilter.class);
	
	public void filterNode(Node root) throws XPathExpressionException{
		//on*,style,href,src,action
		//script/style
		String xpath="//*[local-name()='script' || local-name()='style']//@*[starts-with(local-name(),'on') or " +
				"local-name()='style' or local-name()='href' or local-name()='src' or local-name()='action'  ]";
		
		NodeList nodes = ParseUtil.selectByXPath(root, xpath);
		for (int i = 0,len = nodes.getLength(); i < len; i++) {
			Node item = nodes.item(i);
			if(item.getNodeType() == Node.ELEMENT_NODE){
				//script/style
			}else{
				Attr attr = (Attr)item;
				String localNode = attr.getLocalName();
			}
		}
	}

}
