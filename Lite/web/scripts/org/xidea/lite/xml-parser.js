/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

function XMLParser(nativeJS){
	this.nativeJS = nativeJS;
	this.initialize(parseCoreNode,parseXMLNode,parseText);
}
XMLParser.prototype = new ParseContext();
