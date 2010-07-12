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
	config = config || new ParseConfig();
	this.path = path;
	this.currentURI = path?new URI(path):config.root
	this.featrueMap = config.getFeatrueMap(path);
	this.initialize(config);
}

ParseContext.prototype = {
	/**
	 * 初始化上下文
	 * @arguments 链顶插入的解析器列表（第一个元素为初始化后的链顶解析器，以后类推）
	 */
	initialize:function(config){
    	this.config = config;
    	this.parserList = config.getNodeParsers(this.path);
	    this.result = new ResultContext();
	    this.topChain = buildTopChain(this);
	},
	parseText:function(source, textType) {
		switch(textType){
		case XML_ATTRIBUTE_TYPE :
	    case XML_TEXT_TYPE :
	    case EL_TYPE :
	        break;
	    default:
			$log.error("未知编码模式："+textType)
			throw new Error();
		}
		
		var mark = this.mark();
		var oldType = this.getTextType();
		this.setTextType(textType);
		this.parse(source);
		this.setTextType(oldType);
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
		var type = source.nodeType;
		if(type>0){//xml
		}else{//text
			if(typeof source != 'string'){
				//NodeList
				if(source instanceof URI){
					source = this.loadXML(source.path);
				}else{
					var len = source.length;
					if(len >= 0 && source.item){//NodeList
						for(var i = 0;i<len;i++){
							this.topChain.next(source.item(i));
						}
						return;
					}
				}
			}
		}
		this.topChain.next(source);
	},
    createURI:function(path) {
    	$log.error(path,this.currentURI,this.config.root)
    	return URI.create(path,this.currentURI,this.config.root)
    },
    openStream:function(uri){
    	//only for java
    	return Packages.org.xidea.lite.impl.ParseUtil.openStream(uri)
    },
    loadXML:function(path){
    	if(/^[\s\ufeff]*</.test(path)){
    		//this.currentURI = "data:text/xml,"+encodeURIComponent(path)
    		return loadXML(path)
    	}else{
    		//$log.info("loadXML",path)
    		this.currentURI = this.createURI(path);
    		return loadXML(this.currentURI)
    	}
    	
    },
    selectNodes:selectNodes,
    toString:function(){
    	return this.toCode();
    }
}
var rm = ResultContext.prototype;
for(var n in rm){
	if(rm[n] instanceof Function){
		ParseContext.prototype[n] = buildWrapper(n);
	}
}
function buildWrapper(n){
	return function(){
		var result = this.result;
		return result[n].apply(result,arguments)
	}
}