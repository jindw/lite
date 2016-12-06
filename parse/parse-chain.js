/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

exports.buildTopChain=buildTopChain;

/**
 * 解析链对象
 */
function buildTopChain(context){
	function TopChain(){
	}
	TopChain.prototype = context;
	var pt = TopChain.prototype = new TopChain();
	pt.index = context._nodeParsers.length;
	pt.subIndex = -1;
	pt.getSubChain = getSubChain;
	pt.next = doNext;
	pt.constructor = TopChain;
	return new TopChain();
}

function doNext(node){
	//console.info(typeof node,node&& node.tagName)
	if (this.subIndex > 0) {
		var next = this.getSubChain(this.subIndex - 1);
	} else {
		next = this.nextChain||buildNext(this,this.index-1);
	}
	doParse(node,next);
}
function doParse(node,chain){
	//try{
		var parser = chain._nodeParsers[chain.index];
		if(parser == null){
			console.error('解析栈异常',parser,chain.index,chain._nodeParsers);
		}
		parser(node,chain,chain);
	//}catch(e){
	//	console.error("解析器执行异常："+parser,e)
	//	throw e;
	//}
}
function getSubChain(subIndex){
	if (this.subChains == null) {
		this.subChains =[];
	}
	var i = this.subChains.length;
	for (;i <= subIndex; i++) {
		var subChain = new this.constructor();
		subChain.index = this.index
		//subChain.nodeType = this.nodeType;
		subChain.subIndex = i;
		subChain.subChains = this.subChains;
		this.subChains.push(subChain);
	}
	if (subChain == null) {
		subChain = this.subChains[subIndex];
	}
	return subChain;
}
function buildNext(thiz,index){
	if(index>=0){
		var n = new thiz.constructor();
		n.index = index
		return thiz.nextChain = n;
	}
	return null;
}

