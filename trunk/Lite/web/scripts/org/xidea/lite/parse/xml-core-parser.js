/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var Core = {
	seek:function(text){
		var end = findELEnd(text,0);
		if(end>0){
			try{
				var el = text.substring(1,end);
				el = this.parseEL(el);
	            switch(this.getTextType()){
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
				$log.error("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
				return -1;
			}
		}else{
			$log.warn("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
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
				$log.error("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
				return -1;
			}
		}else{
			$log.warn("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
			return -1;
		}
		
	},
	seekxa:function(text){
		var end = findELEnd(text,0);
		if(end>0){
			try{
				var el = text.substring(1,end);
				if(/^\s*([\w\-]+|"[^"]+"|'[^']+')\s*\:/.test(el)){
					var map = findParamMap(el);
					for(var n in map){
						this.appendXA(n,map[n]);
					}
				}else{
					this.appendXA(null,el)
				}
		    	return end;
			}catch(e){
				$log.error("XML属性表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
				return -1;
			}
		}else{
			$log.warn("XML属性表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
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
				$log.error("XML文本表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
				return -1;
			}
		}else{
			$log.warn("XML文本表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
			return -1;
		}
	},
	parseExtension:function(node){
		var ns = getAttribute(node,'*namespace','ns');
		var file = getAttribute(node,'file');
		var pkg = getAttribute(node,'package');
		if(pkg){
			source = pkg;
		}else if(file){
			var source = this.loadText(this.createURI(file))+'\n';
		}else{
			var source = getAttribute(node,'#text')+'\n';
		}
		this.addExtension(ns,source);
	}
};
function addParser(fn){
	var i = arguments.length;
	while(--i){
		fn[0] && (Core['parse'+arguments[i]] = fn[0])
		fn[1] && (Core['before'+arguments[i]] = fn[1])
		fn[2] && (Core['seek'+arguments[i]] = fn[2])
	}
}


/**
 * node,attribute
 */
function processIf(node){
	var test = getAttributeEL(node,'*test','value');
    this.appendIf(test);
    processChild(this,node);
    this.appendEnd();
}

function seekIf(text){
	var end = findELEnd(text,0);
	if(end>0){
		this.appendIf(text.substring(1,end));
		return end;
	}
}
addParser([processIf,processIf,seekIf],'if')

function processElse(node){
    var test = getAttributeEL(node,'test','value');
    this.appendElse(test || null);
    processChild(this,node);
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
addParser([processElse,processElse,seekElse],'else');
function processElif(node){
    var test = getAttributeEL(node,'*test','value');
    this.appendElse(test || null);
    processChild(this,node);
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
addParser([processElif,processElif,seekElif],"elseif","elif");




function processChoose(node){
	var value = getAttributeEL(node,"value");
	var oldStatus = this.getAttribute(processChoose);
	this.setAttribute(processChoose,{value:value,first:true});
	processChild(this,node);
	this.setAttribute(processChoose,oldStatus);
}
//function seekChoose(text){
//	if(text.charAt() != '$'){
//		var end = findELEnd(text,-1);
//		if(end<=0){
//			$log.error('表达式异常')
//			return -1;
//		}else{
//			var value = text.substring(1,end);
//		}
//	}else{
//		end = 1;
//	}
//	var oldStatus = this.getAttribute(processChoose);
//	this.setAttribute(processChoose,{value:value,first:true});
//	processChild(this,node);
//	value && this.setAttribute(processChoose,oldStatus);
//	return end;
//}
addParser([processChoose,processChoose],"choose");
function processWhen(node){
	var stat = this.getAttribute(processChoose);
	var value = getAttributeEL(node,"*test","if");
	if(stat.value){
		value = stat.value + '=='+value;
	}
	if(stat.first){
		stat.first = false;
		this.appendIf(value);
	}else{
		this.appendElse(value);
	}
	processChild(this,node);
	this.appendEnd();
}

addParser([processWhen,processWhen],"when");
function processOtherwise(node){
	this.appendElse(null);
	processChild(this,node);
	this.appendEnd();
}
addParser([processOtherwise,processOtherwise],"otherwise");

var FOR_PATTERN = /\s*([\$\w_]+)\s*(?:,\s*([\w\$_]+))?\s*(?:\:|in)([\s\S]*)/;
function processFor(node){
	if(node.nodeType == 1){
    	var value = getAttributeEL(node,'*list','values','items','value');
    	var var_ = getAttribute(node,'*var','name','id','item');
    	var status_ = getAttribute(node,'status');
	}else{//attr
		var value = getAttributeEL(node);
		var match = value.match(FOR_PATTERN);
		if(!match){
			throw $log.error("非法 for 循环信息",value);
		}
		var var_ = match[1];
		var status_ =match[2];
		var value =match[3];
	}
    startFor(this,var_,value,status_ || null);
    processChild(this,node);
    this.appendEnd();
}
function seekFor(text){
	var end = findELEnd(text);
	if(end>0){
		var value = text.substring(1,end);
		var match = value.match(FOR_PATTERN);
		if(!match){
			throw $log.error("非法 for 循环信息",value);
		}
		var var_ = match[1];
		var status_ =match[2];
		var value =match[3];
		startFor(this,var_,value,status_ || null);
    	return end;
	}
}
function findForStart(list){
	var value = list;
	var dd=0;
	while(true){
		try{
			new Function("return "+value.replace(/\.\./g,'./.'));
			return dd;
		}catch(e){
			dd = list.indexOf('..',dd+1);
			if(dd>0){
				value = list.substring(0,dd)+'-'+list.substring(dd+2)
			}else{
				return dd;
			}
		}
	}
}
function startFor(context,key,list,status_){
	var dd = findForStart(list);
	if(dd>0){
		var begin = list.substring(0,dd);
		var end = list.substring(dd+2);
		if(findForStart(begin) !== 0){
			begin = begin.replace(/^\s*\[/,'');
			end = end.replace(/\]\s*$/,'');
			$log.info("range for 为非通用表达式，只能在for循环中使用");
		}else{
			$log.error("range for 表达式(非通用表达式)推荐模式为：[start,last]，您提供的表达式为"+list);
		}
		list = "Math.abs("+begin+'-'+end+")+1";
		context.appendFor(key,list,status_||null);
		context.appendVar(key,key+'+'+begin+"-1");
	}else if(dd ==0){
		context.appendFor(key,list,status_);
	}else{
		$log.error("for表达式无效："+list);
	}
}


addParser([processFor,processFor,seekFor],"for","foreach");

function processVar(node){
    var name_ = getAttribute(node,'*name','id');
	if(node.nodeType == 1){
		var value = getAttribute(node,'value');
	    if(value){
	    	var code = this.parseText(value,0);
	    	if(code.length == 1){
	    		code = code[0];
	    		if(code instanceof Array){
	    			this.appendVar(name_,code[1]);
	    		}else{
	    			$log.warn("标签:"+node.tagName+"的value属性"+value+"建议设置为表达式，您的输入没有表达式，系统自动按静态文本处理");
	    			this.appendVar(name_,stringifyJSON(code));
	    		}
	    	}else{
	    		this.appendCaptrue(name_);
		        this.appendAll(code)
		        this.appendEnd();
	    	}
	    }else{
	        this.appendCaptrue(name_);
	        processChild(this,node);
	        this.appendEnd();
	    }
	}else{
		var map = findParamMap(name_);
		if(map){
			for(var n in map){
				this.appendVar(n,map[n]);
			}
			processChild(this,node);
		}else{
	        this.appendCaptrue(name_);
	        processChild(this,node);
	        this.appendEnd();
		}
	}
}
function seekVar(text){
	var end = findELEnd(text);
	if(end>0){
		var value = text.substring(1,end);
		if(/^\s*(?:\w+|['"][^"]+['"])\s*$/.test(value)){
	        this.appendCaptrue(value.replace(/['"]/g,''));
		}else{
			var map = findParamMap(value);
			for(var n in map){
				this.appendVar(n,map[n]);
			}
		}
    	return end;
	}
}
addParser([processVar,processVar,seekVar],"var","set");


function parseOut(node){
    var value = getAttribute(node,"value","#text");
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
addParser([parseOut,parseOut,seekOut],"out");

function _parseDefName(name){
	var n = name;
	var i = n.indexOf(n);
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
					i=0;
					args = args.substring(i+1).replace(/^\s+|\s+$/g,'');
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
					$log.error("函数定义中参数表语法错误:"+arg,e);
					throw e;
				}
			}
			var p = arg.indexOf('=',i);
			if(p>0){
				params.push('"'+toid(arg.substring(0,p))+'"');
				defaults.push(arg.substring(p+1));
			}else{
				if(defaults.length){
					var msg = "函数定义中参数表语法错误:默认参数值能出现在参数表最后:"+name;
					log.error(msg);
					throw new Error(msg);
				}
				params.push('"'+toid(arg)+'"');
			}
			
			
		}
		
	}
	return ['{"name":"',n,
		'","params":[',params.join(','),
		'],"defaults":[',defaults.join(','),
		']}'].join('')
}
function toid(n){
	n = n.replace(/^\s+|\s+$/g,'');
	try{
		new Function("return "+n);
	}catch(e){
		$log.error("无效id:"+n,e);
		throw e;
	}
	return n;
}
function processDef(node){
    var ns = getAttribute(node,'*name');
    var el = _parseDefName(ns);
    this.appendPlugin(PLUGIN_DEFINE,this.parseEL(el));
    processChild(this,node);
    this.appendEnd();
}

function seekDef(text){
    var end = findELEnd(text);
	if(end>0){
		var ns = text.substring(1,end);
	    var el = _parseDefName(ns);
	    //prompt('',el.join(''))
	    this.appendPlugin(PLUGIN_DEFINE,this.parseEL(el));
    	return end;
	}
}

addParser([processDef,processDef,seekDef],"def",'macro');

function beforeInclude(attr){
	var match = attr.value.match(/^([^#]*)(?:#(.*))?$/);
	var path = match[1];
	var xpath = match[2];
	if(path){
		var path2 = path.replace(/^[\$#]([\w\$_]+)$/,'$$$1');
		if(path2.charAt() == '$') {
			doc = this.getAttribute(path);
		}else{
			var doc = this.loadXML(this.createURI(path))
		}
	}else{
		var doc = attr.ownerDocument;
	}
	if(doc==null){
		this.append("<strong style='color:red'>没找到包含节点："+this.currentURI+ attr.value+"</strong>");
	}else{
		var attrs = this.selectNodes(doc, xpath);
		var element = attr.ownerElement;
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
	var uri = doc && doc.documentURI
	if(uri){
		context.currentURI = context.createURI(uri);
	}
}
function parseInclude(node){
    var path = getAttribute(node,'path');
    var xpath = getAttribute(node,'xpath');
    var selector = getAttribute(node,'selector');
    var parentURI = this.currentURI;
	try{
		
	    if(path!=null){
	    	if(path.charAt() == '$'){
	    		doc = this.getAttribute(path);
	    		setNodeURI(this,node);
	    	}else{
		        var url = this.createURI(path);
	    		//$log.warn(path,this.currentURI+'',url+'')
		        var doc = this.loadXML(url);
		        this.currentURI = url;
	    	}
	    }else{
	    	var doc = node.ownerDocument
	    }
		if(doc==null){
			this.append("<strong style='color:red'>没找到包含节点："+this.currentURI+ node.value+"</strong>");
		}else{
		    if(xpath!=null){
		    	var d = doc;
		        doc = selectNodes(doc,xpath);
		        //alert([url,xpath,new XMLSerializer().serializeToString(d),doc.length])
		    }
		    if(selector != null){
		    	$log.warn("目前尚不支持css selector 选择节点");
		    }
		    this.parse(doc)
		}
    }finally{
        this.currentURI = parentURI;
    }
}

function processExtends(node){
	var oldConfig = this.getAttribute("#extends");
	var el = node.nodeType == 1?node:node.ownerElement;
	var root = el == el.ownerDocument.documentElement;
	var extendsConfig = {blockMap:{},parse:false,root:root};
	if(oldConfig){
		if(oldConfig.parse){//解析进行时
			if(root){//模板继承
				if(extendsConfig.root){
					this.reset(0);
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
	var parentURL = getAttribute(node,"*path","value","parent");
	//childNodes
	var url = this.createURI(parentURL);
	var parentNode = this.loadXML(url);
	if(!root){//元素继承
		parentNode = parentNode.documentElement;
	}
	var i = this.mark();
	 processChild(this,node);
	this.reset(i);
    var parentURI = this.currentURI;
	try{
		this.currentURI = url;
		extendsConfig.parse=true;
		this.parse(parentNode);
	}finally{
        this.currentURI = parentURI;
	}
	this.setAttribute("#extends" ,oldConfig);
}

function processBlock(node){
	var extendsConfig = this.getAttribute("#extends");
	var value = getAttribute(node,"name","id");
	var childNodes = node.nodeType == 1?node.childNodes:node.ownerElement;
		
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
	        		this.currentURI = parentURI;
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


function processClient(node){
	var context2 = this.createNew();
	// new ParseCothisontext.config,this.currentURI);
	var id = getAttribute(node,'*name','id');
	var translator = new JSTranslator(id);
	
	processChild(context2,node);
	var code = translator.translate(context2);
	this.append("<!--//--><script>//<![CDATA[\n"
				+code.replace(/<\/script>/ig,'<\\/script>')+"//]]></script>\n");
}


function processChild(context,node){
	if(node.nodeType == 1){//child
		context.parse(node.childNodes)
	}else if(node.nodeType == 2){//attr
		context.parse(node.ownerElement);//||node.selectSingleNode('parent::*'));
	}else {//other
		context.parse(node)
	}
}







//beforeset,parseout,parseforeach,parseInclude,parsemacro,parseset,parseelse,parseclient,parsechoose,parse,beforegroup,parseif,beforeelse,parseotherwise,beforeif,beforeotherwise,parseelif,beforeout,parsegroup,beforeforeach,parsewhen,beforeInclude,beforemacro,beforeelif,beforeclient,beforechoose,beforewhen,xmlns
 







addParser([parseInclude,beforeInclude],"include");
addParser([processClient,processClient],"client");
addParser([processBlock,processBlock],"block","group");
addParser([processExtends,processExtends],"extends","extend");

//addParsers(processXMLNS,"xmlns");
Core.seekEnd = function(text){
	this.appendEnd();
	return 0;
};
Core.xmlns = function(){};
Core.parse = function(node){
	$log.error("未支持标签：",node.tagName,node.ownerDocument && node.ownerDocument.documentURI)
};
