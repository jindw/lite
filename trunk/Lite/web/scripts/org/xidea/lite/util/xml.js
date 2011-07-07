function loadXML(uri,root){
	try{
		if(uri instanceof URI){
			if(uri.source){
				uri = uri.source;
			}else if(uri.scheme == 'lite'){
	    		var path = uri.path+(uri.query||'')+(uri.fragment || '');
	    		path = path.replace(/^\//,'./');
	    		//$log.warn(uri+'@'+path)
	    		uri = root.resolve(path)+'';
	    	}else{
	    		uri+='';
	    	}
		}
        if(/^[\s\ufeff]*[<#]/.test(uri)){
    	    var doc =parseXMLByText(uri.replace(/^[\s\ufeff]*/,''))
    	    //alert([data,doc.documentElement.tagName])
    	}else{
    		//print(url)
    		if(/^(?:https?\:\/\/|\/).*$/.test(uri)){
	    	    var pos = uri.indexOf('#')+1;
	    	    var xpath = pos && uri.substr(pos);
	    	    var uri = pos?uri.substr(0,pos-1):uri;
	    	    var doc = parseXMLByURL(uri);
	    	    if(xpath && doc.nodeType){
	    	        doc = selectNodes(doc,xpath);
	    	    }
    		}else{
    			//文本看待
    			return parseXMLByText(uri);
    		}
    	}
	}catch(e){
		$log.error("文档解析失败:"+uri,e)
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
	    var text2 = normalizeXML(text,url);
    	var errors = [];
    	var xml = parseFromString(text2,errors);
    	if(errors.length == 0 && xml){
			return xml;
		}
		var errors0 = [];
		var xml = parseFromString(text,errors0);
		if(errors0.length == 0 && xml){
			//report normalizeXML bug
			return xml;
		}
	    $log.error("解析xml失败:",errors.join('\n'),text2);
    }catch(e){
    	$log.error("解析xml失败:",e,text);
    }
    
}

var defaultEntryMap = {"&nbsp;": "&#160;","&copy;": "&#169;",'&':'&amp;','<':'&lt;'};
var defaultNSMap = {
	"xmlns:f": "http://www.xidea.org/lite/core",
	"xmlns:c": "http://www.xidea.org/lite/core",
	"xmlns": "http://www.w3.org/1999/xhtml"
};
function normalizeTag(source,tag,uri,pos){
	var i = 0;
	source = source.replace(/(\b[a-zA-Z_][\w_\-\.]*(?:\:[\w_][\w_\-\.]*)?)(?:\s*=\s*('[^']*'|\"[^\"]*\"|\$\{[^}]+\}|[^\s>]+))?/g,function(a,n,v){
		if(i==0){
			i++;
			tag.name = n;
			tag.attrMap = {};
			tag.nsMap = {};
			return n;
		}else{
			if(!v){
				v = '"'+n+'"'
			}else{
				v = v.replace(/&\w+;|&#\d+;|&#x[\da-fA-F]+;|[&<]/g,function(a){
					if(a in defaultEntryMap){
    					return defaultEntryMap[a];
    				}else{
    					return a;
    				}
				});
				var c = v.charAt();
				if(c != '"' && c != '\''){
					v= '"'+v.replace(/"/g,'&#34;')+'"';
				}
			}
			tag.attrMap[n]=v;
			if(/xmlns(?:\:.*)?/.test(n)){
				tag.nsMap[n] = v;
			}
			return n+'='+v
		}
	});
	for(var n in (tag.parentTag && tag.parentTag.nsMap || defaultNSMap)){
		tag.nsMap[n] = 1;
	}
	for(var n in tag.attrMap){
		if(/^xmlns\:/.test(n)){
			tag.nsMap[n] = 1;
		}
	}
	for(var n in tag.attrMap){
		if(n == 'xmlns'){
			pos += '|xmlns';
		}else if(/^(?:xml|xmlns)\:$/.test(n)){
			var n2 = n.replace(/^(.+)\:.+$/,"xmlns:$1");
			if( n2 !=n){
				pos += '|'+n;
				if(!(n2 in tag.nsMap)){
					$log.error("missed namespace:"+n2,source);
				}
			}
		}
	}
	pos+='|';
	var i = source.length-1;
	if(source.charAt(i-1) == '/'){
		i--;
	}
	var begin = source.substring(0,i);
	var end = source.substring(i);
	if(!tag.parentTag){
		pos+='@'+uri;
		for(var n in defaultNSMap){
			if(!(n in tag.attrMap)){
				begin = begin+' ' +n+'="'+defaultNSMap[n]+'"'
			}
		}
	}
	return begin+' c:__i="'+pos+'"'+end;
}
function normalizeXML(text,uri){
	var lines = text.split(/\r\n?|\n/);
	var text2 = lines.join('\n');
	var lineIndex = 0;
	var lineBase = 0;
	var rootCount = 0;
	var tag = null;
	var leaf = {
		'meta':1,'link':1,'img':1,'br':1,'hr':1,'input':1};
	
	function getPositionAttr(offset){
		while(lineBase+ lines[lineIndex].length<=offset){
			lineBase+= lines[lineIndex].length+1;
			lineIndex++;
		}
		offset -= lineBase;
		var pos = lineIndex+','+offset
		return pos;
	}
	//一个比较全面的容错。
	text2 = text2.replace(
    	//<\?\w+[\s\S]+?\?>|<!(?:[^>\[]+\[[\s\S]+\]>|[^>]+>)|<!\[CDATA\[[\s\S]+?\]\]>|<!--[\s\S]+?-->
    	//
    	///<[a-zA-Z_][\w_\-\.]*(?:\:[\w_][\w_\-\.]+)?(?:\s+[\w_](?:'[^']*'|\"[^\"]*\"|\$\{[^}]+\}|[^>'"$]+|[\$])*>|\s*\/?>)/,
    	//
    	//<\/[\w_][\w_\-\.]*(?:\:[\w_][\w_\-\.]+)?>
    	//
    	//&\w+;|&#\d+;|&#x[\da-fA-F]+;|[&<]
    	/(<\?\w+[\s\S]+?\?>|<!(?:[^>\[\-]+\[[\s\S]+\]>|[^>\[\-]+>)|<!\[CDATA\[[\s\S]+?\]\]>|<!--[\s\S]+?-->)|<([a-zA-Z_][\w_\-\.]*(?:\:[\w_][\w_\-\.]+)?)(?:\s+[\w_](?:'[^']*'|\"[^\"]*\"|\$\{[^}]+\}|[^>'"$]+|\$)*>|\s*\/?>)|(<\/[\w_][\w_\-\.]*(?:\:[\w_][\w_\-\.]+)?>)|&\w+;|&#\d+;|&#x[\da-fA-F]+;|[&<]/g,
    	function(a,notTag,startTag,endTag,offset){
    		if(notTag){
    			return a;
    		}else if(startTag){
    			if(tag == null){
    				rootCount++;
    			}
    			tag = {parentTag:tag};
    			var isClosed = false;
    			if(/\/>$/.test(a)){
    				isClosed = true;
    			}else{
    				if(startTag in leaf){//leaf
    					if(text.indexOf('</'+startTag+'>',offset)<0){
    						isClosed = true;
    						a = a.replace(/>$/,'/>');
    					}else{
    						delete leaf[startTag];
    					}
    				}
    			}
    			a = normalizeTag(a,tag,uri,getPositionAttr(offset));
    			if(isClosed){
    				tag = tag.parentTag;
    			}
    			return a;
    		}else if(endTag){
    			if(a.replace(/^<\/|>$/g,'') in leaf){
    				return '';
    			}
    			if(tag == null){
    				$log.error("未开始标签:"+a,text)
    			}else{
    				tag = tag.parentTag
    			}
    			return a;
    		}else if(a in defaultEntryMap){
    			return defaultEntryMap[a];
    		}
    		return a;
    	});
	//if(!text2.match(/\sxmlns\:c\b/)){
    //	text2 = text2.replace(/<[\w\-\.\:]+/,"$& xmlns:c='http://www.xidea.org/lite/core'");
	//}
	if(rootCount>1){
		text2 = text2.replace(/<[\w][\s\S]+/,"<c:block xmlns:c='http://www.xidea.org/lite/core'>$&</c:block>")
	}
    return text2;
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
	        var doc = new ActiveXObject("Msxml2.DOMDocument.3.0");
	        //var doc = new ActiveXObject("Microsoft.XMLDOM");
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
		var attribute = attributes[i];
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
function selectNodes(currentNode,xpath){
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
if(!(window.DOMParser && window.XMLHttpRequest || window.ActiveXObject)){
    var pu = Packages.org.xidea.lite.impl.ParseUtil;
    parseXMLByURL = parseXMLByText = function(url){
    	//TODO:data for text
    	url = $JSI.loadText&&$JSI.loadText(url) || url;
        if(/^[\s\ufeff]*</.test(url)){
        	return pu.loadXML(url);
        }else{
        	var pos = url.indexOf('#');
        	var xpath = pos>0 && url.substr(pos+1);
        	var url = pos>0?url.substr(0,pos):url;
        	var doc = pu.loadXML(url);
        	if(xpath){
		        doc = selectNodes(doc,xpath);
		    }
		    return doc;
        }
    }
    selectNodes = function(node,path){
        return pu.selectNodes(node,path);
    }
}
function getAttribute(el,key){
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
					$log.warn("标准属性名为："+key +'; 您采用的是：'+an);
				}
				return v;
			}
		}
	}
	if(required){
		$log.error("标记："+el.tagName+"属性：'"+key +"' 为必要属性。");
	}
	}catch(e){
		$log.error(e)
	}
	return null;
}
function getAttributeEL(el){
	el = getAttribute.apply(null,arguments);
	if(el !== null){
		var el2 = el.replace(/^\s*\$\{([\s\S]*)\}\s*$/,"$1")
		if(el == el2){
			$log.warn("缺少表达式括弧,文本将直接按表达式返回",el);
		}else{
			el2 = el2.replace(/^\s+|\s+$/g,'');
			if(!el2){
				$log.warn("表达式内容为空:",el);
			}
			el = el2;
		}
	}
	return el;
}
