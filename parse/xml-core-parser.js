/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var CORE_URI = "http://www.xidea.org/lite/core"
var PLUGIN_NATIVE = "org.xidea.lite.NativePlugin"
var Core = {
	xmlns : function(){},
	seek:function(text){
		var end = findELEnd(text,0);
		if(end>0){
			try{
				var els = text.substring(1,end);
				var el = this.parseEL(els);
	            switch(this.textType){
	            case XT_TYPE:
	            	this.appendXT(el);
	            	break;
	            case XA_TYPE:
	            	this.appendXA(null,el);
	            	break;
	            default:
	            	this.appendEL(el);
	            }
	            return end;
			}catch(e){
				console.error("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+els+"]",e)
				return -1;
			}
		}else{
			console.warn("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",text:"+text+"]")
			return -1;
		}
	},
	"seek!":function(text){
		var end = findELEnd(text,0);
		if(end>0){
			try{
				var el = text.substring(1,end);
	            this.appendEL(el);
	            return end;
			}catch(e){
				console.error("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
				return -1;
			}
		}else{
			console.warn("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
			return -1;
		}
	},
	/**
	 * TODO:...
	 * ${a,b=>x*b}
	 * ${list.sort({a=>a.length-b.length})}
	 *
	"seek:":function(text){
		var sp = text.indexOf(':');
		var end = sp>0 && findELEnd(text,sp);
		if(end){
			var args = text.substring(1,sp);
			var el = text.substring(sp+1,end);
	    	var config = _parseDefName('('+ns+')');
	    	this.appendPlugin(PLUGIN_DEFINE,JSON.stringify(config));
	        this.appendEL(el);
	        return end;
		}else{
			console.warn("Lambda 表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
			return -1;
		}
	},*/
	"seek#":function(text){
		var end = findELEnd(text,0);
		if(end>0){
			try{
				var el = text.substring(1,end);
	            this.appendPlugin("org.xidea.lite.EncodePlugin","{}");
	            this.appendEL(el);
	            this.appendEnd()
	            return end;
			}catch(e){
				console.error("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
				return -1;
			}
		}else{
			console.warn("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
			return -1;
		}
		
	},
	seekxa:function(text){
		var end = findELEnd(text,0);
		if(end>0){
			try{
				var el = text.substring(1,end);
				if(/^\s*([\w\-]+|"[^"]+"|'[^']+')\s*\:/.test(el)){
					var map = findLiteParamMap(el);
					for(var n in map){
						this.appendXA(n,map[n]);
					}
				}else{
					this.appendXA(null,el)
				}
		    	return end;
			}catch(e){
				console.error("XML属性表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
				return -1;
			}
		}else{
			console.warn("XML属性表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
			return -1;
		}
	},
	seekxt:function(text){
		var end = findELEnd(text,0);
		if(end>0){
			try{
				var el = text.substring(1,end);
				this.appendXT(el)
	            return end;
			}catch(e){
				console.error("XML文本表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
				return -1;
			}
		}else{
			console.warn("XML文本表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
			return -1;
		}
	},
	seekEnd : function(text){
		this.appendEnd();
		return 0;
	},
	parseExtension:function(node){
		var ns = findXMLAttribute(node,'*namespace','ns');
		var file = findXMLAttribute(node,'file');
		var pkg = findXMLAttribute(node,'package');
		
		if(pkg){
			source = pkg;
		}else if(file){
			var source = this.loadText(this.createURI(file))+'\n';
		}else{
			var source = findXMLAttribute(node,'#text')+'\n';
		}
		this.addExtension(ns,source);
	},
	parse9 : function(doc,ns){
		var isProcessed = this.getAttribute(DOCUMENT_LAYOUT_PROCESSED);
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
			 	//console.log('layout:',layout,' of ',uri)
			 	if(layout){
			 		this.setAttribute('$page',doc);
			 		var uri = this.createURI(layout);
					//this.currentURI = uri;
			 		//doc = this.loadXML(uri);
			 		this.parse(uri);
			 		return ;
			 	}
			 }
		}
		this.next(doc);
//		console.error('aaa',ns);
	//},
	//"parse*" : function(node){
	//	console.error("未支持标签："+node.tagName)
	},
	parsePHP:function(node){
    	var value = node.textContent || node.text;
    	this.appendPlugin(PLUGIN_NATIVE,'{"type":"php"}');
    	parseChildRemoveAttr(this,node);
    	this.appendEnd();
	},
	parseJS:function(node){
    	var value = node.textContent || node.text;
    	this.appendPlugin(PLUGIN_NATIVE,'{"type":"js"}');
    	parseChildRemoveAttr(this,node);
    	this.appendEnd();
	},
	parseComment:function(){}
}
var DOCUMENT_LAYOUT_PROCESSED = "http://www.xidea.org/lite/core/c:layout-processed";
var CHOOSE_KEY = "http://www.xidea.org/lite/core/c:choose@value";
var FOR_PATTERN = /\s*([\$\w_]+)\s*(?:,\s*([\w\$_]+))?\s*(?:\:|in)([\s\S]*)/;
function _parseChild(context,node){
	node = node.firstChild;
	while(node){
		context.parse(node)
		node = node.nextSibling;
	}
}

/**
 * node,attribute
 */
function processIf(node){
	var test = findXMLAttributeAsEL(node,'*test','value');
    this.appendIf(test);
    parseChildRemoveAttr(this,node);
    this.appendEnd();
}

function seekIf(text){
	var end = findELEnd(text,0);
	if(end>0){
		this.appendIf(text.substring(1,end));
		return end;
	}
}

function processElse(node){
    var test = findXMLAttributeAsEL(node,'test','value');
    this.appendElse(test || null);
    parseChildRemoveAttr(this,node);
    this.appendEnd();
}
function seekElse(text){
	if(text.charAt() == '$'){
		this.appendEnd();
		this.appendElse(null);
		return 0;
	}else{
		var end = findELEnd(text);
		if(end>0){
			this.appendEnd();
			this.appendElse(text.substring(1,end)||null);
			return end;
		}
	}
}
function processElif(node){
    var test = findXMLAttributeAsEL(node,'*test','value');
    this.appendElse(test || null);
    parseChildRemoveAttr(this,node);
    this.appendEnd();
}
function seekElif(text){
	var end = findELEnd(text);
	if(end>0){
		this.appendEnd();
		this.appendElse(text.substring(1,end)||null);
		return end;
	}
}



function processChoose(node){
	var value = findXMLAttributeAsEL(node,"value","test");
	var oldStatus = this.getAttribute(CHOOSE_KEY);
	this.setAttribute(CHOOSE_KEY,{value:value,first:true});
	parseChildRemoveAttr(this,node,true);
	this.setAttribute(CHOOSE_KEY,oldStatus);
}
//function seekChoose(text){
//	if(text.charAt() != '$'){
//		var end = findELEnd(text,-1);
//		if(end<=0){
//			console.error('表达式异常')
//			return -1;
//		}else{
//			var value = text.substring(1,end);
//		}
//	}else{
//		end = 1;
//	}
//	var oldStatus = this.getAttribute(processChoose);
//	this.setAttribute(processChoose,{value:value,first:true});
//	parseChildRemoveAttr(this,node);
//	value && this.setAttribute(processChoose,oldStatus);
//	return end;
//}
function processWhen(node){
	var stat = this.getAttribute(CHOOSE_KEY);
	var value = findXMLAttributeAsEL(node,"*test","if","value");
	if(stat.value){
		value = '('+stat.value + ')==('+value+')';
	}
	if(stat.first){
		stat.first = false;
		this.appendIf(value);
	}else{
		this.appendElse(value);
	}
	parseChildRemoveAttr(this,node);
	this.appendEnd();
}

function processOtherwise(node){
	this.appendElse(null);
	parseChildRemoveAttr(this,node);
	this.appendEnd();
}

function processFor(node){
	if(node.nodeType == 1){
    	var value = findXMLAttributeAsEL(node,'*list','values','items','value');
    	var var_ = findXMLAttribute(node,'*var','name','id','item');
    	var status_ = findXMLAttribute(node,'status');
	}else{//attr
		var value = findXMLAttribute(node);
		var match = value.replace(/^\$\{(.+)\}$/,'$1').match(FOR_PATTERN);
		if(!match){
			throw console.error("非法 for 循环信息",value);
		}
		var var_ = match[1];
		var status_ =match[2];
		var value =match[3];
	}
    startFor(this,var_,value,status_ || null);
    parseChildRemoveAttr(this,node);
    this.appendEnd();
}
function seekFor(text){
	var end = findELEnd(text);
	if(end>0){
		var value = text.substring(1,end);
		var match = value.match(FOR_PATTERN);
		if(!match){
			throw console.error("非法 for 循环信息",value);
		}
		var var_ = match[1];
		var status_ =match[2];
		var value =match[3];
		startFor(this,var_,value,status_ || null);
    	return end;
	}
}
function splitList(list){
	try{
		new Function("return "+list.replace(/\.\./g,'.%%.'));//for x4e
		return [list];
	}catch(e){
		var dd= 0
		while(true){
			dd = list.indexOf('..',dd+1);
			if(dd>0){
				try{
					var begin = list.substring(0,dd);
					var end = list.substring(dd+2);
					new Function("return "+begin+'-'+end);//for x4e
					
					var begin2 = begin.replace(/^\s*\[/,'');
					
					if(begin2 != begin){
						try{
							new Function("return "+begin);
							begin2 = begin;
						}catch(e){
						}
					}
					if(begin2 != begin){
						end = end.replace(/\]\s*$/,'');
						console.debug("[start,last] 语法 不是通用表达式，只能在for循环中使用。",list);
						return [begin2,end];
					}else{
						console.warn("range for 表达式(非通用表达式)推荐模式为：[start,last]，您提供的表达式为"+list);
						return [begin,end];
					}
				}catch(e){
				}
				//value = list.substring(0,dd)+'-'+list.substring(dd+2)
			}else{
				return [];
			}
		}
	}
}
function startFor(context,key,list,status_){
	var be = splitList(list);
	if(be.length==2){
		var begin = be[0];//list.substring(0,dd);
		var end = be[1];//list.substring(dd+2);
		list = "Math.abs("+begin+'-'+end+")+1";
		context.appendFor(key,list,status_||null);
		context.appendVar(key,key+'+'+begin+"-1");
	}else if(be.length ==1){
		context.appendFor(key,list,status_);
	}else{
		console.error("for表达式无效："+list);
	}
}


function processVar(node){
    var name_ = findXMLAttribute(node,'*name','id');
	if(node.nodeType == 1){
		var value = findXMLAttribute(node,'value');
	    if(value){
	    	var code = this.parseText(value,0);
	    	if(code.length == 1){
	    		code = code[0];
	    		if(code instanceof Array){
	    			this.appendVar(name_,code[1]);
	    		}else{
	    			console.warn("标签:"+node.tagName+"的value属性"+value+"建议设置为表达式，您的输入没有表达式，系统自动按静态文本处理");
	    			this.appendVar(name_,JSON.stringify(code));
	    		}
	    	}else{
	    		this.appendCapture(name_);
		        this.appendAll(code)
		        this.appendEnd();
	    	}
	    }else{
	        this.appendCapture(name_);
	        parseChildRemoveAttr(this,node);
	        this.appendEnd();
	    }
	}else{
		var map = findLiteParamMap(name_);
		if(map){
			for(var n in map){
				this.appendVar(n,map[n]);
			}
			parseChildRemoveAttr(this,node);
		}else{
	        this.appendCapture(name_);
	        parseChildRemoveAttr(this,node);
	        this.appendEnd();
		}
	}
}
function seekVar(text){
	var end = findELEnd(text);
	if(end>0){
		var value = text.substring(1,end);
		if(/^\s*(?:\w+|['"][^"]+['"])\s*$/.test(value)){
	        this.appendCapture(value.replace(/['"]/g,''));
		}else{
			var map = findLiteParamMap(value);
			for(var n in map){
				this.appendVar(n,map[n]);
			}
		}
    	return end;
	}
}


function parseOut(node){
    var value = findXMLAttribute(node,"value","#text");
    value = this.parseText(value,EL_TYPE);
    this.appendAll(value);
}
function seekOut(text){
	var end = findELEnd(text);
	if(end>0){
		var value = text.substring(1,end);
		this.appendEL(value);
    	return end;
	}
}

function _parseDefName(name){
	var n = name;
	var i = n.indexOf('(');
	var defaults = [];
	var params = [];
	if(i>0){
		var args = n.substring(i+1);
		args = args.replace(/^\s+|\)\s*$/g,'')
		n = toid(n.substring(0,i));
		i = 0;
		while(args){
			i = args.indexOf(',',i);
			if(i>0){
				var arg = args.substring(0,i);
				try{
					new Function(arg);
					args = args.substring(i+1).replace(/^\s+|\s+$/g,'');
					i=0;
				}catch(e){
					i++;
					continue;
				}
			}else{
				arg = args;
				args = null;
				try{
					new Function(arg);
				}catch(e){
					console.error("函数定义中参数表语法错误:"+arg+name,e);
					throw e;
				}
			}
			var p = arg.indexOf('=',i);
			if(p>0){
				params.push(toid(arg.substring(0,p)));
				defaults.push(JSON.parse(arg.substring(p+1)));
			}else{
				if(defaults.length){
					var msg = "函数定义中参数表语法错误:默认参数值能出现在参数表最后:"+name;
					console.error(msg);
					throw new Error(msg);
				}
				params.push(toid(arg));
			}
			
			
		}
		
		return {"name":n,"params":params,"defaults":defaults};
	}else{
		return {"name":n}
	}
}
function toid(n){
	n = n.replace(/^\s+|\s+$/g,'');
	try{
		new Function("return "+n);
	}catch(e){
		console.error("无效id:"+n,e);
		throw e;
	}
	return n;
}
function processDef(node){
    var ns = findXMLAttribute(node,'*name');
    var config = _parseDefName(ns);
    this.appendPlugin(PLUGIN_DEFINE,JSON.stringify(config));
    parseChildRemoveAttr(this,node);
    this.appendEnd();
}
function seekDef(text){
    var end = findELEnd(text);
	if(end>0){
		var ns = text.substring(1,end);
	    var config = _parseDefName(ns);
	    this.appendPlugin(PLUGIN_DEFINE,JSON.stringify(config));
    	return end;
	}
}

function seekClient(text){
	var end = findELEnd(text);
	if(end>0){
		var ns = text.substring(1,end);
	    var config = _parseDefName(ns);
	    this.appendPlugin("org.xidea.lite.parse.ClientPlugin",JSON.stringify(config));
    	return end;
	}
}

function processClient(node){
	var name_ = findXMLAttribute(node,'*name','id');
	var config = _parseDefName(name_);
	this.append("<script>//<![CDATA[\n");
	this.appendPlugin("org.xidea.lite.parse.ClientPlugin",JSON.stringify(config));
	parseChildRemoveAttr(this,node);
	this.appendEnd();
	this.append("//]]></script>")
}
/**
 * <c:date-format pattern="" >
 */
function dateFormat(node){
	var value =  findXMLAttributeAsEL(node,'value','date','time','#text').replace(/^\s+|\s+$/g,'') || 'null';
	var pattern = findXMLAttribute(node,'pattern');
	if(pattern){
		var pattern2 = pattern.replace(/^\s*\$\{([\s\S]+)\}\s*$/,'$1')
		if(pattern2 == pattern){
			pattern2 = JSON.stringify(pattern);
		}
	}else{
		pattern2 = '"YYYY-MM-DD"';
	}
	this.appendPlugin("org.xidea.lite.DatePlugin","{}");
	this.appendEL(pattern2);
	this.appendEL(value);
	this.appendEnd();
}
Core.parseDateFormat = dateFormat
Core.parseDate = dateFormat
Core.parseTime = dateFormat
function beforeInclude(attr){
	var match = attr.value.match(/^([^#]*)(?:#(.*))?$/);
	var path = match[1];
	var xpath = match[2];
	if(path){
		var path2 = path.replace(/^[\$#]([\w\$_]+)$/,'$$$1');
		if(path2.charAt() == '$') {
			doc = this.getAttribute(path2);
		}else{
			var doc = this.loadXML(this.createURI(path))
		}
	}else{
		var doc = attr.ownerDocument;
	}
	if(doc==null){
		this.append("<strong style='color:red'>没找到包含节点："+this.currentURI+ attr.value+"</strong>");
	}else{
		var attrs = selectByXPath(doc, xpath);
		var element = attr.ownerElement || attr.selectSingleNode('..');
		//element.removeAttributeNode(attr)
		for(var i = attrs.length;i--;){
			var a = attrs.item(i);
			mergeAttribute(element,a);
		}
		this.process(element);
	}
}
function mergeAttribute(element,node){
	if(node.nodeType == 2){
		var attr = element.getAttributeNS(node.namespaceURI,node.name);
		element.setAttributeNS(node.namespaceURI,node.name,attr+node.value);
	}else if(node.nodeType == 1){
		var attributes = node.attributes;
		for (var i = 0; i < attributes.length; i++) {
			mergeAttribute(element,attributes.item(i));
		}
	}
}
function setNodeURI(context,node){
	if(!node.nodeType){
		if(node.length){
			node = node.item(0);
		}
	}
	var doc = node.nodeType == 9?node:node.ownerDocument;
	if(doc){
		
		var uri = doc.documentURI
		if(/^lite:\//.test(uri)){
			context.setCurrentURI(context.createURI(uri));
		}else if(uri){
			var info = getLiteTagInfo(doc.documentElement);
			//console.log(info)
			var i = info && info.indexOf('|@');
			if(i>0){
				uri = info.substring(i+2);
			}
			context.setCurrentURI(context.createURI(uri));
			//console.error(uri,info)
		}
	}
}
function parseInclude(node){
    var path = findXMLAttribute(node,'path');
    var xpath = findXMLAttribute(node,'xpath');
    var selector = findXMLAttribute(node,'selector');
    var parentURI = this.currentURI;
	try{
	    if(path!=null){
	    	if(path.charAt() == '#'){
	    		console.warn("装饰器命名节点改用${pageName}模式了:(,您实用的模式还是:"+path);
	    		path = '$'+path.substring(1);
	    	}
	    	if(path.charAt() == '$'){
	    		doc = this.getAttribute(path);
	    		setNodeURI(this,node);
	    	}else{
		        var uri = this.createURI(path);
		        var doc = this.loadXML(uri);
	    		this.setCurrentURI(uri);
	    	}
	    }else{
	    	var doc = this.loadXML(this.currentURI);
	    	var doc = node.ownerDocument
	    }
		if(doc==null){
			this.append("<strong style='color:red'>没找到包含节点："+this.currentURI+ node.value+"</strong>");
		}else{
		    if(xpath!=null){
		    	var d = doc;
		        doc = selectByXPath(doc,xpath);
		        //alert([url,xpath,new XMLSerializer().serializeToString(d),doc.length])
		    }
		    if(selector != null){
		    	console.warn("目前尚不支持css selector 选择节点");
		    }
		        
		    this.parse(doc)
		    
		}
    }finally{
        this.setCurrentURI(parentURI);
    }
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
	var childNodes = node.nodeType == 1?node.childNodes:node.ownerElement||node.selectSingleNode('..');
		
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
					this.parse(cached);
				}finally{
	        		this.setCurrentURI(parentURI);
				}
			}else{
				this.parse(childNodes);
			}
		}else{
			if(!cached){
				blockMap[value] = childNodes;
			}
		}
	}else{
		this.parse(childNodes);
	}
}

function processI18N(node){
	var i18nKey = findXMLAttribute(node,'i18n');
	var uri = this.currentURI;
	var path = uri.scheme == 'lite'? uri.path: String(uri);
	if(node.nodeType == 1){
		var begin = this.mark();
		_parseChild(this,node);
		var content = this.reset(begin);
		
		i18nKey = i18nHash(path,i18nKey,node.textContent);
		//
		this.parse("${I18N."+i18nKey+"}");
	}else{
		var el = node.ownerElement;
		var node2 = el.cloneNode(true)||el;
		var begin = this.mark();
		this.parse(el.textContent);
		var content = this.reset(begin);

		i18nKey = i18nHash(path,i18nKey,el.textContent);
		node2.textContent = "${I18N."+i18nKey+"}";
		node2.removeAttribute(node.name);
		node2.setAttribute('data-i18n-key',i18nKey)
		this.next(node2);
	}
	addI18NData(this,i18nKey,content);
}
function seekI18N(text){
	
}
function addI18NData(context,i18nKey,content){
	if(typeof content != 'string' && content.length == 1){
		content = content[0];
	}
	var i18nSource = context.getAttribute("#i18n-source");
	var i18nObject = context.getAttribute("#i18n-object");
	if(!i18nObject){
		i18nObject = {};
		context.setAttribute("#i18n-object",i18nObject);
	}
	if(i18nKey in i18nObject){
		i18nObject[i18nKey] = content;
		i18nSource = JSON.stringify(i18nObject)
	}else{
		if(i18nSource){
			i18nSource = i18nSource.slice(0,-1)+',';
		}else{
			i18nSource = '{';
		}
		i18nSource = i18nSource + '"'+i18nKey+'":' +JSON.stringify(content)+'}';
	}
	
	context.setAttribute("#i18n-data",i18nSource);
}


function parseChildRemoveAttr(context,node,ignoreSpace){
	if(node.nodeType == 1){//child
		var child = node.firstChild;
		if(ignoreSpace){
			while(child){
				if(child.nodeType != 3 || String(child.data).replace(/\s+/g,'')){
					context.parse(child)
				}
				child = child.nextSibling;
			}
		}else{
			while(child){
				context.parse(child)
				child = child.nextSibling;
			}
		}
	}else if(node.nodeType == 2){//attr
		var el = node.ownerElement||node.selectSingleNode('..');
		el.removeAttributeNode(node);
		context.parse(el);//||node.selectSingleNode('parent::*'));
	}else {//other
		context.parse(node)
	}
}








function addParser(map,n){
	for(var p in map){
		Core[p+n] = map[p];
	}
}
function addAll(pb,seeker){
	var i = arguments.length;
	while(--i>1){
		var map = {parse:pb,before:pb};
		if(seeker){
			map.seek = seeker;
		}
		addParser(map,arguments[i])
	}
}

addAll(processIf,seekIf,'if')
addAll(processElse,seekElse,'else');
addAll(processElif,seekElif,"elseif","elif");
addAll(processFor,seekFor,"for","foreach");
addAll(processVar,seekVar,"var","set");
addAll(parseOut,seekOut,"out");
addAll(processDef,seekDef,"def",'macro');
addAll(processClient,seekClient,"client");
//addAll(processI18N,seekI18N,'i18n')

//没有seeker
addAll(processChoose,null,"choose");
addAll(processWhen,null,"when");
addAll(processOtherwise,null,"otherwise");
addAll(processExtends,null,"extends","extend");
addAll(processBlock,null,"block","group");

//属性与标签语法差异太大,不能用统一函数处理.
addParser({parse:parseInclude,before:beforeInclude},"include");


if(typeof require == 'function'){
exports.Core=Core;
exports.parseChildRemoveAttr=parseChildRemoveAttr;
var findELEnd=require('./el-util').findELEnd;
var findLiteParamMap=require('./el-util').findLiteParamMap;
var getLiteTagInfo=require('./xml').getLiteTagInfo;
var selectByXPath=require('./xml').selectByXPath;
var findXMLAttribute=require('./xml').findXMLAttribute;
var findXMLAttributeAsEL=require('./xml').findXMLAttributeAsEL;
var URI=require('./resource').URI;
var i18nHash=require('./resource').i18nHash;
var PLUGIN_DEFINE=require('./template-token').PLUGIN_DEFINE;
var XA_TYPE=require('./template-token').XA_TYPE;
var EL_TYPE=require('./template-token').EL_TYPE;
var XT_TYPE=require('./template-token').XT_TYPE;
}