/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
 
 
/**
 * false:preserved
 * true:trim all 
 * null:default trim muti-space to only one,(and trim first,last space)?
 * none: 
 */
var XML_SPACE_TRIM = "http://www.xidea.org/lite/attribute/h:trim-space" 
var AUTO_FORM_PREFIX = "http://www.xidea.org/lite/attribute/h:autofrom" 
var AUTO_FORM_SELETED = "http://www.xidea.org/lite/attribute/h:autofrom#selected" 
var HTML = {
	xmlns : function(){},
	parseScript:function(el){
		var oldSpace = this.getAttribute(XML_SPACE_TRIM);
		this.setAttribute(XML_SPACE_TRIM,false);
		try{
			if(!el.hasAttribute('src')){
				var child = el.firstChild;
				while(child){
					if(child.nodeType==3 || child.nodeType == 4){
						child.data = processJS(child.data);
					}
					child = child.nextSibling;
				}
			}
			this.next(el);
		}finally{
			this.setAttribute(XML_SPACE_TRIM,oldSpace);
		}
	},
	"parse2on*":function(attr){
		attr.value = processJS(attr.value);
		this.next(attr);
	},
	parseInput:function(el){
		var autoform = this.getAttribute(AUTO_FORM_PREFIX);
		if(autoform!=null){
			var name_ = el.getAttribute('name');
			if(name_){
				var type = el.getAttribute('type');
				if(type && !/^(?:reset|button|submit)$/i.test(type)){
					if(/^(?:checkbox|radio)$/i.test(type)){
						if(!el.hasAttribute('checked')){
							buildCheck2select(this,el,name_,'checked');
							return ;
						}
					}else if(!el.hasAttribute('value')){
						el.setAttribute('value', "${"+name_+"}");
					}
				}
			}
		}
		this.next(el);
	},
	parseTextArea:function(el){
		var autoform = this.getAttribute(AUTO_FORM_PREFIX);
		var hasValue = el.hasAttribute('value');//value added for textarea
		if(hasValue){
			el.textContent = el.getAttribute('value');
		}else if(autoform!=null && !el.hasChildNodes()){
			var name_ = el.getAttribute('name');
			el.textContent = "${"+ name_ + "}";
		}
		this.next(el);
	},
	parseSelect:function(el){
		this.setAttribute(AUTO_FORM_SELETED,el.getAttribute('name'));//不清理也无妨
		this.next(el);
	},
	parseOption:function(el){
		var autoform = this.getAttribute(AUTO_FORM_PREFIX);
		if(autoform!=null){
			var name_ = this.getAttribute(AUTO_FORM_SELETED);
			if(name_){
				buildCheck2select(this,el,name_,'selected');
				return;
			}
		}
		this.next(el);
		
	}
}
var HTML_EXT = {
	xmlns : function(){},
	beforeAutoform:function(node){
		var oldAutoform = this.getAttribute(AUTO_FORM_PREFIX);
		try{
			var prefix = getAttribute(node,'*value');
			this.setAttribute(AUTO_FORM_PREFIX,prefix);
    		parseChildRemoveAttr(this,node);
    	}finally{
			this.setAttribute(AUTO_FORM_PREFIX,oldAutoform);
		}
	},
	/**
	 * safe
	 * any
	 * none
	 */
	beforeTrim:function(node){
		var oldSpace = this.getAttribute(XML_SPACE_TRIM);
		try{
			var value = getAttribute(node,'*value');
			this.setAttribute(XML_SPACE_TRIM,value == 'true'?true:value == 'false'?false:null);
    		parseChildRemoveAttr(this,node);
    	}finally{
			this.setAttribute(XML_SPACE_TRIM,oldSpace);
		}
	}
}
var moveList = ['parseClient','parse2Client','seekClient'];
function buildMoved(tag){
	var fn = Core[tag];
	HTML_EXT[tag]= fn;
	Core[tag] = function(){
		$log.info("标签:"+tag+ " 已经从core到：html-ext上了！")
		fn.apply(this,arguments);
	}
}
while(buildMoved(moveList.pop()));
HTML_EXT.parseAutoform = HTML_EXT.beforeAutoform;
HTML_EXT.parseTrim = HTML_EXT.beforeTrim;
function toelv(value){
	if(value){
		var elv = value.replace(/^\$\{([\s\S]+)\}$/,'$1');
		try{
			if(elv != value){
				new Function("return "+elv);
			}
		}catch(e){
			elv = value;
		}
		if(elv == value){
			elv = stringifyJSON(value);
		}
	}
	return elv;
}
function buildCheck2select(context,el,name_,checkName){
	var value = el.getAttribute('value');
	if(!value && checkName == 'selected'){
		value = el.textContent;
	}
	var elv = toelv(value);
	
	if(!elv){
		context.next(el);
		return;
	}
	var forId = context.allocateId();
	var flag = context.allocateId();
	
	context.appendVar(flag,'true');
	context.appendFor(forId,"[].concat("+name_+')',null);
		context.appendIf(flag +'&&'+ forId+'+""===""+'+elv);
			el.setAttribute(checkName,checkName);
			context.appendVar(flag,'false');
			context.next(el)
		context.appendEnd();
	context.appendEnd();
	context.appendIf(flag);
		el.removeAttribute(checkName);
		context.next(el)
	context.appendEnd();
}
function preservedParse(){
	var oldSpace = this.getAttribute(XML_SPACE_TRIM);
	this.setAttribute(XML_SPACE_TRIM,false);
	try{
		this.next(node);
	}finally{
		this.setAttribute(XML_SPACE_TRIM,oldSpace);
	}
}
function processJS(value){
	return autoEncode(value,/^\s*JSON\s*\.*/,replaceJSON);
}
function replaceJSON(v){
	return "JSON.stringify("+v+")";
}
function replaceURI(v){
	return "encodeURIComponent("+v+")";
}
function forceURIParse(attr){
	attr.value = autoEncode(attr.value,/^\s*encodeURI*/,replaceURI);
	this.next(attr);
}
HTML.parsePre = preservedParse;
HTML.parseTextArea = preservedParse;
//if(tagName=='link'){
//}else if(/^a/i.test(tagName)){
HTML.parse2href=forceURIParse;
//	if(/^form$/i.test(tagName)){
HTML.parse2action=forceURIParse;
//if(/^(?:script|img|button)$/i.test(tagName)){
//}else if(/^(?:a|frame|iframe)$/i.test(tagName)){
HTML.parse2src=forceURIParse;
function autoEncode(value,pattern,replacer){
	var p = -1;
	var result = [];
	while(true){
		p = value.indexOf("${",++p);
		if(p>=0){
			if(!(countEescape(value,p) % 2)){
				var p2 = findELEnd(value,p+1);
				if(p2>0){
					var el = value.substring(p+2,p2);
					if(!pattern.test(el)){
						el = replacer(el);
					}
					result.push(value.substring(0,p+2),el,'}');
					value = value.substring(p2+1)
					p=-1;
				}else{
					p++;
				}
			}
		}else{
			break;
		}
	}
	if(result.length){
		result.push(value);
		return result.join('');
	}else{
		return value;
	}
}
function countEescape(text, p$) {
	if (p$ > 0 && text.charAt(p$ - 1) == '\\') {
		var pre = p$ - 1;
		while (pre-- > 0 && text.charAt(pre) == '\\')
			;
		return p$ - pre - 1;
	}
	return 0;
}
//autoEncode("${a}",/^encodeURI*/,function(a){return 'encodeURI('+a+')'})
/**/