/*
    if have any bug please send email to me:
    initial:242772@qq.com
*/

function XML2JSON(response){
    this.trimText = true;
};
XML2JSON.prototype={
    parserList : [function(node,parentObject){
        switch(node.nodeType){
            case 1: //NODE_ELEMENT 
                return parseElement(this,node,parentObject)
            case 2: //NODE_ATTRIBUTE                             
                return parseAttribute(this,node,parentObject)
            case 3: //NODE_TEXT                                        
                return parseTextNode(this,node,parentObject,this.trimText)
            case 4: //NODE_CDATA_SECTION                     
                return parseCDATA(this,node,parentObject,this.trimText)
            case 5: //NODE_ENTITY_REFERENCE                
                return true;//parseEntityReference(this,node,parentObject)
            case 6: //NODE_ENTITY            
                return true;//parseEntity(this,node,parentObject)
            case 7: //NODE_PROCESSING_INSTRUCTION    
                return true;//parseProcessingInstruction(this,node,parentObject)
            case 8: //NODE_COMMENT                                 
                return true;//parseComment(this,node,parentObject)
            case 9: //NODE_DOCUMENT                                
            case 11://NODE_DOCUMENT_FRAGMENT             
                return parseDocument(this,node,parentObject)
            case 10://NODE_DOCUMENT_TYPE                     
                return true;//parseDocumentType(this,node,parentObject)
            //case 11://NODE_DOCUMENT_FRAGMENT             
            //    return parseDocumentFragment(this,node,parentObject)
            case 12://NODE_NOTATION 
                return true;//parseNotation(this,node,parentObject)
            default://文本节点
                //this.println("<!-- ERROR： UNKNOW nodeType:"+node.nodeType+"-->")
        }
    }],
    parseNode:function(node,parentObject){
        var i  = this.parserList.length;
        while(i--){
            if(this.parserList[i].call(this,node,parentObject)){
                return true;
            }
        }
    },
	parse: function(xml){
		var result = {};
		this.parseNode(toXMLDocument(xml),result);
		return result;
	},
	parseRss : function (data) {
		var object = this.parse(data);
		return object.rss.channel.item;
	}
}
function appendNode(parentObject,key,objectChild){
    var old = parentObject[key]
    if(old){
        if(old instanceof Array){
            old.push(objectChild)
        }else{
            parentObject[key] = [old,objectChild];
        }
    }else{
        parentObject[key] = objectChild;
    }
}

function getPureText(node){
    var next = node.firstChild;
    var buf = [];
    do{
        switch(next.nodeType){
        case 3: //NODE_TEXT                                        
        case 4: //NODE_CDATA_SECTION    
            buf.push(next.data);                 
        case 8: //NODE_COMMENT   
            break;
        default:
            return null;
        }
    }while(next = next.nextSibling);
    return buf.join('');
}


function parseDocument(x2j,node,parentObject){
    node = node.documentElement;
    x2j.parseNode(node,parentObject);
    return true;
}
function parseElement(x2j,node,parentNode){
    var next = node.attributes;
    if(next.length){
        var object = {};
        for (var i=0; i<next.length; i++) {
            x2j.parseNode(next.item(i),object)
        }
    }
    var next = node.firstChild;
    if(next){
        if(object || (object = getPureText(node)) == null){
            object = {};
            do{
                x2j.parseNode(next,object)
            }while(next = next.nextSibling)
        }
    }
    appendNode(parentNode,node.tagName,object)
    return true;
}
function parseAttribute(x2j,node,parentObject){
    parentObject[node.name] = node.value;
    return true;
}
function parseTextNode(x2j,node,parentObject,trim){
    var data = node.data;
    if(!trim || /^\s*$/.test(data)){
        appendNode(parentObject,"#text",data)
    }
    return true;
}
function parseCDATA(x2j,node,parentObject){
    return parseTextNode(node,parentObject,trim)
}


function toXMLDocument(data){
    if(data && data.constructor == String){
        if(!/^[\s\ufeff]*</.test(data)){
            var xhr = new XMLHttpRequest();
            xhr.open("GET",data,false)
            xhr.send('');
            if(/\/xml/.test(xhr.getResponseHeader("Content-Type"))){//text/xml,application/xml...
                return xhr.responseXML;
            }else{
                data = xhr.responseText;
            }
        }
        if (window.DOMParser){ //support firefox
            try {
                return (new DOMParser()).parseFromString(data, "text/xml");
            } catch(e){$log.debug(e)}
        } else if(window.ActiveXObject){
            try{
                var dom  = new ActiveXObject('Microsoft.XMLDOM');
                dom.async = false;
                if(dom.loadXML(data)){ // parse error ..
                    return dom
                }else{
                    $log.debug(dom.parseError.reason + dom.parseError.srcText);
                }
            }catch(e){ dom = null; }
        }
    }
    return data;
}
