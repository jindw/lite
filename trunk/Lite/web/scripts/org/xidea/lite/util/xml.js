if(typeof require == 'function'){
var normalizeLiteXML=require('./xml-normalize').normalizeLiteXML;
var URI=require('./resource').URI;
var XMLHttpRequest=require('./xhr').XMLHttpRequest;
}function loadLiteXML(uri,root){
	try{
		if(uri instanceof URI){ 
			if(uri.source){
				uri = uri.source;
			}else if(uri.scheme == 'lite'){
	    		var path = uri.path+(uri.query||'')+(uri.fragment || '');
	    		path = path.replace(/^\//,'./');
	    		//console.warn(uri+'@'+path)
	    		uri = root.resolve(path)+'';
	    	}else{
	    		uri+='';
	    	}
		}
        if(/^[\s\ufeff]*[<#]/.test(uri)){
        	//console.info(uri)
    	    var doc =parseXMLByText(uri.replace(/^[\s\ufeff]*/,''))
    	    //alert([data,doc.documentElement.tagName])
    	}else{
    		//print(url)
    		if(/^(?:\w+?\:\/\/|\/).*$/.test(uri)){
	    	    var pos = uri.indexOf('#')+1;
	    	    var xpath = pos && uri.substr(pos);
	    	    var uri = pos?uri.substr(0,pos-1):uri;
	    	    var doc = parseXMLByURL(uri);
	    	    if(xpath && doc.nodeType){
	    	        doc = selectByXPath(doc,xpath);
	    	    }
    		}else{
    			//文本看待
    			return parseXMLByText(uri);
    		}
    	}
	}catch(e){
		console.error("文档解析失败:"+uri,e)
		throw e;
	}
	return doc;
}
function txt2xml(source){
	return "<out xmlns='http://www.xidea.org/lite/core'><![CDATA["+
			source.replace(/^\ufeff?#.*[\r\n]*/, "").replace(/]]>/, "]]]]><![CDATA[>")+
			"]]></out>";
}

/**
 * @private
 */
function parseXMLByURL(url){
	var xhr = new XMLHttpRequest();
    xhr.open("GET",url,false)
    xhr.send('');
    ////text/xml,application/xml...
    var xml = /\/x-lite-xml/.test(xhr.getResponseHeader("Content-Type")) && xhr.responseXML;//chrome 在content-type 为apllication/xml+xhtml时，responseXML为空
    if(xml && !getParserError(xml)){
    	return addInst(xml,xhr.responseText);
    }else{
    	return parseXMLByText(xhr.responseText,url)
    }
}
function addInst(xml,s){
    var p = /^\s*<\?(\w+)\s+(.*)\?>/;
	var m;
	var first = xml.firstChild;
	while(m = s.match(p)){
		if(m[1] == 'xml'){
			var pi = xml.createProcessingInstruction(m[1], m[2]);
			xml.insertBefore(pi, first);
		}
		s = s.substring(m[0].length);
	}
	return xml;
}
function getParserError(root,depth){
	if(root){
		if(depth === undefined){
			if(root.nodeType == 9){
				root = root.documentElement;
			}
			depth = 2;//webkit: html/body/parseerror
		}
		if(root.tagName == "parsererror"){
			if(this.XMLSerializer){
				return new XMLSerializer().serializeToString(root) || "xml error";
			}else{
				return root.xml || root.textContent || "xml error"
			}
		}
		if(depth>0){
			return getParserError(root.firstChild,depth-1)
		}
	}
	return false;
}
/**
 * @private
 */
function parseXMLByText(text,url){
	if(!/^[\s\ufeff]*</.test(text)){
		text = txt2xml(text);
	}
	try{
	    var text2 = normalizeLiteXML(text,url);
    	var errors = [];
    	var xml = parseFromString(text2,errors);
    	if(errors.length == 0 && xml){
			return xml;
		}
		var errors0 = [];
		var xml = parseFromString(text,errors0);
		if(errors0.length == 0 && xml){
			//report normalizeLiteXML bug
			return xml;
		}
	    console.error("解析xml失败:",errors.join('\n'),text2);
    }catch(e){
    	console.error("解析xml失败:",e,text);
    }
    
}

function parseFromString(text,errors){
	try{
		if(this.DOMParser){
	        var doc = new DOMParser().parseFromString(text,"text/xml");
	        var error = getParserError(doc);
	        if(error){//http://www.mozilla.org/newlayout/xml/parsererror.xml
	        	errors && errors.push(error);
	        	return null;
	        }
	    }else{
	        //["Msxml2.DOMDocument.6.0", "Msxml2.DOMDocument.3.0", "MSXML2.DOMDocument", "MSXML.DOMDocument", "Microsoft.XMLDOM"];
	        var doc = new ActiveXObject("Msxml2.DOMDocument");
	        //var doc = new ActiveXObject("Microsoft.XMLDOM");
	        doc.validateOnParse = false;
	        doc.loadXML(text);
	        if (doc.parseError.errorCode!=0){
	        	//todo....
	        	var error = doc.parseError.reason+";code:"+doc.parseError.errorCode;
	        	errors && errors.push(error);
	        }
	        doc.setProperty("SelectionLanguage", "XPath");
	    }
    }catch(e){
    	errors && errors.push(e);
    	return null;
    }
    return addInst(doc,text);
}
/**
 * @private
 */
function getNamespaceMap(node){
	var attributes = node.attributes;
	var map = {};
	for(var i = 0;i<attributes.length;i++){
		var attribute = attributes[i] ||attributes.item(i);
		var name = attribute.name;
		if(/^xmlns(:.*)?$/.test(name)){
			var value = attribute.value;
			var prefix = name.substr(6) || value.replace(/^.*\/([^\/]+)\/?$/,'$1');
			map[prefix] = value;
		}
	}
	return map;
}
function nodeListItem(i){
	return this[i];
}
/**
 * TODO:貌似需要importNode
 */
function selectByXPath(currentNode,xpath){
	var doc = currentNode.ownerDocument || currentNode;
    //var docFragment = doc.createDocumentFragment();
    var nsMap = getNamespaceMap(doc.documentElement);
    
    try{//ie
    	var buf = [];
    	for(var n in nsMap){
    		buf.push("xmlns:"+n+'="'+nsMap[n]+'"')
    	}
    	doc.setProperty("SelectionNamespaces",buf.join(' '));
    	doc.setProperty("SelectionLanguage","XPath");
        var nodes = currentNode.selectNodes(xpath);
//        var buf = [];
//        for (var i=0; i<nodes.length; i++) {
//            buf.push(nodes.item(i))
//        }
    }catch(e){
    	
        var xpe = doc.evaluate? doc: new XPathEvaluator();
        //var nsResolver = xpe.createNSResolver(doc.documentElement);
        var result = xpe.evaluate(xpath, currentNode, function(prefix){return nsMap[prefix]}, 5, null);
        var node;
        var nodes = [];
        
        while (node = result.iterateNext()){
            nodes.push(node);
        }
        nodes.item = nodeListItem;
    }
//    while (node = buf.shift()){
//        docFragment.appendChild(node.cloneNode(true));
//    }
    return nodes;
}
if(!(window.DOMParser && window.XMLHttpRequest || window.ActiveXObject) && window.Packages){
    var pu = Packages.org.xidea.lite.impl.ParseUtil;
    parseXMLByURL = parseXMLByText = function(url){
    	//TODO:data for text
        if(/^[\s\ufeff]*</.test(url)){
        	return pu.loadXML(url);
        }else{
        	var pos = url.indexOf('#');
        	var xpath = pos>0 && url.substr(pos+1);
        	var url = pos>0?url.substr(0,pos):url;
    		url = $JSI.loadText&&$JSI.loadText(url) || url;
        	var doc = pu.loadXML(url);
        	if(xpath){
		        doc = selectByXPath(doc,xpath);
		    }
		    return doc;
        }
    }
    selectByXPath = function(node,path){
        return pu.selectByXPath(node,path);
    }
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
					console.warn("标准属性名为："+key +'; 您采用的是：'+an);
				}
				return v;
			}
		}
	}
	if(required){
		console.error("标记："+el.tagName+"属性：'"+key +"' 为必要属性。");
	}
	}catch(e){
		console.error(e)
	}
	return null;
}
function findXMLAttributeAsEL(el){
	el = findXMLAttribute.apply(null,arguments);
	if(el !== null){
		var el2 = el.replace(/^\s*\$\{([\s\S]*)\}\s*$/,"$1")
		if(el == el2){
			console.warn("缺少表达式括弧,文本将直接按表达式返回",el);
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
//function getLiteOwnerElement(attr){
//	try{
//		if(attr.ownerElement){
//			return attr.ownerElement;
//		}
//		return attr.selectSingleNode("..");
//	}catch(e){
//		return null;
//	}
//}

if(typeof require == 'function'){
exports.loadLiteXML=loadLiteXML;
exports.selectByXPath=selectByXPath;
exports.findXMLAttribute=findXMLAttribute;
exports.findXMLAttributeAsEL=findXMLAttributeAsEL;
}