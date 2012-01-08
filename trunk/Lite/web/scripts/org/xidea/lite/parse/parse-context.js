if(typeof require == 'function'){
var ResultContext=require('./result-context').ResultContext;
var ParseConfig=require('./config').ParseConfig;
var URI=require('org/xidea/lite/util/resource').URI;
var loadLiteXML=require('org/xidea/lite/util/xml').loadLiteXML;
}/*
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
	this.currentURI = defaultBase;
	this.featureMap = config.getFeatureMap(path);
    this.textType=0;
	this._path = path;
	this._attributeMap = [[],[],{}]
    this._result = new ResultContext();
	this._context = this;
	this._result._context = this;
	this._resources = [];
	initializeParser(this,config.getExtensionMap(path));
}
/**
 * 初始化上下文
 * @arguments 链顶插入的解析器列表（第一个元素为初始化后的链顶解析器，以后类推）
 */
function initializeParser(context,extensionMap){
	var extensionParser = new ExtensionParser();
	for(var ns in extensionMap){
		var exts = extensionMap[ns];
		for(var len = exts.length,i=0;i<len;i++){
			extensionParser.addExtension(ns,exts[i])
		}
	}
	context._nodeParsers = [parseTextLeaf,parseDefaultXMLNode,parseExtension];
	context._textParsers = [extensionParser];
	context._extensionParser = extensionParser;
    context._topChain = buildTopChain(context);
}
function parseExtension(node,context,chain){//extension
	return context._extensionParser.parse(node,context,chain);
}
function parseTextLeaf(text,context){
	if(typeof text == 'string'){
		return parseText(text,context,context._textParsers)
	}else{
		console.error("未知节点类型",typeof text,text)
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
			console.error("未知编码模式："+textType)
			throw new Error();
		}
		
		var mark = this.mark();
		var oldType = this.textType;
		this._context.textType = textType;
		parseTextLeaf(source,this);
		this._context.textType = oldType;
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
			//console.info(len,source && source.xml)
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
				var nodeType = source.nodeType;
				
				if(nodeType === undefined && typeof source.item != 'undefined'){//NodeList
					if(len === 0){
						return;
					}
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
    	//console.error(path,this.currentURI,this.config.root)
    	var base = this.config.root.toString();
    	if(path.indexOf(base) ==0){
    		path = path.substring(base.length-1);
    	}
    	var cu = this.currentURI;
    	if(cu){
    		//if(cu.scheme == 'data'){
    		//	return new URI(cu);
    		//}else{
    		//console.log(path,cu)
    		//console.log('???'+cu.resolve(path))
    		return cu.resolve(path);
    		//}
    	}else{
    		path= path.replace(/^[\\\/]/,'./');// /xxx=>./xxx
    		//console.warn(defaultBase+'',path,defaultBase.resolve(path)+'',defaultBase.authority)
    		
    		//console.log(path,defaultBase)
    		//console.log('###'+defaultBase.resolve(path))
    		return defaultBase.resolve(path);
    	}
    	
    	
    },
    loadText:function(uri){
    	//only for java
    	if(uri.scheme == 'lite'){
    		var path = uri.path+(uri.query||'');
    		path = path.replace(/^\//,'./')
    		uri = this.config.root.resolve(path);
    	}
    	var xhr = new XMLHttpRequest();
	    xhr.open("GET",uri,false)
	    xhr.send('');
	    ////text/xml,application/xml...
	    return xhr.responseText;
    },
    loadXML:function(path){
    	var t1 = +new Date();
    	if(path instanceof URI){
    	}else{
    		if(/^\s*</.test(path)){
    			return loadLiteXML(path,this.config.root)
    		}else{
    			path = new URI(path)
    		}
    	}
    	this.setCurrentURI(path);
    	var doc = loadLiteXML(path,this.config.root);
    	this._context._loadTime+=(new Date()-t1);
    	return doc;
    },
    openStream:function(uri){
//    	//only for java
//    	if(uri.scheme == 'lite'){
//    		var path = uri.path+(uri.query||'');
//    		path = path.replace(/^\//,'./')
//    		uri = this.config.root.resolve(path);
//    	}
//    	return Packages.org.xidea.lite.impl.ParseUtil.openStream(uri)
		throw new Error("only for java");
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
	getFeature:function(key){
		return this.featureMap[key];
	},
	getFeatureMap:function(){
		return this.featureMap;
	},
    setCurrentURI:function(uri){
    	this._context.addResource(uri=new URI(uri));
    	this._context.currentURI = uri;
    },
    addResource:function(uri){
    	for(var rs = this._resources, i=0;i<rs.length;i++){
    		if(rs[i]+'' == uri){
    			return ;
    		}
    	}
    	this._resources.push(uri);
    },
    getResources:function(){
    	return this._resources;
    },
    createNew:function(){
    	var nc = new ParseContext(this.config,this.currentURI);
    	nc.featureMap = this.featureMap;
    	nc._resources = this._resources;
    	return nc;
    },
    _loadTime :0
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
if(typeof require == 'function'){
exports.ParseContext=ParseContext;
var buildTopChain=require('./parse-chain').buildTopChain;
var ExtensionParser=require('./extension-parser').ExtensionParser;
var Extension=require('./extension').Extension;
var parseDefaultXMLNode=require('./xml-default-parser').parseDefaultXMLNode;
var parseText=require('./text-parser').parseText;
var getByKey=require('org/xidea/lite/util/kv').getByKey;
var setByKey=require('org/xidea/lite/util/kv').setByKey;
var XA_TYPE=require('org/xidea/lite/impl/template-token').XA_TYPE;
var EL_TYPE=require('org/xidea/lite/impl/template-token').EL_TYPE;
var XT_TYPE=require('org/xidea/lite/impl/template-token').XT_TYPE;
}