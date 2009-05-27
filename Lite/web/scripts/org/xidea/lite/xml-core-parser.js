/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

var TEMPLATE_NS_REG = /^http:\/\/www.xidea.org\/ns\/(?:template|lite)(?:\/core)?\/?$/;

function isTemplateNS(tagName,namespaceURI){
    return /^c\:|^xmlns\:c$/i.test(tagName) && (namespaceURI=="#" || namespaceURI=="#core" || namespaceURI ==null) || TEMPLATE_NS_REG.test(namespaceURI);
}

//:core
function parseCoreNode(node,context,chain){//for
    switch(node.nodeType){
    case 1:
        var tagName = node.tagName.toLowerCase();
        if(isTemplateNS(tagName,node.namespaceURI)){
            switch(tagName.substr(2)){
            case 'if':
                parseIfTag(node,context,chain);
                break;
            case 'elseif':
            case 'else-if':
            case 'else':
                parseElseIfTag(node,context,chain);
                break;
            case 'for':
            case 'foreach':
                parseForTag(node,context,chain);
                break;
            case 'set':
            case 'var':
                parseVarTag(node,context,chain);
                break;
            case 'out':
                parseOutTag(node,context,chain);
                break;
            case 'choose':
                parseChooseTag(node,context,chain);
                break;
            case 'when':
            case 'otherwise':
                break;
            case 'def':
            case 'macro':
            	parseDefTag(node,context,chain);
                break;
            //for other
            case 'include':
                processIncludeTag(node,context,chain);
                break;
            default:
                $log.error("未知标签：",tagName,node.ownerDocument.documentURI)
            }
        }else{
        	chain.process(node);
        }
        break;
    case 2: //NODE_ATTRIBUTE  
	    if(!isTemplateNS(node.name,node.value)){
			chain.process(node);
		}
		break;
    default:
        chain.process(node);
    }
    
}
/**
 * 
 */
function parseDefTag(node,context,chain){
    var next = node.firstChild;
    var ns = getAttribute(context,node,'name',false,true);
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
    context.appendAdvice("#def",context.parseEL(el.join('')));
    if(next){
        do{
            context.parse(next)
        }while(next = next.nextSibling)
    }
    context.appendEnd();
}
function processIncludeTag(node,context,chain){
    var var_ = getAttribute(context,node,'var');
    var path = getAttribute(context,node,'path');
    var xpath = getAttribute(context,node,'xpath');
    var name = getAttribute(context,node,'name');
    var doc = node.ownerDocument || node;
    var parentURL = context.url;
	try{
		if(name){
			var docFragment = doc.createDocumentFragment();
			var next = node.firstChild;
            if(next){
                do{
                    docFragment.appendChild(next)
                }while(next = next.nextSibling)
            }
            context['#'+name] = docFragment;
		}
	    if(var_){
            var next = node.firstChild;
            context.appendVar(var_);
            if(next){
                do{
                    context.parse(next)
                }while(next = next.nextSibling)
            }
            context.appendEnd();
	    }
	    if(path!=null){
	    	if(path.charAt() == '#'){
	    		doc = context['#'+name];
	    		context.url = doc.documentURI;
	    	}else{
		        var url = parentURL?parentURL.replace(/[^\/]*(?:[#\?].*)?$/,path):path;
		        var doc = context.loadXML(url);
	    	}
	    }
	    if(xpath!=null){
	        doc = selectNodes(doc,xpath);
	    }
	    context.parse(doc)
    }finally{
        context.url = parentURL;
    }
}
function parseIfTag(node,context,chain){
    var next = node.firstChild;
    var test = getAttribute(context,node,'test',true,true);
    context.appendIf(test);
    if(next){
        do{
            context.parse(next)
        }while(next = next.nextSibling)
    }
    context.appendEnd();
}

function parseElseIfTag(node,context,chain,requireTest){
    var next = node.firstChild;
    if(requireTest != false){
        var test = getAttribute(context,node,'test',true,requireTest == true);
    }
    if(test){
    	context.appendElse(test);
    }else{
    	context.appendElse();
    }
    if(next){
        do{
            context.parse(next)
        }while(next = next.nextSibling)
    }
    context.appendEnd();
}

function parseChooseTag(node,context,chain){
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
        			parseIfTag(next,context,chain);
        		}else{
		            parseElseIfTag(next,context,chain,true);
        		}
        	}else if(next.tagName == elseTag){
        		parseElseIfTag(next,context,chain,false);
        	}
		}while(next = next.nextSibling)
    }
}

function parseForTag(node,context,chain){
    var next = node.firstChild;
    var items = getAttribute(context,node,'items',true);
    var var_ = getAttribute(context,node,'var');
    var status_ = getAttribute(context,node,'status');
    
    context.appendFor(var_,items,status_);
    if(next){
        do{
            context.parse(next)
        }while(next = next.nextSibling)
    }
    context.appendEnd();
}
function parseVarTag(node,context,chain){
    var name = getAttribute(context,node,'name');
    var value = getAttribute(context,node,'value');
    if(value){
    	var value = context.parseText(value,false);
    	if(value.length == 1){
    		value = value[0];
    		if(value instanceof Array){
    			value = value[1];
    		}
    		context.appendVar(name,value);
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

function parseOutTag(node,context,chain){
    var value = getAttribute(context,node,"value");
    value = context.parseText(value,EL_TYPE);
    context.appendAll(value);
}



/**
 * @internal
 */
var stringRegexp = /["\\\x00-\x1f\x7f-\x9f]/g;
/**
 * 转义替换字符
 * @internal
 */
var charMap = {
    '\b': '\\b',
    '\t': '\\t',
    '\n': '\\n',
    '\f': '\\f',
    '\r': '\\r',
    '"' : '\\"',
    '\\': '\\\\'
};

/**
 * 转义替换函数
 * @internal
 */
function charReplacer(item) {
    var c = charMap[item];
    if (c) {
        return c;
    }
    c = item.charCodeAt().toString(16);
    return '\\u00' + (c.length>1?c:'0'+c);
}

function getAttribute(context,node,key,isEL,required){
	var value = node.getAttribute(key);
	if(value){
		value = String(value);
		if(isEL){
	         return findFirstEL(context,value);
		}else{
			return value.replace(/^\s+|\s+$/g,'');
		}
	}else if(required){
		$log.error("属性"+key+"为必须值");
		throw new Error();
	}
}
function findFirstEL(context,value){
	var els = context.parseText(value,EL_TYPE);
	var i = els.length;
	while(i--) {
		var el = els[i];
		if(el instanceof Array){//el
		    return el[1];
		}
	}
}
