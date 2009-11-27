/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
/**
 * XML 模板解释引擎，集成自 ParseContext。
 * 该引擎带有parseCoreNode,parseXMLNode,parseText三个节点解析器实现
 */
function XMLParser(nativeJS,base){
	this.nativeJS = nativeJS;
	this.currentURI = base;
	this.initialize(parseCoreNode,parseXMLNode,parseText);
}
XMLParser.prototype = new ParseContext();
