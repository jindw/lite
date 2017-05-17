var DOCUMENT_LAYOUT_PROCESSED = "http://www.xidea.org/lite/core/c:layout-processed";

var HTML_URI = "http://www.w3.org/1999/xhtml"
var setNodeURI = require('./syntax-util').setNodeURI
var parseChildRemoveAttr =  require('./syntax-util').parseChildRemoveAttr
var findXMLAttribute =  require('./xml').findXMLAttribute
var wrapScript = require('./syntax-html').wrapScript;
var PLUGIN_MODULE = "org.xidea.lite.ModulePlugin"



exports.parse$9 = parseDocument;
exports.parseExtends = exports.interceptExtends = processExtends
//alise
//exports.parseExtend =  exports.interceptExtend = processExtends
exports.parseBlock = exports.interceptBlock = processBlock;
exports.parseWidget = exports.interceptWidget =processWidget;
exports.parseLazyWidget = exports.interceptLazyWidget =processWidget;

//addParserAndAttrInterceptor(processBlock,"module","lazy-module","lazy-block","block","group");
//var PLUGIN_DEFINE=require('./template-token').PLUGIN_DEFINE;
function parseDocument(doc,ns){//for extends syntax!! Document.nodeType == 9
	ns = ns||'http://www.xidea.org/lite/core'
	var isProcessed = this.getAttribute(DOCUMENT_LAYOUT_PROCESSED);
	//console.log(doc.documentURI+'',isProcessed)
	if(!isProcessed){
		this.setAttribute(DOCUMENT_LAYOUT_PROCESSED,true);
		var root = doc.documentElement;
		var ln = root.localName || root.nodeName.replace(/^w+\:/,'');
		if((ln == 'extends' || ln == 'extend') &&  root.namespaceURI == ns){
			
			processExtends.call(this,root);
			return ;
		}else{
			try{
				var attr = root.getAttributeNodeNS(ns,"extends") || root.getAttributeNodeNS(ns,"extend");
			}catch(e){
				var attrs = root.attributes;
				var i = attrs.length-1;
				while(i-->0){
					var a = attrs.item(i);
					if(a.namespaceURI == ns && /^(?:w+\:)?extends?$/.test(a.nodeName)){
						attr = a;
						break;
					}
				}
			}
			if(attr != null){
				processExtends.call(this,attr);
				return ;
			}
			var layout = this.configMap.layout;
			if(layout){
				var uri = this.createURI(layout);
				if(uri != String(this.currentURI)){//layout != this
					this.setAttribute('$page',doc);
					//console.log('layout:',this.currentURI+'',' of ',uri+'')
					//this.currentURI = uri;
					//doc = this.loadXML(uri);
					this.parse(uri);
				}
				
				return ;
			}
		}
	}
	this.next(doc);
}

function processExtends(node){
	var oldConfig = this.getAttribute("#extends");
	var el = node.nodeType == 1?node:node.ownerElement|| node.selectSingleNode('..');
	var root = el == el.ownerDocument.documentElement;
	var extendsConfig = {blockMap:{},parse:false,root:root};
	if(oldConfig){
		if(oldConfig.parse){//解析进行时
			if(root){//模板继承
				if(extendsConfig.root){
					//this.reset(0);
				}
				extendsConfig = oldConfig;
				extendsConfig.parse = false;
			}else{
				extendsConfig.root = false;
			}
		}else{//查找进行时
			return;
		}
	}
	
	this.setAttribute("#extends" ,extendsConfig);
	var parentURI = findXMLAttribute(node,"*path","value","parent");
	//childNodes
	var uri = this.createURI(parentURI);
	var parentNode = this.loadXML(uri);
	if(!root){//元素继承
		parentNode = parentNode.documentElement;
	}
	var i = this.mark();
	parseChildRemoveAttr(this,node);
	this.reset(i);
    var parentURI = this.currentURI;
	try{
		this.setCurrentURI(uri);
		extendsConfig.parse=true;
		this.parse(parentNode);
	}finally{
        this.setCurrentURI(parentURI);
	}
	this.setAttribute("#extends" ,oldConfig);
}

function processBlock(node){
	var extendsConfig = this.getAttribute("#extends");
	var value = findXMLAttribute(node,"name","id");
	if(extendsConfig){//
		var blockMap = extendsConfig.blockMap;
		var cached = value && (value in blockMap) && blockMap[value];
		if(extendsConfig.parse){
			if(cached){
				var parentURI = this.currentURI;
				try{
					//set current uri
					setNodeURI(this,cached);
					extendsConfig.parse=true;
					//this.parse(cached);
					_parseBlock(this,cached);
				}finally{
	        		this.setCurrentURI(parentURI);
				}
			}else{
				//this.parse(childNodes);
				_parseBlock(this,node);
			}
		}else{
			if(!cached){
				blockMap[value] = node;
			}
		}
	}else{
		_parseBlock(this,node);

	}
}


function loadText(ctx,path){
	var uri = ctx.createURI(path);
	return ctx.loadText(uri);
}

function processWidget(node){
	var ctx = this;
	var lazy = node.nodeName.match(/lazy/i);
	var currentURI = ctx.currentURI;
	var src =  findXMLAttribute(node,"path");
	var uri = ctx.createURI(src);
	var doc = ctx.loadXML(uri);
	
	//var lessPath = src.replace(/\.\w+$/,'.less');
	var cssPath = src.replace(/\.\w+$/,'.css');
	var jsPath = src.replace(/\.\w+$/,'.js');
	
	var fragment = doc.createDocumentFragment();
	var body = doc.getElementsByTagName('body')[0];
	var resources = doc.getElementsByTagName('link');
	var i= resources.length-1
	while(i-->0){
		var res = resources[i];
		res.parentNode && res.parentNode.removeChild(res);
		fragment.appendChild(res)
	}
	var source = loadText(ctx,cssPath);
	if(source){
		var s = doc.createElementNS(HTML_URI,'link');
		//s.namespaceURI = doc.documentElement.namespaceURI;
		s.setAttribute('rel','stylesheet');
		s.setAttribute('type','text/css');
		s.setAttribute('href',cssPath);
		//console.log('css!!!!'+s)
		fragment.appendChild(s)
	}
	if(body){
		var res = body.firstChild;
		//console.log(body+'')
		while(res!=null){
			//console.log('!!!'+node.nodeType+node.nextSibling)
			var next = res.nextSibling;
			fragment.appendChild(res)//append will be removed from old tree!
			res = next;
		}
		body.parentNode.removeChild(body)
		var resources = doc.getElementsByTagName('script');
		var i= resources.length-1
		while(i-->0){
			var res = resources[i];
			res.parentNode && res.parentNode.removeChild(res);
			fragment.appendChild(res)
		}
	}else{
		fragment.appendChild(doc.documentElement)
	}
	var source = loadText(ctx,jsPath);
	//TODO:...
	////(first?'!this.__widget_arrived&&(this.__widget_arrived=function(id,h){document.querySelector(id).innerHTML=h});':'')
	if(source){
		var s = doc.createElementNS(HTML_URI,'script');
		//s.setAttribute('src',jsPath);
		source = wrapScript(source,'__widget_arrived')
		s.appendChild(doc.createTextNode(source));
		fragment.appendChild(s)
	}
	try{
		node.nodeType == 1 && node.removeAttribute('path');
		if(lazy){
			var config={};
			var tagName = _appendLazyStart(ctx,node,config);
			parseChildRemoveAttr(ctx,node);
			tagName && ctx.appendText('</'+tagName+'>')
			ctx.appendPlugin(PLUGIN_MODULE,JSON.stringify(config));
			this.parse(fragment);
			ctx.appendEnd();
		}else{
			this.parse(fragment);
		}
	}finally{
		this.currentURI = currentURI;
	}
}
/**
 * node src 指向的src 是延迟载入
 * node 子节点放 loading 子页面
 */
function parseLazyWidget(node){
	
}

function _parseBlock(ctx,node){
	if(!node.nodeName.match(/lazy/i)){
		parseChildRemoveAttr(ctx,node);
	}else{
		var config={};
		var tagName = _appendLazyStart(ctx,node,config);
		ctx.appendPlugin(PLUGIN_MODULE,JSON.stringify(config));
		parseChildRemoveAttr(ctx,node);
		ctx.appendEnd();
		tagName && ctx.append('</'+tagName+'>')
	}
}
function _appendLazyStart(ctx,node,config){
	var blockId = genBlockID(ctx);
	//var elementId = '__lazy_module_'+blockId+'__';
	if(node.nodeType == 1){
		var attrs = node.attributes ;
		var attrMap = {}
		for(var i=0,len = attrs.length;i<len;i++){
			var a = attrs.item(i);
			var n = a.name;
			if(!n.match(/\:|^id$/i)){
				attrMap[n] = a;
				config[n] = a.value;
			}
		}
		ctx.appendText('<div data-lazy-widget="',blockId,'"');
		for(var n in attrMap){
			//ctx.appendText(' ',n,"='",config[n],"'");
			ctx.parse(attrMap[n])
		}
		ctx.appendText('>')
		config.id=blockId
		return 'div';
	}else{
		node.ownerElement.setAttribute('data-lazy-widget',blockId);
		config.id=blockId
	}

	
}
function genBlockID(ctx){
	var oldId = ctx.getAttribute(genBlockID)||0;
	ctx.setAttribute(genBlockID,++oldId)
	return oldId
}
