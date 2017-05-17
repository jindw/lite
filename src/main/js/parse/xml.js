function loadLiteXML(uri,root,options){
    try{
        if(uri instanceof URI){ 
            if(uri.source){
                return parseXMLByText(uri.source.replace(/^[\s\ufeff]*/,uri,options))
            }else if(uri.scheme == 'lite'){
                var path = uri.path+(uri.query||'')+(uri.fragment || '');
                path = root.resolve(path.replace(/^\//,'./'))+'';
            }else{
                var path = String(uri);
            }
        }else{
            var path = String(uri);
        }
        if(/^[\s\ufeff]*[<#]/.test(path)){
            return parseXMLByText(path.replace(/^[\s\ufeff]*/,''),root,options)
        }else{
            //console.log(path,/^(?:\w+\:\/\/|\w\:\\|\/).*$/.test(path))
            if(/^(?:\w+\:\/\/|\w\:\\|\/).*$/.test(path)){
                var pos = path.indexOf('#')+1;
                var xpath = pos && path.substr(pos);
                var path = pos?path.substr(0,pos-1):path;
                var source = loadTextByPath(path.replace(/^file\:\/\/?/,''));
                var doc = parseXMLByText(source,uri,options);
                if(xpath && doc.nodeType){
                    doc = selectByXPath(doc,xpath);
                }
                return doc;
            }else{
                //文本看待
                return parseXMLByText(path,options);
            }
        }
    }catch(e){
        console.error("文档解析失败:"+uri,e)
        throw e;
    }
}
function txt2xml(source){
    return "<c:out c:xmlns='http://www.xidea.org/lite/core'><![CDATA["+
            source.replace(/^\ufeff?#.*[\r\n]*/, "").replace(/]]>/, "]]]]><![CDATA[>")+
            "]]></c:out>";
}
function addInst(xml,s){
    var p = /^\s*<\?(\w+)\s+(.*)\?>/;
    var m;
    var first = xml.firstChild;
    while(m = s.match(p)){
        if(m[1] == 'xml' && first.nodeType != 7){
            var pi = xml.createProcessingInstruction(m[1], m[2]);
            xml.insertBefore(pi, first);
        }
        s = s.substring(m[0].length);
    }
    return xml;
}

/**
 * @private
 */
function parseXMLByText(text,path,options){
	text = String(text)
    if(!/^[\s\ufeff]*</.test(text)){
        text = txt2xml(text);
    }
    try{
    	options = Object.create(options||{});//errorHandler
    	options.locator={systemId:path}
    	options.xmlns=defaultNSMap
        var doc = new DOMParser(options).parseFromString(text,"text/html")
        if(!doc.querySelectorAll){//init querySelector
			var elp = doc.documentElement.constructor.prototype;
			elp.querySelector = querySelector;
			elp.querySelectorAll = querySelectorAll;
			elp = doc.constructor.prototype;
			elp.querySelector = querySelector;
			elp.querySelectorAll = querySelectorAll;
        }
        //return doc;
        return addInst(doc,text);
    }catch(e){
        console.error("解析xml失败:",e,text);
    }
    
}

function loadTextByPath(path){
    var fs = require('fs');
    try{
    var text = fs.readFileSync(path,'utf-8');
    }catch(e){
    	return fs.readFileSync(decodeURI(path),'utf-8');
    }
    return text;
}
function selectorInit(node){
	var doc = node.ownerDocument||node;
	var nw = doc.nwmatcher;
	if(!nw){
		nw = doc.nwmatcher = nwmatcher({document:doc});
		nw.configure( { USE_QSAPI: false, VERBOSITY: true } );
	}
	return nw;
}
function querySelectorAll(selector){
	var nodes = selectorInit(this).select(selector,this);
	nodes.item = nodeListItem;
	return nodes;
}
function querySelector(selector){
	return selectorInit(this).first(selector,this)
}

function selectByXPath(currentNode,xpath){
    var nodes = xpathSelectNodes(currentNode,xpath);
    nodes.item = nodeListItem;
    return nodes;
}
function nodeListItem(i){
    return this[i];
}
function findXMLAttribute(el,key){
    if(el.nodeType == 2){
        return el.value;
    }
    try{
    //el
    var required = key.charAt() == '*';
    if(required){
        key = key.substr(1);
    }
    for(var i=1,len = arguments.length;i<len;i++){
        var an = arguments[i];
        if(an == '#text'){
            return el.textContent||el.text;
        }else{
            var v = el.getAttribute(an);//ie bug: no hasAttribute
            if(v || (typeof el.hasAttribute != 'undefined') && el.hasAttribute(an)){//ie bug
                if(i>1 && key.charAt(0) != '#'){
                    console.warn(el.tagName+" 标准属性名为："+key +'; 您采用的是：'+an);
                }
                return v;
            }
        }
    }
    if(required){
        console.error("标记："+el.tagName+"属性：'"+key +"' 为必要属性。");
    }
    }catch(e){
        console.error('findXMLAttribute error:',e)
    }
    return null;
}
function findXMLAttributeAsEL(el){
    el = findXMLAttribute.apply(null,arguments);
    if(el != null){
        var el2 = el.replace(/^\s*\$\{([\s\S]*)\}\s*$/,"$1")
        if(el == el2){
        	if(el2){
            	console.warn("缺少表达式括弧,文本将直接按表达式返回",el);
        	}
        }else{
            el2 = el2.replace(/^\s+|\s+$/g,'');
            if(!el2){
                console.warn("表达式内容为空:",el);
            }
            el = el2;
        }
    }
    return el;
}
function getLiteTagInfo(node){
    return node.lineNumber + ','+ node.columnNumber+'@'+node.ownerDocument.documentURI;
}

var defaultNSMap = {c:'http://www.xidea.org/lite/core',h:'http://www.xidea.org/lite/html-ext'}
var nwmatcher = require('./nwmatcher');
var URI=require('./resource').URI;
var DOMParser = require('xmldom').DOMParser;
var xpathSelectNodes = require('xpath.js');

exports.defaultNSMap = defaultNSMap;
exports.querySelector = querySelector;
exports.querySelectorAll = querySelectorAll;
exports.loadLiteXML=loadLiteXML;
exports.selectByXPath=selectByXPath;
exports.findXMLAttribute=findXMLAttribute;
exports.findXMLAttributeAsEL=findXMLAttributeAsEL;
exports.getLiteTagInfo = getLiteTagInfo;
