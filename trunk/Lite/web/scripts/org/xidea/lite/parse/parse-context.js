/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
//add as default
/**
 * 模板解析上下文对象实现
 */
function ParseContext(config,path){
	this.initialize(config);
	this.featrueMap = config.getFeatrueMap(path);
}

ParseContext.prototype = {
	/**
	 * 初始化上下文
	 * @arguments 链顶插入的解析器列表（第一个元素为初始化后的链顶解析器，以后类推）
	 */
	initialize:function(config){
    	this.config = config;
    	this.parserList = [];
	    this.result = new ResultContext();
	    this.topChain = new ParseChain(this);
	},
	textType:0,
	parseText:function(source, textType) {
		var mark = this.mark();
		var oldType = this.textType;
		this.parse(source);
		this.textType = oldType;
		var result = this.reset(mark);
		return result;
	},
    /**
     * 调用解析链顶解析器解析源码对象
     * @param 文本源代码内容或xml源代码文档对象。
     * @public
     * @abstract
     */
	parse:function(source) {
		if(source instanceof URI){
			source = this.loadXML(source.path);
		}
		this.topChain.next(source);
	},
    createURI:function(path,parentURI) {
		return new URI(path||'',(parentURI||this.currentURI || ''));
    },
    loadXML:function(path){
    	if(/^[\s\ufeff]*</.test(path)){
    		//this.currentURI = "data:text/xml,"+encodeURIComponent(path)
    	}else{
    		this.currentURI = path.replace(/#.*/,'');
    	}
    	return loadXML(path)
    },
    selectNodes:selectNodes
}
var rm = ResultContext.prototype;
for(var n in rm){
	if(rm[n] instanceof Function){
		ParseContext[n] = buildWrapper(n);
	}
}
function buildWrapper(n){
	return function(){
		var result = this.result;
		result[n].apply(result,arguments)
	}
}