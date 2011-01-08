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
				el = this.parseEL(el);
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
function processIf(node,context,chain){
	var test = getAttributeEL(node,'*test','value');
    context.appendIf(test);
    processChild(context,node);
    context.appendEnd();
}

function seekIf(text){
	var end = findELEnd(text,0);
	if(end>0){
		this.appendIf(text.substring(1,end));
		return end;
	}
}
addParser([processIf,processIf,seekIf],'if')

function processElse(node,context,chain){
    var test = getAttributeEL(node,'test','value');
    context.appendElse(test || null);
    processChild(context,node);
    context.appendEnd();
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
function processElif(node,context,chain){
    var test = getAttributeEL(node,'*test','value');
    context.appendElse(test || null);
    processChild(context,node);
    context.appendEnd();
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




function processChoose(node,context,chain){
	var value = getAttributeEL(node,"value");
	var oldStatus = context.getAttribute(processChoose);
	context.setAttribute(processChoose,{value:value,first:true});
	processChild(context,node);
	context.setAttribute(processChoose,oldStatus);
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
//	var oldStatus = context.getAttribute(processChoose);
//	context.setAttribute(processChoose,{value:value,first:true});
//	processChild(context,node);
//	value && context.setAttribute(processChoose,oldStatus);
//	return end;
//}
addParser([processChoose,processChoose],"choose");
function processWhen(node,context,chain){
	var stat = context.getAttribute(processChoose);
	var value = getAttributeEL(node,"*test","if");
	if(stat.value){
		value = stat.value + '=='+value;
	}
	if(stat.first){
		stat.first = false;
		context.appendIf(value);
	}else{
		context.appendElse(value);
	}
	processChild(context,node);
	context.appendEnd();
}

addParser([processWhen,processWhen],"when");
function processOtherwise(node,context,chain){
	context.appendElse(null);
	processChild(context,node);
	context.appendEnd();
}
addParser([processOtherwise,processOtherwise],"otherwise");

var FOR_PATTERN = /\s*([\$\w_]+)\s*(?:,\s*([\w\$_]+))?\s*(?:\:|in)([\s\S]*)/;
function processFor(node,context,chain){
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
    startFor(context,var_,value,status_ || null);
    processChild(context,node);
    context.appendEnd();
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

function processVar(node,context,chain){
    var name_ = getAttribute(node,'*name','id');
	if(node.nodeType == 1){
		var value = getAttribute(node,'value');
	    if(value){
	    	var code = context.parseText(value,0);
	    	if(code.length == 1){
	    		code = code[0];
	    		if(code instanceof Array){
	    			context.appendVar(name_,code[1]);
	    		}else{
	    			$log.warn("标签:"+node.tagName+"的value属性"+value+"建议设置为表达式，您的输入没有表达式，系统自动按静态文本处理");
	    			context.appendVar(name_,stringifyJSON(code));
	    		}
	    	}else{
	    		context.appendCaptrue(name_);
		        context.appendAll(code)
		        context.appendEnd();
	    	}
	    }else{
	        context.appendCaptrue(name_);
	        processChild(context,node);
	        context.appendEnd();
	    }
	}else{
		var map = findParamMap(name_);
		if(map){
			for(var n in map){
				context.appendVar(n,map[n]);
			}
			processChild(context,node);
		}else{
	        context.appendCaptrue(name_);
	        processChild(context,node);
	        context.appendEnd();
		}
	}
}
function seekVar(text){
	var end = findELEnd(text);
	if(end>0){
		var value = text.substring(1,end);
		var map = findParamMap(value);
		if(map){
			for(var n in map){
				this.appendVar(n,map[n]);
			}
		}else{
	        this.appendCaptrue(value);
		}
    	return end;
	}
}
addParser([processVar,processVar,seekVar],"var","set");


function parseOut(node,context,chain){
    var value = getAttribute(node,"value","#text");
    value = context.parseText(value,EL_TYPE);
    context.appendAll(value);
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


function processDef(node,context,chain){
    var ns = getAttribute(node,'*name');
    ns = (ns.replace(/^\s+/,'')+'{end').split(/[^\w]+/);
    ns.pop();
    var el = ['{"name":"',ns[0],'","params":['];
    for(var i=1;i<ns.length;i++){
    	if(i>1){
    		el.push(",")
    	}
    	el.push('"',ns[i],'"');
    }
    el.push("]}")
    //prompt('',el.join(''))
    context.appendPlugin(PLUGIN_DEFINE,context.parseEL(el.join('')));
    processChild(context,node);
    context.appendEnd();
}

function seekDef(text){
    var end = findELEnd(text);
	if(end>0){
		var ns = text.substring(1,end);
	    ns = (ns.replace(/^\s+/,'')+'{end').split(/[^\w]+/);
	    ns.pop();
	    var el = ['{"name":"',ns[0],'","params":['];
	    for(var i=1;i<ns.length;i++){
	    	if(i>1){
	    		el.push(",")
	    	}
	    	el.push('"',ns[i],'"');
	    }
	    el.push("]}")
	    //prompt('',el.join(''))
	    this.appendPlugin(PLUGIN_DEFINE,this.parseEL(el.join('')));
    	return end;
	}
}

addParser([processDef,processDef,seekDef],"def",'macro');

function beforeInclude(attr, context,chain){
	var match = attr.value.match(/^([^#]*)(?:#(.*))?$/);
	var path = match[1];
	var xpath = match[2];
	if(path){
		var path2 = path.replace(/^[\$#]([\w\$_]+)$/,'$$$1');
		if(path2.charAt() == '$') {
			doc = context.getAttribute(path);
		}else{
			var doc = context.loadXML(context.createURI(path))
		}
	}else{
		var doc = attr.ownerDocument;
	}
	if(doc==null){
		context.append("<strong style='color:red'>没找到包含节点："+context.currentURI+ attr.value+"</strong>");
	}else{
		var attrs = context.selectNodes(doc, xpath);
		var element = attr.ownerElement;
		//element.removeAttributeNode(attr)
		for(var i = attrs.length;i--;){
			var a = attrs.item(i);
			mergeAttribute(element,a);
		}
		chain.process(element);
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
function parseInclude(node,context,chain){
	if(node.nodeType == 2){
		$log.warn("暂不支持属性包含")
	}
    var path = getAttribute(node,'path');
    var xpath = getAttribute(node,'xpath');
    var selector = getAttribute(node,'selector');
    var parentURI = context.currentURI;
	try{
		
	    if(path!=null){
	    	if(path.charAt() == '$'){
	    		doc = context.getAttribute(path);
	    		setNodeURI(context,node);
	    	}else{
		        var url = context.createURI(path);
	    		//$log.warn(path,context.currentURI+'',url+'')
		        var doc = context.loadXML(url);
		        context.currentURI = url;
	    	}
	    }else{
	    	var doc = node.ownerDocument
	    }
		if(doc==null){
			context.append("<strong style='color:red'>没找到包含节点："+context.currentURI+ node.value+"</strong>");
		}else{
		    if(xpath!=null){
		        doc = selectNodes(doc,xpath);
		    }
		    if(selector != null){
		    	$log.warn("目前尚不支持css selector 选择节点");
		    }
		    context.parse(doc)
		}
    }finally{
        context.currentURI = parentURI;
    }
}

function processExtends(node,context,chain){
	var oldConfig = context.getAttribute("#extends");
	var el = node.nodeType == 1?node:node.ownerElement;
	var root = el == el.ownerDocument.documentElement;
	var extendsConfig = {blockMap:{},parse:false,root:root};
	if(oldConfig){
		if(oldConfig.parse){//解析进行时
			if(root){//模板继承
				if(extendsConfig.root){
					context.reset(0);
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
	
	context.setAttribute("#extends" ,extendsConfig);
	var parentURL = getAttribute(node,"*path","value","parent");
	//childNodes
	var url = context.createURI(parentURL);
	var parentNode = context.loadXML(url);
	if(!root){//元素继承
		parentNode = parentNode.documentElement;
	}
	var i = context.mark();
	 processChild(context,node);
	context.reset(i);
    var parentURI = context.currentURI;
	try{
		context.currentURI = url;
		extendsConfig.parse=true;
		context.parse(parentNode);
	}finally{
        context.currentURI = parentURI;
	}
	context.setAttribute("#extends" ,oldConfig);
}

function processBlock(node,context,chain){
	var extendsConfig = context.getAttribute("#extends");
	var value = getAttribute(node,"name","id");
	var childNodes = node.nodeType == 1?node.childNodes:node.ownerElement;
		
	if(extendsConfig){//
		var blockMap = extendsConfig.blockMap;
		var cached = value && (value in blockMap) && blockMap[value];
		if(extendsConfig.parse){
			if(cached){
				var parentURI = context.currentURI;
				try{
					//set current uri
					setNodeURI(context,cached);
					extendsConfig.parse=true;
					context.parse(cached);
				}finally{
	        		context.currentURI = parentURI;
				}
			}else{
				context.parse(childNodes);
			}
		}else{
			if(!cached){
				blockMap[value] = childNodes;
			}
		}
	}else{
		context.parse(childNodes);
	}
}


function processClient(node,context,chain){
	var context2 = context.createNew();
	// new ParseContext(context.config,context.currentURI);
	var id = getAttribute(node,'*name','id');
	var translator = new Translator(id);
	
	processChild(context2,node);
	var code = translator.translate(context2);
	context.append("<!--//--><script>//<![CDATA[\n"
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
