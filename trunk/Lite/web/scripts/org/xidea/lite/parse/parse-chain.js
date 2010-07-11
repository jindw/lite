/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
/**
 * 解析链对象
 */
function buildTopChain(context){
	function TopChain(){
	}
	addChainAddon(TopChain,context);
	return new TopChain();
}
function addChainAddon(TopChain,context){
	TopChain.prototype = context;
	var pt = TopChain.prototype = new TopChain();
	pt.index = 0;
	pt.next = doNext;
	pt.buildNext = buildNext;
	pt.constructor = TopChain;
}
function doNext(node){
	var parser = this.parserList[this.index];
	var n = this.nextChain||this.buildNext();
	parser(node,this,n);
}
function buildNext(){
	var index = (this.index||0) +1;
	if(this.parserList.length>index){
		var n = new this.constructor();
		n.index = index
		return this.nextChain = n;
	}
	return null;
}