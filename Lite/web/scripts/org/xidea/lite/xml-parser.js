/*
 * JavaScript Integration Framework
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/jsi/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
//parse
//add as default
function XMLParser(nativeJS){
    this.nativeJS = nativeJS;
    this.parserList = this.parserList.concat([]);
    this.result = [];
}

function isTemplateNS(value,shortAvaliable){
    return shortAvaliable && (value=="#" || value=="#core" || value ==null) || TEMPLATE_NS_REG.test(value);
}
XMLParser.prototype = new TextParser()
XMLParser.prototype.parse = function(url){
    if(/^[\s\ufeff]*</.test(url)){
        var data =toDoc(url)
        //alert([data,doc.documentElement.tagName])
    }else{
        var pos = url.indexOf('#')+1;
        var data = this.load( pos?url.substr(0,pos-1):url,pos && url.substr(pos));
    }
    this.parseNode(data);
    return this.reuslt;
}
XMLParser.prototype.load = function(url,xpath){
	try{
	    var xhr = new XMLHttpRequest();
	    xhr.open("GET",url,false)
	    xhr.send('');
	    if(/\/xml/.test(xhr.getResponseHeader("Content-Type"))){//text/xml,application/xml...
	        var doc = xhr.responseXML;
	    }else{
	        var doc = toDoc(xhr.responseText)
	    }
	    if(xpath){
	        doc = selectNodes(doc,xpath);
	    }
	    this.url = url;
		return doc;
	}catch(e){
		$log.error("文档解析失败",url,e)
		throw e;
	}
}
/**
 * 解析函数集
 * @private
 */
XMLParser.prototype.addParser(function(node){
    switch(node.nodeType){
        //case 1: //NODE_ELEMENT 
        //    return parseElement.call(this,node)
        case 2: //NODE_ATTRIBUTE                             
            return parseAttribute.call(this,node)
        case 3: //NODE_TEXT                                        
            return parseTextNode.call(this,node)
        case 4: //NODE_CDATA_SECTION                     
            return parseCDATA.call(this,node)
        case 5: //NODE_ENTITY_REFERENCE                
            return parseEntityReference.call(this,node)
        case 6: //NODE_ENTITY            
            return parseEntity.call(this,node)
        case 7: //NODE_PROCESSING_INSTRUCTION    
            return parseProcessingInstruction.call(this,node)
        case 8: //NODE_COMMENT                                 
            return parseComment.call(this,node)
        case 9: //NODE_DOCUMENT                                
        case 11://NODE_DOCUMENT_FRAGMENT             
            return parseDocument.call(this,node)
        case 10://NODE_DOCUMENT_TYPE                     
            return parseDocumentType.call(this,node)
        //case 11://NODE_DOCUMENT_FRAGMENT             
        //    return parseDocumentFragment.call(this,node)
        case 12://NODE_NOTATION 
            return parseNotation.call(this,node)
        default://文本节点
            //this.println("<!-- ERROR： UNKNOW nodeType:"+node.nodeType+"-->")
    }
    return node;
});


var htmlLeaf = /^(?:meta|link|img|br|hr)$/i;
var scriptTag = /^script$/i
XMLParser.prototype.addParser(function(node){
    if(node.nodeType ==1){
        var attributes = node.attributes;
        this.append('<'+node.tagName);
        for (var i=0; i<attributes.length; i++) {
            try{
                //htmlunit bug...
                var attr = attributes.item(i);
            }catch(e){
                var attr =attributes[i];
            }
            this.parseNode(attr)
        }
        if(htmlLeaf.test(node.tagName)){
            this.append('/>')
            return true;
        }
        this.append('>')
        var child = node.firstChild
        if(child){
            do{
                this.parseNode(child)
            }while(child = child.nextSibling)
        }
        this.append('</'+node.tagName+'>')
        return null;
    }
    return node;
});

//:core
XMLParser.prototype.addParser(function(node){//for
    if(node.nodeType ==1){
        var tagName = node.tagName.toLowerCase();
        if(isTemplateNS(node.namespaceURI,/^c\:/i.test(tagName))){
            switch(tagName.substr(2)){
            case 'if':
                parseIfTag.call(this,node);
                break;
            case 'elseif':
            case 'else-if':
            case 'else':
                parseElseIfTag.call(this,node);
                break;
            case 'for':
            case 'foreach':
                parseForTag.call(this,node);
                break;
            case 'set':
            case 'var':
                parseVarTag.call(this,node);
                break;
            case 'out':
                parseOutTag.call(this,node);
                break;
            case 'choose':
                parseChooseTag.call(this,node);
                break;
            case 'when':
            case 'otherwise':
                break;
            case 'def':
            case 'macro':
            	parseDefTag.call(this,node);
                break;
            
            
            //for other
            case 'include':
                processIncludeTag.call(this,node);
                break;
            default:
                $log.error("未知标签：",tagName,node.ownerDocument.documentURI)
            }
            return null;
        }
    }
    return node;
});
/**
 * 
 */
function parseDefTag(node){
    var next = node.firstChild;
    var ns = getAttribute(this,node,'name',false,true);
    var result = this.result;
    var mark = result.length;
    ns = (ns.replace(/^\s+/,'')+'end').split(/[^\w]+/);
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
    this.append([ADD_ON_TYPE,this.parseEL(el.join('')),"#def"]);
    if(next){
        do{
            this.parseNode(next)
        }while(next = next.nextSibling)
    }
    this.append([]);
    var old = result.splice(0,mark);
    result.push.apply(result,old);
}
function processIncludeTag(node){
    var var_ = getAttribute(this,node,'var');
    var path = getAttribute(this,node,'path');
    var xpath = getAttribute(this,node,'xpath');
    var name = getAttribute(this,node,'name');
    var doc = node.ownerDocument || node;
    var parentURL = this.url;
	try{
		if(name){
			var docFragment = doc.createDocumentFragment();
			var next = node.firstChild;
            if(next){
                do{
                    docFragment.appendChild(next)
                }while(next = next.nextSibling)
            }
            this['#'+name] = docFragment;
		}
	    if(var_){
            var next = node.firstChild;
            this.append([CAPTRUE_TYPE,var_]);
            if(next){
                do{
                    this.parseNode(next)
                }while(next = next.nextSibling)
            }
            this.append([]);
	    }
	    if(path!=null){
	    	if(path.charAt() == '#'){
	    		doc = this['#'+name];
	    		this.url = doc.documentURI;
	    	}else{
		        var url = parentURL?parentURL.replace(/[^\/]*(?:[#\?].*)?$/,path):path;
		        var doc = this.load(url);
	    	}
	    }
	    if(xpath!=null){
	        doc = selectNodes(doc,xpath);
	    }
	    this.parseNode(doc)
    }finally{
        this.url = parentURL;
    }
}
function parseIfTag(node){
    var next = node.firstChild;
    var test = getAttribute(this,node,'test',true,true);
    this.append([IF_TYPE,test]);
    if(next){
        do{
            this.parseNode(next)
        }while(next = next.nextSibling)
    }
    this.append([]);
}

function parseElseIfTag(node){
    this.clearPreviousText();
    var next = node.firstChild;
    var test = getAttribute(this,node,'test',true,false);
    if(test){
    	this.append([ELSE_TYPE,test]);
    }else{
    	this.append([ELSE_TYPE]);
    }
    if(next){
        do{
            this.parseNode(next)
        }while(next = next.nextSibling)
    }
    this.append([]);
}


function parseElseTag(node){
    this.clearPreviousText();
    var next = node.firstChild;
    this.append([ELSE_TYPE]);
    if(next){
        do{
            this.parseNode(next)
        }while(next = next.nextSibling)
    }
    this.append([]);
}


function parseChooseTag(node){
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
        			parseIfTag.call(this,next);
        		}else{
		            parseElseIfTag.call(this,next);
        		}
        	}else if(next.tagName == elseTag){
        		parseElseTag.call(this,next);
        	}
        	//else if(next.tagName){
        	//	$log.error("choose 只接受 when，otherwise 节点");
        	//}
        	//this.parseNode(next)//
		}while(next = next.nextSibling)
    }
}

function parseForTag(node){
    var next = node.firstChild;
    var items = getAttribute(this,node,'items',true);
    var var_ = getAttribute(this,node,'var');
    var status_ = getAttribute(this,node,'status');
    
    this.append([FOR_TYPE,items,var_]);
    if(status_){
        this.append([VAR_TYPE,this.parseEL("for"),status_])
    }
    if(next){
        do{
            this.parseNode(next)
        }while(next = next.nextSibling)
    }
    this.append([]);
}
function parseVarTag(node){
    var name = getAttribute(this,node,'name');
    var value = getAttribute(this,node,'value');
    if(value){
    	var value = this.parseText(value,false);
    	if(value.length == 1){
    		value = value[0];
    		if(value instanceof Array){
    			value = value[1];
    		}
    		this.append([VAR_TYPE,value,name]);
    	}else{
    		this.append([CAPTRUE_TYPE,name]);
	        this.append.apply(this,value)
	        this.append([]);
    	}
    }else{
        var next = node.firstChild;
        this.append([CAPTRUE_TYPE,name]);
        if(next){
            do{
                this.parseNode(next)
            }while(next = next.nextSibling)
        }
        this.append([]);
    }
}

function parseOutTag(node){
    var value = getAttribute(this,node,"value");
    value = this.parseText(value,false);
    this.append.apply(this,value);
}

//parser element
/*
function parseElement(node){
    var next = node.attributes;
    this.append('<'+node.tagName);
    for (var i=0; i<next.length; i++) {
        this.parseNode(next.item(i))
    }
    var next = node.firstChild;
    if(next){
        this.append('>')
        var postfix = '</'+node.tagName+'>';
        do{
            this.parseNode(next)
        }while(next = next.nextSibling)
        this.append(postfix)
    }else{
        this.append('/>')
    }
    return null;
}
*/
//parser attribute
function parseAttribute(node){
    var name = node.name;
    var value = node.value;
	if(isTemplateNS(value, name.toLowerCase() == "xmlns:c")){
		return null;
	}
    var buf = this.parseText(value,true,true);
    var isStatic;
    var isDynamic;
    //hack this.parseText is void 
    var i =  buf.length;
    while(i--){
        //hack reuse value param
        var value = buf[i];
        if(value.constructor == String){
            if(value){
                isStatic = true;
            }else{
                buf.splice(i,1);
            }
        }else{
            isDynamic = true;
        }
    }
    if(isDynamic && !isStatic){
        //remove attribute;
        //this.append(" "+name+'=""');
        if(buf.length > 1){
            //TODO:....
            throw new Error("属性内只能有单一EL表达式！！");
        }else{//只考虑单一EL表达式的情况
            buf = buf[0];
	        this.append( [XML_ATTRIBUTE_TYPE,buf[1],name]);
	        return null;
        }
    }
    this.append(" "+name+'="');
    if(/^xmlns$/i.test(name)){
        if(buf[0] == 'http://www.xidea.org/ns/lite/xhtml'){
            buf[0] = 'http://www.w3.org/1999/xhtml'
        }
    }
    this.append.apply(this,buf);
    this.append('"');
    return null;
}
function parseTextNode(node){
    var data = String(node.data);
    //this.append.apply(this,this.parseText(data.replace(/^\s*([\r\n])\s*|\s*([\r\n])\s*$|^(\s)+|(\s)+$/g,"$1$2$3$4"),true))
    //不用回车js序列化后更短
    this.append.apply(this,this.parseText(data.replace(/^\s+|\s+$/g," "),true))
    return null;
}

function parseCDATA(node){
    this.append("<![CDATA[");
    this.append.apply(this,this.parseText(node.data));
    this.append("]]>");
    return null;
}
function parseEntityReference(){
    return null;//not support
}
function parseEntity(){
    return null;//not support
}
function parseProcessingInstruction(node){
    this.append("<?"+node.nodeName+" "+node.data+"?>");
    return null;
}
function parseComment(){
    return null;//not support
}
function parseDocument(node){
    for(var n = node.firstChild;n!=null;n = n.nextSibling){
        this.parseNode(n);
    }
    return null;
}
/**
 * @protected
 */
function parseDocumentType(node){
    if(node.xml){
        this.append(node.xml);
    }else{
        if(node.publicId){
            this.append('<!DOCTYPE ');
            this.append(node.nodeName);
            this.append(' PUBLIC "');
            this.append(node.publicId );
            this.append( '" "');
            this.append(node.systemId);
            this.append('">');
        }else if(node.systemId){
            this.append('<!DOCTYPE ');
            this.append(node.nodeName);
            this.append(' SYSTEM "');
            this.append(node.systemId);
            this.append('">');
        }else{
            this.append('<!DOCTYPE ');
            this.append(node.nodeName);
            this.append(' [');
            this.append(node.internalSubset);
            this.append(']>');
        }
    }
    return null;
}
//    /**
//     * @protected
//     */
//    function parseDocumentFragment(node){
//        var nl = node.childNodes;
//        for (var i=0; i<nl.length; i++) {
//            this.parseNode(nl.item(i));
//        }
//        return null;
//    }
/**
 */
function parseNotation(node){
    return null;//not support
}

//1 2



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
		if(isEL){
	         return findFirstEL(context,value);
		}else{
			return String(value).replace(/^\s+|\s+$/g,'');
		}
	}else if(required){
		$log.error("属性"+key+"为必须值");
		throw new Error();
	}
}
function findFirstEL(context,value){
	var els = context.parseText(value,false);
	var i = els.length;
	while(i--) {
		var el = els[i];
		if(el instanceof Array){//el
		    return el[1];
		}else if(el){
			return '"' + (stringRegexp.test(value) ?
                            value.replace(stringRegexp,charReplacer) :
                            value)
                       + '"';
		}
	}
}

/**
 * @public
 */
function toDoc(text){
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
    try{
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