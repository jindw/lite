/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var appendForStart = require('./syntax-util').appendForStart;
var parseChildRemoveAttr=require('./syntax-util').parseChildRemoveAttr;
var findLiteParamMap=require('./syntax-util').findLiteParamMap;
var parseDefName = require('./syntax-util').parseDefName;
var setNodeURI = require('./syntax-util').setNodeURI;
var findELEnd=require('./el-util').findELEnd;
var querySelectorAll = require('./xml').querySelectorAll;
var selectByXPath=require('./xml').selectByXPath;
var findXMLAttribute=require('./xml').findXMLAttribute;
var findXMLAttributeAsEL=require('./xml').findXMLAttributeAsEL;
var URI=require('./resource').URI;
var PLUGIN_DEFINE=require('./template-token').PLUGIN_DEFINE;
var XML_SPACE_ONELINE = require('./parse-xml').XML_SPACE_ONELINE
var EL_TYPE=require('./template-token').EL_TYPE;
var XT_TYPE = require('./template-token').XT_TYPE;
var XA_TYPE = require('./template-token').XA_TYPE;
var CHOOSE_KEY = "http://www.xidea.org/lite/core/c:choose@value";
var Core = {
	xmlns : function(){},
	parseExtension:function(node){
		var ns = findXMLAttribute(node,'*namespace','ns');
		var file = findXMLAttribute(node,'file');
		var pkg = findXMLAttribute(node,'package');
		
		if(pkg){
			source = pkg;
		}else if(file){
			var source = this.loadText(this.createURI(file))+'\n';
		}else{
			var source = findXMLAttribute(node,'#text')+'\n';
		}
		this.addExtension(ns,source);
	},
	parseComment:function(){},
	seek:function(text){
		var end = findELEnd(text,0);
		if(end>0){
			try{
				var els = text.substring(1,end);
				var el = this.parseEL(els);
	            switch(this.textType){
	            case XT_TYPE:
	            	this.appendXT(el);
	            	break;
	            case XA_TYPE:
	            	this.appendXA(null,el);
	            	break;
	            default:
	            	this.appendEL(el);
	            }
	            return end;
			}catch(e){
				console.error("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+els+"]",e)
				return -1;
			}
		}else{
			console.warn("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",text:"+text+"]")
			return -1;
		}
	},
	"seek!":function(text){
		var end = findELEnd(text,0);
		if(end>0){
			try{
				var el = text.substring(1,end);
	            this.appendEL(el);
	            return end;
			}catch(e){
				console.error("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
				return -1;
			}
		}else{
			console.warn("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
			return -1;
		}
	},
	"seek#":function(text){
		var end = findELEnd(text,0);
		if(end>0){
			try{
				var el = text.substring(1,end);
				this.appendPlugin("org.xidea.lite.EncodePlugin","{}");
				this.appendEL(el);
				this.appendEnd()
				return end;
			}catch(e){
				console.error("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
				return -1;
			}
		}else{
			console.warn("表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
			return -1;
		}
		
	},
	/**
	 * <c:date-format pattern="" >
	 */
	parseDateFormat:function(node){
		var pattern = findXMLAttribute(node,'pattern');
		var value =  findXMLAttributeAsEL(node,'value','date','time','#text') || 'null';
		value = value.replace(/^\s+|\s+$/g,'')
		if(pattern){
			var pattern2 = pattern.replace(/^\s*\$\{([\s\S]+)\}\s*$/,'$1')
			if(pattern2 == pattern){
				pattern2 = JSON.stringify(pattern);
			}
		}else{
			pattern2 = '"YYYY-MM-DD"';
		}
		this.appendPlugin("org.xidea.lite.DatePlugin","{}");
		this.appendEL(pattern2);
		this.appendEL(value);
		this.appendEnd();
	}
	
}
exports.Core=Core;

Core.parseTime = Core.parseDate = Core.parseDateFormat;
addParserAndAttrInterceptor(processIf,'if')
addParserAndAttrInterceptor(processElse,'else');
addParserAndAttrInterceptor(processElif,"elseif","elif");
addParserAndAttrInterceptor(processFor,"for","foreach");
addParserAndAttrInterceptor(processVar,"var","set");
addParserAndAttrInterceptor(processOut,"out");
addParserAndAttrInterceptor(processDef,"def",'macro');
addParserAndAttrInterceptor(processClient,"client");
addParserAndAttrInterceptor(processChoose,"choose");
addParserAndAttrInterceptor(processWhen,"when");
addParserAndAttrInterceptor(processOtherwise,"otherwise");
addParserAndAttrInterceptor(processInclude,"include");

function addParserAndAttrInterceptor(processor){
	var i = arguments.length;
	while(--i>0){
		var n = arguments[i];
		Core['parse'+n]=Core['intercept'+n]=processor;
	}
}

/**
 * node,attribute parse and interceptor
 */
function processIf(node){
	var test = findXMLAttributeAsEL(node,'*test','value');
    this.appendIf(test);
    parseChildRemoveAttr(this,node);
    this.appendEnd();
}


function processElse(node){
    var test = findXMLAttributeAsEL(node,'test','value');
    this.appendElse(test || null);
    parseChildRemoveAttr(this,node);
    this.appendEnd();
}

function processElif(node){
    var test = findXMLAttributeAsEL(node,'*test','value');
    this.appendElse(test || null);
    parseChildRemoveAttr(this,node);
    this.appendEnd();
}

function processChoose(node){
	var value = findXMLAttributeAsEL(node,"value","test");
	var oldStatus = this.getAttribute(CHOOSE_KEY);
	this.setAttribute(CHOOSE_KEY,{value:value,first:true});
	parseChildRemoveAttr(this,node,true);
	this.setAttribute(CHOOSE_KEY,oldStatus);
}
function processWhen(node){
	var stat = this.getAttribute(CHOOSE_KEY);
	var value = findXMLAttributeAsEL(node,"*test","if","value");
	if(stat.value){
		value = '('+stat.value + ')==('+value+')';
	}
	if(stat.first){
		stat.first = false;
		this.appendIf(value);
	}else{
		this.appendElse(value);
	}
	parseChildRemoveAttr(this,node);
	this.appendEnd();
}

function processOtherwise(node){
	this.appendElse(null);
	parseChildRemoveAttr(this,node);
	this.appendEnd();
}

function processFor(node){
	if(node.nodeType == 1){
    	var list = findXMLAttributeAsEL(node,'*list','values','items','value');
    	var var_ = findXMLAttribute(node,'*var','name','id','item');
    	var status_ = findXMLAttribute(node,'status');
	}else{//attr
		var var_ = findXMLAttribute(node).replace(/\s*\$\{([\s\S]*)\}\s*$/,'$1');
	}
    appendForStart(this,var_,list,status_ || null);
    parseChildRemoveAttr(this,node);
    this.appendEnd();
}


function processVar(node){
    var name_ = findXMLAttribute(node,'*name','id');
	if(node.nodeType == 1){
		var value = findXMLAttribute(node,'value');
	    if(value){
	    	var code = this.parseText(value,0);
	    	if(code.length == 1){
	    		code = code[0];
	    		if(code instanceof Array){
	    			this.appendVar(name_,code[1]);
	    		}else{
	    			console.warn("标签:"+node.tagName+"的value属性"+value+"建议设置为表达式，您的输入没有表达式，系统自动按静态文本处理");
	    			this.appendVar(name_,JSON.stringify(code));
	    		}
	    	}else{
	    		this.appendCapture(name_);
		        this.appendAll(code)
		        this.appendEnd();
	    	}
	    }else{
	        this.appendCapture(name_);
	        parseChildRemoveAttr(this,node);
	        this.appendEnd();
	    }
	}else{
		var map = findLiteParamMap(name_);
		if(map){
			for(var n in map){
				this.appendVar(n,map[n]);
			}
			parseChildRemoveAttr(this,node);
		}else{
	        this.appendCapture(name_);
	        parseChildRemoveAttr(this,node);
	        this.appendEnd();
		}
	}
}



function processOut(node){
	var value = findXMLAttribute(node,"value","#text");
	value = this.parseText(value,EL_TYPE);
	this.appendAll(value);
	if(node.nodeType == 2){//attribute
		console.error(' c:out attribute is unsafe!!');
	}
}


function processDef(node){
    var ns = findXMLAttribute(node,'*name');
    var config = parseDefName(ns);
    this.appendPlugin(PLUGIN_DEFINE,JSON.stringify(config));
    parseChildRemoveAttr(this,node);
    this.appendEnd();
}


function processClient(node){
	var name_ = findXMLAttribute(node,'*name','id');
	var config = parseDefName(name_);
	var oneline = this.getAttribute(XML_SPACE_ONELINE);
	this.setAttribute(XML_SPACE_ONELINE,true);
	this.appendText("<script>//<![CDATA[\n");
	this.appendPlugin("org.xidea.lite.parse.ClientPlugin",JSON.stringify(config));
	parseChildRemoveAttr(this,node);
	this.appendEnd();
	this.appendText("//]]></script>")
	this.setAttribute(XML_SPACE_ONELINE,oneline);
}

function processInclude(node){
	if(node.nodeType == 1){
		_parseInclude.apply(this,arguments);
	}else if(node.nodeType == 2){
		_interceptInclude.apply(this,arguments);
	}
}
function _parseInclude(node){
    var path = findXMLAttribute(node,'path');
    var xpath = findXMLAttribute(node,'xpath');
    var selector = findXMLAttribute(node,'selector');
    var parentURI = this.currentURI;
	try{
	    if(path){
	    	if(path.charAt() == '#'){
	    		console.warn("装饰器命名节点改用${pageName}模式了:(,您实用的模式还是:"+path);
	    		path = '$'+path.substring(1);
	    	}
	    	if(path.charAt() == '$'){
	    		doc = this.getAttribute(path);
	    		setNodeURI(this,node);
	    	}else{
		        var uri = this.createURI(path);
		        var doc = this.loadXML(uri);
	    		this.setCurrentURI(uri);
	    	}
	    }else{
	    	//var doc = this.loadXML(this.currentURI);
	    	var doc = node.ownerDocument.cloneNode(true);
	    }
		if(doc==null){
			this.appendText("<strong style='color:red'>没找到包含节点："+this.currentURI+ node.value+"</strong>");
		}else{
		    if(selector != null){
		    	var list = querySelectorAll.call(doc,selector);;
		    	//console.log('#####')
	    		if(list && list.length){
	    			for(var i=0;i<list.length;i++){
	    			//console.log(list[i]+'/'+i)
	    				this.parse(list[i])
	    			}
	    		}else{
	    			console.warn("empty selection:"+selector)
	    		}
	    		return;
		    }else if(xpath!=null){
		    	var d = doc;
		        doc = selectByXPath(doc,xpath);
		        //alert([url,xpath,new XMLSerializer().serializeToString(d),doc.length])
		    }
		        
		    this.parse(doc)
		    
		}
    }finally{
        this.setCurrentURI(parentURI);
    }
}

function _interceptInclude(attr){
	var match = attr.value.match(/^([^#]*)(?:#(.*))?$/);
	var path = match[1];
	var xpath = match[2];
	if(path){
		var path2 = path.replace(/^[\$#]([\w\$_]+)$/,'$$$1');
		if(path2.charAt() == '$') {
			doc = this.getAttribute(path2);
		}else{
			var uri = this.createURI(path);
			var doc = this.loadXML(uri)
			//this.setCurrentURI(uri);
		}
	}else{
		var doc = attr.ownerDocument;
	}
	if(doc==null){
		this.appendText("<strong style='color:red'>没找到包含节点："+this.currentURI+ attr.value+"</strong>");
	}else{
		var attrs = selectByXPath(doc, xpath);
		var element = attr.ownerElement || attr.selectSingleNode('..');
		//element.removeAttributeNode(attr)
		for(var i = attrs.length;i--;){
			var a = attrs.item(i);
			_mergeAttribute(element,a);
		}
	}
	parseChildRemoveAttr(this,attr);
	//this.setCurrentURI(oldURI);
}
function _mergeAttribute(element,node){
	if(node.nodeType == 2){
		var attr = element.getAttributeNS(node.namespaceURI,node.name);
		element.setAttributeNS(node.namespaceURI,node.name,attr+node.value);
	}else if(node.nodeType == 1){
		var attributes = node.attributes;
		for (var i = 0; i < attributes.length; i++) {
			_mergeAttribute(element,attributes.item(i));
		}
	}
}
