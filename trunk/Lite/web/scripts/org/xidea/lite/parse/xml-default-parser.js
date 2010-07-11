/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */


function parseDefaultXMLNode(node,context,chain){
    switch(node.nodeType){
        case 1: //NODE_ELEMENT 
            parseElement(node,context,chain)
            break;
        case 2: //NODE_ATTRIBUTE                             
            parseAttribute(node,context,chain)
            break;
        case 3: //NODE_TEXT                                        
            parseTextNode(node,context,chain)
            break;
        case 4: //NODE_CDATA_SECTION                     
            parseCDATA(node,context,chain)
            break;
        case 5: //NODE_ENTITY_REFERENCE                
            parseEntityReference(node,context,chain)
            break;
        case 6: //NODE_ENTITY            
            parseEntity(node,context,chain)
            break;
        case 7: //NODE_PROCESSING_INSTRUCTION    
            parseProcessingInstruction(node,context,chain)
            break;
        case 8: //NODE_COMMENT                                 
            parseComment(node,context,chain)
            break;
        case 9: //NODE_DOCUMENT                                
        case 11://NODE_DOCUMENT_FRAGMENT             
            parseDocument(node,context,chain)
            break;
        case 10://NODE_DOCUMENT_TYPE                     
            parseDocumentType(node,context,chain)
//        case 11://NODE_DOCUMENT_FRAGMENT             
//            parseDocumentFragment(node,context,chain)
            break;
        case 12://NODE_NOTATION 
            parseNotation(node,context,chain);
            break;
        default://文本节点
        	chain.next(node);
            //this.println("<!-- ERROR： UNKNOW nodeType:"+node.nodeType+"-->")
    }
}


var htmlLeaf = /^(?:meta|link|img|br|hr)$/i;
var scriptTag = /^script$/i
function parseElement(node,context,chain){
    var attributes = node.attributes;
    context.append('<'+node.tagName);
    for (var i=0; i<attributes.length; i++) {
        try{
            //htmlunit bug...
            var attr = attributes.item(i);
        }catch(e){
            var attr =attributes[i];
        }
        context.parse(attr)
    }
    if(htmlLeaf.test(node.tagName)){
        context.append('/>')
        return ;
    }
    context.append('>')
    var child = node.firstChild
    if(child){
        do{
            context.parse(child)
        }while(child = child.nextSibling)
    }
    context.append('</'+node.tagName+'>')
}

//parser attribute
function parseAttribute(node,context,chain){
    var name = String(node.name);
    var value = String(node.value);
    var buf = context.parseText(value,XML_ATTRIBUTE_TYPE);
    var isStatic;
    var isDynamic;
    //hack context.parseText is void 
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
        //context.append(" "+name+'=""');
        if(buf.length > 1){
            //TODO:....
            throw new Error("属性内只能有单一EL表达式！！");
        }else{//只考虑单一EL表达式的情况
            buf = buf[0];
            //buf[1] 是一个表达式对象
	        context.appendAttribute(name,buf[1]);
	        return null;
        }
    }
    context.append(" "+name+'="');
    if(/^xmlns$/i.test(name)){
        if(buf[0] == 'http://www.xidea.org/ns/lite/xhtml'){
            buf[0] = 'http://www.w3.org/1999/xhtml'
        }
    }
    context.appendAll(buf);
    context.append('"');
}
function parseTextNode(node,context,chain){
    var data = String(node.data);
    //context.appendAll(context.parseText(data.replace(/^\s*([\r\n])\s*|\s*([\r\n])\s*$|^(\s)+|(\s)+$/g,"$1$2$3$4"),XML_TEXT_TYPE))
    //不用回车js序列化后更短
    context.appendAll(context.parseText(data.replace(/^\s+|\s+$/g," "),XML_TEXT_TYPE))
}

function parseCDATA(node,context,chain){
    context.append("<![CDATA[");
    context.appendAll(context.parseText(node.data,EL_TYPE));
    context.append("]]>");
}
function parseEntityReference(){
    return null;//not support
}
function parseEntity(){
    return null;//not support
}
function parseProcessingInstruction(node,context,chain){
    context.append("<?"+node.nodeName+" "+node.data+"?>");
}
function parseComment(){
    return null;//not support
}
function parseDocument(node,context,chain){
    for(var n = node.firstChild;n!=null;n = n.nextSibling){
        context.parse(n);
    }
}
///**
// * @protected
// */
//function parseDocumentFragment(node,context,chain){
//    var nl = node.childNodes;
//    for (var i=0; i<nl.length; i++) {
//        context.parse(nl.item(i));
//    }
//}
/**
 * @protected
 */
function parseDocumentType(node,context,chain){
    if(node.xml){
        context.append(node.xml);
    }else{
        if(node.publicId){
            context.append('<!DOCTYPE ');
            context.append(node.nodeName);
            context.append(' PUBLIC "');
            context.append(node.publicId );
            context.append( '" "');
            context.append(node.systemId);
            context.append('">');
        }else if(node.systemId){
            context.append('<!DOCTYPE ');
            context.append(node.nodeName);
            context.append(' SYSTEM "');
            context.append(node.systemId);
            context.append('">');
        }else{
            context.append('<!DOCTYPE ');
            context.append(node.nodeName);
            context.append(' [');
            context.append(node.internalSubset);
            context.append(']>');
        }
    }
}

/**
 */
function parseNotation(node,context,chain){
    return null;//not support
}

//1 2


