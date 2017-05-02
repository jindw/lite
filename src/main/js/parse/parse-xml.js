/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
//import {XA_TYPE,EL_TYPE,XT_TYPE} from './template-token';
//export var XML_SPACE_TRIM = "http://www.xidea.org/lite/attribute/h:trim-space" 
//export 
var XA_TYPE = require('./template-token').XA_TYPE;
var EL_TYPE = require('./template-token').EL_TYPE;
var XT_TYPE = require('./template-token').XT_TYPE;
var Expression=require('js-el').Expression;

var XML_SPACE_TRIM =exports.XML_SPACE_TRIM = "http://www.xidea.org/lite/attribute/h:trim-space" 
var XML_SPACE_ONELINE = exports.XML_SPACE_ONELINE = "http://www.xidea.org/lite/attribute/h:space-oneline" 

exports.parseDefaultXMLNode = parseDefaultXMLNode;
function parseDefaultXMLNode(node,context,chain){
	//try{
	    switch(node.nodeType){
	        case 1: //NODE_ELEMENT 
	            processElement(node,context,chain)
	            break;
	        case 2: //NODE_ATTRIBUTE                             
	            processAttribute(node,context,chain)
	            break;
	        case 3: //NODE_TEXT                                        
	            processTextNode(node,context,chain)
	            break;
	        case 4: //NODE_CDATA_SECTION                     
	            processCDATA(node,context,chain)
	            break;
	        case 5: //NODE_ENTITY_REFERENCE                
	            processEntityReference(node,context,chain)
	            break;
	        case 6: //NODE_ENTITY            
	            processEntity(node,context,chain)
	            break;
	        case 7: //NODE_PROCESSING_INSTRUCTION    
	            processProcessingInstruction(node,context,chain)
	            break;
	        case 8: //NODE_COMMENT                                 
	            processComment(node,context,chain)
	            break;
	        case 9: //NODE_DOCUMENT                                
	        case 11://NODE_DOCUMENT_FRAGMENT             
	            processDocument(node,context,chain)
	            break;
	        case 10://NODE_DOCUMENT_TYPE                     
	            processDocumentType(node,context,chain)
	//        case 11://NODE_DOCUMENT_FRAGMENT             
	//            processDocumentFragment(node,context,chain)
	            break;
	        case 12://NODE_NOTATION 
	            processNotation(node,context,chain);
	            break;
	        default://文本节点
	        	chain.next(node);
	            //this.println("<!-- ERROR： UNKNOW nodeType:"+node.nodeType+"-->")
	    }
	//}catch(e){
	//	console.error('default xml node parse error:'+e);
	//}
}

var htmlLeaf = /^(?:meta|link|img|br|hr|input)$/i;
var htmlReserved = /^(?:script|style|pre|textarea)$/i
var scriptTag = /^script$/i
function processElement(node,context,chain){
    var attributes = node.attributes;
    var tagName = node.tagName;
    context.appendText('<'+tagName);
    for (var i=0; i<attributes.length; i++) {
        try{
            //htmlunit bug...
            var attr = attributes.item(i);
        }catch(e){
            var attr =attributes[i];
        }
        context.parse(attr)
    }
    if(htmlLeaf.test(tagName)){
        context.appendText('/>')
        return ;
    }
    context.appendText('>')
    var child = node.firstChild
    if(child){
    	///if(htmlReserved.test(tagName)){
    	//	context.setReservedSpace(true)
    	//}
        do{
            context.parse(child)
        }while(child = child.nextSibling)
    }
    context.appendText('</'+node.tagName+'>')
}

//parser attribute
function processAttribute(node,context,chain){
    var name = String(node.name);
    var value = String(node.value);
    var buf = context.parseText(value,XA_TYPE);
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
        //context.appendText(" "+name+"=''");
        if(buf.length > 1){
            for(var i = 0;i<buf.length;i++){
            	if(buf[i][0] == XA_TYPE){
            		buf[i] = '('+new Expression(buf[i][1])+')';
            		//console.log(buf[i])
            	}else{
            		throw new Error("属性内不能混合多条不安全的表达式输出！！");
            	}
            }
            context.appendXA(name,"''+"+buf.join('+'));
            return null;
        }else{//只考虑单一EL表达式的情况
            if(buf[0][0] == XA_TYPE){
            	//buf[0][1] 是一个表达式对象
	        	context.appendXA(name,buf[0][1]);
	        	return null;
            //}else{
            //	否则任意输出，甚至可以打破xml属性字符
            }
        }
    }
	var oneline = context.getAttribute(XML_SPACE_ONELINE);
    var space = (!oneline &&/^on/i.test(name)?'\n':' ');// on 事件换行有利于调试
    context.appendText(space+name+"='");
    if(/^xmlns$/i.test(name)){
        if(buf[0] == 'http://www.xidea.org/lite/xhtml'){
            buf[0] = 'http://www.w3.org/1999/xhtml'
        }
    }
    context.appendAll(buf);
    context.appendText("'");
}
function processTextNode(node,context,chain){
    var data = String(node.data);
    //context.appendAll(context.parseText(data.replace(/^\s*([\r\n])\s*|\s*([\r\n])\s*$|^(\s)+|(\s)+$/g,"$1$2$3$4"),XT_TYPE))
    
	var trim = context.getAttribute(XML_SPACE_TRIM);
	var oneline = context.getAttribute(XML_SPACE_ONELINE);
    if(trim == true){//尽可能剔除
    	data = data.replace(/^\s*|\s*$|(\s)\s+/g,oneline?' ':"$1");
    }else if(trim != false){//保留换行
   		data = data.replace(/^\s*([\r\n])\s*|\s*([\r\n])\s*$|^(\s)+|(\s)+$/g,oneline?' ':"$1$2$3$4");
    }
    context.appendAll(context.parseText(data,XT_TYPE))
}

function processCDATA(node,context,chain){
    context.appendText("<![CDATA[");
    context.appendAll(context.parseText(node.data,EL_TYPE));
    context.appendText("]]>");
}
function processEntityReference(){
    return null;//not support
}
function processEntity(){
    return null;//not support
}
function processProcessingInstruction(node,context,chain){
	context.appendText("<?",node.target," ",node.data,"?>");
    //context.appendText("<?"+node.nodeName+" "+node.data+"?>");
}
function processComment(){
    return null;//not support
}
function processDocument(node,context,chain){
    for(var n = node.firstChild;n!=null;n = n.nextSibling){
        context.parse(n);
    }
}
///**
// * @protected
// */
//function processDocumentFragment(node,context,chain){
//    var nl = node.childNodes;
//    for (var i=0; i<nl.length; i++) {
//        context.parse(nl.item(i));
//    }
//}
/**
 * @protected
 */
function processDocumentType(node,context,chain){
    if(node.xml){
        context.appendText(node.xml);
    }else{
    	var pubid = node.publicId;
    	var nodeName = node.nodeName;
		var sysid = node.systemId;
		if(sysid == '.'){sysid = null}
        if(pubid){
			if(pubid == "org.xidea.lite.OUTPUT_DTD"){
				if(sysid){
					context.appendText(decodeURIComponent(sysid));
				}
				return;
			}
            context.appendText('<!DOCTYPE ');
            context.appendText(nodeName);
            context.appendText(' PUBLIC "');
            context.appendText(pubid);
            if (sysid == null) {
            	context.appendText( '" "');
            	context.appendText(sysid);
            }
            context.appendText('">');
        }else if(sysid){
            context.appendText('<!DOCTYPE ');
            context.appendText(nodeName);
            context.appendText(' SYSTEM "');
            context.appendText(sysid);
            context.appendText('">');
        }else{
        	context.appendText("<!DOCTYPE ");
			context.appendText(nodeName);
			var sub = node.internalSubset;
            if(sub){
				context.appendText(" [");
				context.appendText(sub);
				context.appendText("]");
			}
			context.appendText(">");
        }
    }
}

/**
 */
function processNotation(node,context,chain){
    return null;//not support
}

//1 2


