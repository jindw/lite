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
        if(/^[\s\ufeff]*</.test(uri)){
    	    var doc =parseXMLByText(uri.substring(uri.indexOf('<')))
    	    //alert([data,doc.documentElement.tagName])
    	}else{
    		//print(url)
    	    var pos = uri.indexOf('#')+1;
    	    var xpath = pos && uri.substr(pos);
    	    var uri = pos?uri.substr(0,pos-1):uri;
    	    var doc = parseXMLByURL(uri);
    	    if(xpath){
    	        doc = selectNodes(doc,xpath);
    	    }
    	}
	}catch(e){
		$log.error("文档解析失败:"+uri,e)
		throw e;
	}
	return doc;
}
/**
 * @private
 */
function parseXMLByURL(url){
	var xhr = new XMLHttpRequest();
    xhr.open("GET",url,false)
    xhr.send('');
    ////text/xml,application/xml...
    var xml = /\/xml/.test(xhr.getResponseHeader("Content-Type")) && xhr.responseXML;//chrome 在content-type 为apllication/xml+xhtml时，responseXML为空
    return xml || parseXMLByText(xhr.responseText)
}
/**
 * @private
 */
function parseXMLByText(text){
	try{
		if(this.DOMParser){
	        var doc = new DOMParser().parseFromString(text,"text/xml");
	        var root = doc.documentElement;
	        if(root.tagName == "parsererror"){
	        	var s = new XMLSerializer();
	        	$log.error("解析xml失败",s.serializeToString(root))
	        }
	    }else{
	        //["Msxml2.DOMDocument.6.0", "Msxml2.DOMDocument.3.0", "MSXML2.DOMDocument", "MSXML.DOMDocument", "Microsoft.XMLDOM"];
	        var doc = new ActiveXObject("Microsoft.XMLDOM");
	        doc.loadXML(text);
	        doc.documentElement.tagName;
	    }
	    
	    return doc;
    }catch(e){
    	$log.error("解析xml失败",e,text);
    	throw e;
    }
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

/**
 * TODO:貌似需要importNode
 */
function selectNodes(currentNode,xpath){
	var doc = currentNode.ownerDocument || currentNode;
    var docFragment = doc.createDocumentFragment();
    var nsMap = getNamespaceMap(doc.documentElement);
    try{//ie
    	var buf = [];
    	for(var n in nsMap){
    		buf.push("xmlns:"+n+'="'+nsMap[n]+'"')
    	}
    	doc.setProperty("SelectionNamespaces",buf.join(' '));
    	doc.setProperty("SelectionLanguage","XPath");
        var nodes = currentNode.selectNodes(xpath);
        var buf = [];
        for (var i=0; i<nodes.length; i++) {
            buf.push(nodes.item(i))
        }
    }catch(e){
        var xpe = doc.evaluate? doc: new XPathEvaluator();
        //var nsResolver = xpe.createNSResolver(doc.documentElement);
        var result = xpe.evaluate(xpath, currentNode, function(prefix){return nsMap[prefix]}, 5, null);
        var node;
        var buf = [];
        while (node = result.iterateNext()){
            buf.push(node);
        }
    }
    while (node = buf.shift()){
        docFragment.appendChild(node.cloneNode(true));
    }
    return docFragment;
}
if(!(window.DOMParser && window.XMLHttpRequest || window.ActiveXObject)){
    var pu = Packages.org.xidea.lite.impl.ParseUtil;
    parseXMLByURL = parseXMLByText = function(url){
    	//TODO:data for text
    	url = $JSI.loadText&&$JSI.loadText(url) || url;
        if(/^[\s\ufeff]*</.test(url)){
        	return pu.loadXML(url,null);
        }else{
        	var pos = url.indexOf('#')+1;
        	var xpath = pos && url.substr(pos);
        	var url = pos?url.substr(0,pos-1):url;
        	var doc = pu.loadXML(url,null);
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
	//el
	var required = key.charAt() == '*';
	if(required){
		key = key.substr(1);
	}
	for(var i=1,len = arguments.length;i<len;i++){
		var an = arguments[i];
		if(an == '#text'){
			return el.textContent;
		}else if(el.hasAttribute(an)){
			if(i>1 && key.charAt(0) != '#'){
				$log.warn("标准属性名为：",key ,'您采用的是：',an);
			}
			return el.getAttribute(an);
		}
	}
	if(required){
		$log.error("属性：",key ,'为必要属性。');
	}
	return null;
}
function getAttributeEL(el){
	el = getAttribute.apply(null,arguments);
	if(el !== null){
		var el2 = el.replace(/^\s*\$\{([\s\S]+)\}\s*$/,"$1")
		if(el == el2){
			$log.warn("缺少表达式括弧,文本将直接按表达式返回");
		}
		el = el2;
	}
	return el;
}
