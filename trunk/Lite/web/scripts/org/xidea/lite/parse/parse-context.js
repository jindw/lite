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
	this._path = path;
	this._currentURI = defaultBase;
	this._featureMap = config.getFeatureMap(path);
    this._config = config;
    this._textType=0;
	this._attributeMap = [[],[]]
    this._result = new ResultContext();
	this._context = this;
	initializeParser(this,config.getExtensions(path));
}
/**
 * 初始化上下文
 * @arguments 链顶插入的解析器列表（第一个元素为初始化后的链顶解析器，以后类推）
 */
function initializeParser(context,extensions){
	var extensionParser = new ExtensionParser();
	for(var len = extensions.length,i=0;i<len;i++){
		var ext = extensions[i];
		var impl = ext['package'];
//		if(/[\\\/]/.test(impl)){
//		}
		extensionParser.addExtension(ext.namespace,impl)
	}
	context._nodeParsers = [parseText2,parseDefaultXMLNode,parseExtension];
	context._textParsers = [extensionParser];
	context._extensionParser = extensionParser;
    context._topChain = buildTopChain(context);
}
function parseExtension(node,context,chain){//extension
	return context._extensionParser.parse(node,context,chain);
}
function parseText2(text,context){
	if(typeof text == 'string'){
		return parseText(text,context,context._textParsers)
	}else{
		$log.error("未知节点类型",typeof text,text)
		//chain.next(text);
	}
}
ParseContext.prototype = {
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
		this._context._textType = textType;
		parseText2(source,this);
		this._context._textType = oldType;
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
		this.setCurrentNode(source);
		if(type>0){//xml
			//$log.info(len,source && source.xml)
			this._topChain.next(source);
		}else{//text
			if(source instanceof URI){
				source = this.loadXML(source);
				if(typeof source == 'string'){
					source=source.replace(/#.*[\r\n]*/,'');
				}
			}
			if(typeof source != 'string'){
				//NodeList
				var len = source.length;
				if(len >= 0 && typeof source.item != 'undefined'){//NodeList
					for(var i = 0;i<len;i++){
						this._topChain.next(source.item(i));
					}
					return;
				}
			}
			this._topChain.next(source);
		}
		
		
	},
    createURI:function(path) {
    	//$log.error(path,this.currentURI,this.config._root)
    	var cu = this.getCurrentURI();
    	if(cu){
    		//if(cu.scheme == 'data'){
    		//	return new URI(cu);
    		//}else{
    		return cu.resolve(path);
    		//}
    	}else{
    		path= path.replace(/^[\\\/]/,'./');// /xxx=>./xxx
    		//$log.warn(defaultBase+'',path,defaultBase.resolve(path)+'',defaultBase.authority)
    		return defaultBase.resolve(path);
    	}
    	
    	
    },
    loadText:function(uri){
    	//only for java
    	if(uri.scheme == 'lite'){
    		var path = uri.path+(uri.query||'');
    		path = path.replace(/^\//,'./')
    		uri = this.config._root.resolve(path);
    	}
    	var xhr = new XMLHttpRequest();
	    xhr.open("GET",url,false)
	    xhr.send('');
	    ////text/xml,application/xml...
	    return xhr.responseText;
    },
    loadXML:function(path){
    	if(!(path instanceof URI)){
    		path = new URI(path)
    	}
    	this.setCurrentURI(path);
    	return loadXML(path,this._config._root)
    },
    openStream:function(uri){
//    	//only for java
//    	if(uri.scheme == 'lite'){
//    		var path = uri.path+(uri.query||'');
//    		path = path.replace(/^\//,'./')
//    		uri = this.config._root.resolve(path);
//    	}
//    	return Packages.org.xidea.lite.impl.ParseUtil.openStream(uri)
		throw new Error("only for java");
    },
    getCurrentURI:function(){
    	return this._context._currentURI;
    },
    setCurrentURI:function(uri){
    	this._context._currentURI = new URI(uri);
    },
    getCurrentNode:function(){
    	return this._context._currentNode;
    },
    setCurrentNode:function(node){
    	this._context._currentNode = node;
    },
	getTextType:function(){
		return this._context._textType;
	},
	setAttribute:function(key,value){
		setByKey(this._context._attributeMap,key,value)
	},
	getAttribute:function(key){
		return getByKey(this._context._attributeMap,key)
	},

	addNodeParser:function(np){
		this._nodeParsers.push(np);
	},
	addTextParser:function(tp){
		this._textParsers.push(tp);
	},
	addExtension:function(ns,pkg){
		this._extensionParser.addExtension(ns,pkg);
	},
    createNew:function(){
    	return new ParseContext(this._config,this.getCurrentURI());
    }
}
var rm = ResultContext.prototype;
for(var n in rm){
	if(rm[n] instanceof Function){
		ParseContext.prototype[n] = buildResultWrapper(n);
	}
}
function buildResultWrapper(n){
	return function(){
		var result = this._result;
		return result[n].apply(result,arguments)
	}
}