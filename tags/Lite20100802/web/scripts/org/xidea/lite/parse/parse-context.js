/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var defaultBase = new URI("lite:///");
//add as default
/**
 * 模板解析上下文对象实现
 */
function ParseContext(config,path){
	config = config || new ParseConfig();
    this.config = config;
	this.path = path;
//	this.currentURI = defaultBase;
	this.featrueMap = config.getFeatrueMap(path);
	this.initialize(config);
}
function parseExtension(node,context,chain){//extension
	return context.extensionParser.parse(node,context,chain);
}
function parseText2(node,context,chain){
	return parseText(node,context,chain,context.textParsers)
}
ParseContext.prototype = {
	/**
	 * 初始化上下文
	 * @arguments 链顶插入的解析器列表（第一个元素为初始化后的链顶解析器，以后类推）
	 */
	initialize:function(config){
		var extensions = config.getExtensions(this.path);
		var extensionParser = new ExtensionParser();
		for(var len = extensions.length,i=0;i<len;i++){
			var ext = extensions[i];
			var impl = ext['package'];
			if(/[\\\/]/.test(impl)){
				
			}
			extensionParser.addExtensionPackage(ext.namespace,impl)
		}
    	this.nodeParsers = [parseExtension,parseDefaultXMLNode,parseText2];
    	this.textParsers = [extensionParser];
    	this.extensionParser = extensionParser;
	    this.result = new ResultContext();
	    this.topChain = buildTopChain(this);
	},
	parseText:function(source, textType) {
		switch(textType){
		case XA_TYPE :
	    case XT_TYPE :
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
					source = this.loadXML(source);
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
    	//$log.error(path,this.currentURI,this.config.root)
    	var cu = this.currentURI;
    	if(cu){
    		if(cu.scheme != 'data'){
    			return cu.resolve(path);
    		}
    	}
    	path= path.replace(/^[\\\/]/,'./');
    	//$log.warn(defaultBase+'',path,defaultBase.resolve(path)+'',defaultBase.authority)
    	return defaultBase.resolve(path);
    	
    },
    getCurrentURI:function(){
    	return this.currentURI;
    },
    setCurrentURI:function(uri){
    	this.currentURI = new URI(uri);
    },
    openStream:function(uri){
    	//only for java
    	if(uri.scheme == 'lite'){
    		var path = uri.path+(uri.query||'');
    		path = path.replace(/^\//,'./')
    		uri = this.config.root.resolve(path);
    	}
    	return Packages.org.xidea.lite.impl.ParseUtil.openStream(uri)
    },
    createNew:function(){
    	return new ParseContext(this.config,this.currentURI);
    },
    loadXML:function(path){
    	if(!(path instanceof URI)){
    		path = new URI(path)
    	}
    	this.currentURI = path;
    	return loadXML(this.currentURI,this.config.root)
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