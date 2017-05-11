var DOCUMENT_LAYOUT_PROCESSED = "http://www.xidea.org/lite/core/c:layout-processed";
var HAS_LAZY_WIDGET = "http://www.xidea.org/lite/core/c:widget#has-lazy";

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


function loadTextAndTraceResource(ctx,path){
	var uri = ctx.createURI(path);
    ctx.setCurrentURI(uri);//trace resource
	return ctx.loadText(uri);
}

function extractWidgetResource(ctx,src,doc,resourceFragment,bodyFragment){
	var cssPath = src.replace(/\.\w+$/,'.css');
	var jsPath = src.replace(/\.\w+$/,'.js');
	var body = doc.getElementsByTagName('body')[0];
	//append any style && links
	var links = doc.getElementsByTagName('link');
	//console.log('links length:',links.length)
	for(var i=0,len= links.length;i<len;i++){
		var res = links[i];
		//res.parentNode && res.parentNode.removeChild(res);
		resourceFragment.appendChild(res)
		
		
	}
	var source = cssPath && loadTextAndTraceResource(ctx,cssPath);

	//console.log('css',cssPath,source)
	if(source){
		var s = doc.createElementNS(HTML_URI,'link');
		//s.namespaceURI = doc.documentElement.namespaceURI;
		s.setAttribute('rel','stylesheet');
		s.setAttribute('type','text/css');
		s.setAttribute('href',cssPath);
		//console.log('css!!!!'+s)
		resourceFragment.appendChild(s)
	}
	
	//append outbody scripts;
	if(body){
		var res = body.firstChild;
		//console.log(body+'')
		while(res!=null){
			//console.log('!!!'+node.nodeType+node.nextSibling)
			var next = res.nextSibling;
			bodyFragment.appendChild(res)//append will be removed from old tree!
			res = next;
		}
	}else{
		bodyFragment.appendChild(doc.documentElement)
	}
	//append javascript resources;
	var source = jsPath && loadTextAndTraceResource(ctx,jsPath);
	return source;
}
/**
 * widget body 外允许放link和script 节点，这些节点会直接嵌入（预装载）。
 */
function processWidget(node){
	var ctx = this;
	var lazy = node.nodeName.match(/lazy/i);
	var currentURI = ctx.currentURI;

	var widgetPath =  findXMLAttribute(node,"path");
	var uri = ctx.createURI(widgetPath);
	
	
	//var lessPath = src.replace(/\.\w+$/,'.less');
	if(widgetPath == null){
		console.error('widget path is null!!')
		return;
	}
	var ownerDoc = node.ownerDocument;
	var doc = ctx.loadXML(uri);
	var resourceFragment = ownerDoc.createDocumentFragment();
	var bodyFragment = ownerDoc.createDocumentFragment();
	//extract widget resource and set currentURL
	var widgetScript = extractWidgetResource(ctx,widgetPath,doc,resourceFragment,bodyFragment)
    ctx.setCurrentURI(uri);
	try{
		ctx.parse(resourceFragment);
		node.nodeType == 1 && node.removeAttribute('path');
		var config={};
		var tagName = _appendWidgetStart(ctx,node,config,lazy);
		var widgetId = config.id;
		//console.error("!!!lazy",lazy)
		if(lazy){
			ctx.setAttribute(HAS_LAZY_WIDGET,true)
			parseChildRemoveAttr(ctx,node);
			tagName && ctx.appendText('</'+tagName+'>')
			ctx.appendPlugin(PLUGIN_MODULE,JSON.stringify(config));
			ctx.parse(bodyFragment);
			ctx.appendEnd();
		}else{
			ctx.parse(bodyFragment);
			tagName && ctx.appendText('</'+tagName+'>')
		}
		
		if(widgetScript){
			var s = doc.createElementNS(HTML_URI,'script');
			//console.log(widgetScript)
			widgetScript = wrapWidgetScript(widgetId,widgetScript)
			//console.log(widgetScript)
			s.appendChild(doc.createTextNode(widgetScript));
			ctx.parse(s)
		}
	}finally{
		ctx.setCurrentURI(currentURI);
	}
}
function wrapWidgetScript(id,source){
	return '__widget_arrived('+JSON.stringify(id)+',function(){'+source+'})'
}

function _parseBlock(ctx,node){
	if(!node.nodeName.match(/lazy/i)){
		parseChildRemoveAttr(ctx,node);
	}else{
		var config={};
		ctx.appendPlugin(PLUGIN_MODULE,JSON.stringify(config));
		parseChildRemoveAttr(ctx,node);
		ctx.appendEnd();
	}
}
function _appendWidgetStart(ctx,node,config,lazy){
	var blockId = genBlockID(ctx);
	var widgetId = lazy?'lazy_'+blockId:'w_'+blockId;
	config.id = widgetId
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
		ctx.appendText('<div data-widget="',widgetId,'"');
		for(var n in attrMap){
			//ctx.appendText(' ',n,"='",config[n],"'");
			ctx.parse(attrMap[n])
		}
		ctx.appendText('>')
		return 'div';
	}else{
		node.ownerElement.setAttribute('data-widget',widgetId);
	}
}
function genBlockID(ctx){
	var oldId = ctx.getAttribute(genBlockID)||0;
	ctx.setAttribute(genBlockID,++oldId)
	return oldId
}
