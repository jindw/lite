/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */


function processIf(node,context,chain){
	var test = getAttributeEL(node,'*test','value');
    context.appendIf(test);
    processChild(context,node);
    context.appendEnd();
}
function processElse(node,context,chain){
    var test = getAttributeEL(node,'test','value');
    context.appendElse(test || null);
    processChild(context,node);
    context.appendEnd();
}
function processElif(node,context,chain){
    var test = getAttributeEL(node,'*test','value');
    context.appendElse(test || null);
    processChild(context,node);
    context.appendEnd();
}

function processChoose(node,context,chain){
	var value = getAttributeEL(node,"value");
	var oldStatus = context.getAttribute(processChoose);
	context.setAttribute(processChoose,{value:value,first:true});
	processChild(context,node);
	context.setAttribute(processChoose,oldStatus);
}
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

function processOtherwise(node,context,chain){
	context.appendElse(null);
	processChild(context,node);
	context.appendEnd();
}
function processFor(node,context,chain){
	if(node.nodeType == 1){
    	var value = getAttributeEL(node,'*list','values','items','value');
    	var var_ = getAttribute(node,'*var','name','id','item');
    	var status_ = getAttribute(node,'status');
	}else{//attr
		var value = getAttributeEL(node);
		var match = value.match(/\s*([\$\w_]+)\s*(?:,\s*([\w\$_]+))?\s*(?:\:|in)([\s\S]*)/);
		if(!match){
			throw $log.error("非法 for 循环信息",value);
		}
		var var_ = match[1];
		var status_ =match[2];
		var value =match[3];
	}
	$log.info(value)
    startFor(context,var_,value,status_ || null);
    processChild(context,node);
    context.appendEnd();
}
function findForStart(list){
	var value = list;
	var dd=0;
	while(true){
		try{
			new Function("return "+value);
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
		context.appendVar(key,key+'+'+(begin-1));
	}else if(dd ==0){
		context.appendFor(key,list,status_);
	}else{
		$log.error("for表达式无效："+list);
	}
}

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

function processOut(node,context,chain){
	if(node.nodeType != 1){
		$log.error("out 标签不支持属性模式");
		processChild(context,node);
		return;
	}
    var value = getAttribute(node,"value","#text");
    value = context.parseText(value,EL_TYPE);
    context.appendAll(value);
}


/**
 * 
 */
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
	    		context.currentURI = context.createURI(String(doc.documentURI));
	    	}else{
	    		
		        var url = context.createURI(path);
	    		//$log.warn(path,context.currentURI+'',url+'')
		        var doc = context.loadXML(url);
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

function processBlock(node,context,chain){
	var ns = node.namespaceURI;
	var value = getAttribute(node,"name","id");
	var cached = value && context.getAttribute("#" + value);
	if(cached){
		node = cached;
	}else if(node.nodeType == 2){
		node = node.ownerElement;
	}else{
		node = node.childNodes;
	}
	processChild(context,node);
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


function processChild(context,node,chain){
	if(node.nodeType == 2){
		context.parse(node.ownerElement);
	}else{
		context.parse(node.childNodes)
	}
}
var Core = {};
function addParsers(fn){
	var i = arguments.length;
	while(i){
		var tn = arguments[--i];
		Core['parse'+tn] = fn
		Core['before'+tn] = fn
	}
}
//beforeset,parseout,parseforeach,parseInclude,parsemacro,parseset,parseelse,parseclient,parsechoose,parse,beforegroup,parseif,beforeelse,parseotherwise,beforeif,beforeotherwise,parseelif,beforeout,parsegroup,beforeforeach,parsewhen,beforeInclude,beforemacro,beforeelif,beforeclient,beforechoose,beforewhen,xmlns

addParsers(processIf,'if');
addParsers(processElse,"else");
addParsers(processElif,"elseif","elif");

addParsers(processChoose,"choose");
addParsers(processWhen,"when");
addParsers(processOtherwise,"otherwise");

addParsers(processFor,"for","foreach");
addParsers(processVar,"var","set");

addParsers(processDef,"def","macro");

addParsers(processOut,"out");
Core.parseInclude =  parseInclude;
Core.beforeInclude =  beforeInclude;
addParsers(processClient,"client");
addParsers(processBlock,"block","group");
//addParsers(processXMLNS,"xmlns");
Core.xmlns = function(){};
//Core.parse = function(node){
//	$log.error("未支持标签：",node.tagName,node.ownerDocument.documentURI)
//};