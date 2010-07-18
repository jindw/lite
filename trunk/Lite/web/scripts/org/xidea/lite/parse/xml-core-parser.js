/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

//var TEMPLATE_NS_REG = /^http:\/\/www.xidea.org\/ns\/(?:template|lite)(?:\/core)?\/?$/;
//
//function isTemplateNS(tagName,namespaceURI){
//    return /^c\:|^xmlns\:c$/i.test(tagName) && (namespaceURI=="#" || namespaceURI=="#core" || namespaceURI ==null) || TEMPLATE_NS_REG.test(namespaceURI);
//}

//:core
var Core = {};
function addParsers(){
	var fn = arguments[0];
	var i = arguments.length;
	while(i-->1){
		Core[arguments[i]] = fn
	}
}
addParsers(processIfTag,"parseIf");
addParsers(processElseTag,"parseElse","parseElseIf","parseElseif","parseElif");

addParsers(processForTag,"parseFor","parseForeach","parseForEach");
addParsers(processVarTag,"parseVar","parseSet");
addParsers(processOutTag,"parseOut");
addParsers(processChooseTag,"parseChoose");
addParsers(processDefTag,"parseDef","parseMacro");
addParsers(processIncludeTag,"parseInclude");
addParsers(processClientTag,"parseClient");
addParsers(processBlockTag,"parseBlock","parseGroup");
addParsers(processXMLNS,"xmlns");
addParsers(function(node){
	$log.error("未支持标签：",node.tagName,node.ownerDocument.documentURI)
},"parse");
function processXMLNS(){
}
function processBlockTag(node,context,chain){
	var ns = node.namespaceURI;
	var value = getAttribute(node,"name","id");
	var cached = value && context.getAttribute("#" + value);
	var node = cached || node;
	context.parse(node.childNodes);
}
function processIfTag(node,context,chain){
    var test = getAttributeEL(node,'*test','value');
    context.appendIf(test);
    context.parse(node.childNodes)
    context.appendEnd();
}
function processElseTag(node,context,chain,requireTest){
    if(requireTest != false){
        var test = getAttributeEL(node,'test','value');
    }
    context.appendElse(test || null);
    context.parse(node.childNodes)
    context.appendEnd();
}

function processChooseTag(node,context,chain){
	var next = node.firstChild;
	var first = true;
	var whenTag = node.tagName.split(':')[0];
	var elseTag = whenTag + ':otherwise';
	whenTag += ':when';
    if(next){
        do{
        	if(next.tagName == whenTag){
        		if(first){
        			first = false;
        			processIfTag(next,context,chain);
        		}else{
		            processElseTag(next,context,chain,true);
        		}
        	}else if(next.tagName == elseTag){
        		processElseTag(next,context,chain,false);
        	}
		}while(next = next.nextSibling)
    }
}

function processForTag(node,context,chain){
    var next = node.firstChild;
    var items = getAttributeEL(node,'*list','values','items','value');
    var var_ = getAttribute(node,'*var','name','id','item');
    var status_ = getAttribute(node,'status');
    context.appendFor(var_,items,status_);
    if(next){
        do{
            context.parse(next)
        }while(next = next.nextSibling)
    }
    context.appendEnd();
}
function processVarTag(node,context,chain){
    var name = getAttribute(node,'*name','id');
    var value = getAttribute(node,'value');
    if(value){
    	var value = context.parseText(value,0);
    	if(value.length == 1){
    		value = value[0];
    		if(value instanceof Array){
    			context.appendVar(name,value[1]);
    		}else{
    			context.appendVar(name,stringifyJSON(value));
    		}
    	}else{
    		context.appendCaptrue(name);
	        context.appendAll(value)
	        context.appendEnd();
    	}
    }else{
        var next = node.firstChild;
        context.appendCaptrue(name);
        if(next){
            do{
                context.parse(next)
            }while(next = next.nextSibling)
        }
        context.appendEnd();
    }
}

function processOutTag(node,context,chain){
    var value = getAttribute(node,"value")||context.textContent;
    value = context.parseText(value,EL_TYPE);
    context.appendAll(value);
}


/**
 * 
 */
function processDefTag(node,context,chain){
    var next = node.firstChild;
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
    if(next){
        do{
            context.parse(next)
        }while(next = next.nextSibling)
    }
    context.appendEnd();
}
function processIncludeTag(node,context,chain){
    var path = getAttribute(node,'path');
    var xpath = getAttribute(node,'xpath');
    var doc = node.ownerDocument || node;
    var parentURI = context.currentURI;
	try{
	    if(path!=null){
	    	if(path.charAt() == '$'){
	    		doc = context['$'+name];
	    		context.currentURI = new URI(String(doc.documentURI));
	    	}else{
	    		
		        var url = context.createURI(path);
	    		//$log.warn(path,context.currentURI+'',url+'')
		        var doc = context.loadXML(url);
	    	}
	    }
	    if(xpath!=null){
	        doc = selectNodes(doc,xpath);
	    }
	    context.parse(doc)
    }finally{
        context.currentURI = parentURI;
    }
}

function processClientTag(node,context,chain){
	var c2 = new ParseContext(context.config,context.currentURI);
	var id = getAttribute(node,'*name','id');
	var translator = new Translator(id);
	c2.parse(node.childNodes);
	var code = translator.translate(c2);
	context.append("<!--//--><script>//<![CDATA[\n"
				+code.replace(/<\/script>/ig,'<\\/script>')+"//]]></script>\n");
}

