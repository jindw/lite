if(typeof require == 'function'){
var Core=require('./xml-core-parser').Core;
}/*
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
var AUTO_FORM_PREFIX = "http://www.xidea.org/lite/attribute/h:autofrom" 
var AUTO_FORM_SELETED = "http://www.xidea.org/lite/attribute/h:autofrom#selected" 
var HTML = {
	xmlns : function(){},
	/**
	 * <!--[if IE]><p>11</p><![endif]-->
	 * 
	 * <!--[if IE 8]><!-->
	 *   <p>aa</p>
	 * <!--<![endif]-->
	 */
	parse8:function(comm){//comment
		var text = comm.textContent || comm.data;
		var match = text.match(/^\[if\s[^\]]+\]>|<!\[endif\]$/ig);
		if(match){
			if(match.length == 1){
//				match = match[0];
//				if(match.charAt() == '['){//start
//					this.append('<!--'+match);
				this.append('<!--'+text+'-->')
			}else{
				var len1 = match[0].length;
				var len2 = match[1].length
				var content = text.substring(len1,text.length - len2);
				try{
					if(/^\s*</.test(content)){
						content = loadLiteXML(content,null);
					}
					//xml = this.xml.documentElement;
				}catch(e){
				}
				this.append('<!--'+match[0]);
				this.parse(content);
				this.append(match[1]+'-->');
			}
		}
		
	},
	parseInput:function(el){
		var autoform = this.getAttribute(AUTO_FORM_PREFIX);
		if(autoform!=null){
			var name_ = el.getAttribute('name');
			//console.warn(uneval(autoform),name_);
			if(name_){
				var type = el.getAttribute('type');
				if(!/^(?:reset|button|submit)$/i.test(type)){
					if(/^(?:checkbox|radio)$/i.test(type)){
						if(!el.hasAttribute('checked')){
							buildCheck2select(this,el,name_,'checked',/checkbox/i.test(type));
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
		var multiple = el.hasAttribute('multiple');
		this.setAttribute(AUTO_FORM_SELETED,[el.getAttribute('name'),multiple]);//不清理也无妨
		this.next(el);
	},
	parseOption:function(el){
		var autoform = this.getAttribute(AUTO_FORM_PREFIX);
		if(autoform!=null){
			var name_multiple = this.getAttribute(AUTO_FORM_SELETED);
			if(name_multiple){
				buildCheck2select(this,el,name_multiple[0],'selected',name_multiple[1]);
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
			var prefix = findXMLAttribute(node,'*value');
			//console.info("#####",prefix);
			if(prefix == 'true'){
				prefix = '';
			}
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
			var value = findXMLAttribute(node,'*value');
			this.setAttribute(XML_SPACE_TRIM,value == 'true'?true:value == 'false'?false:null);
    		
//			console.error(this.getClass(),XML_SPACE_TRIM,this.getAttribute(XML_SPACE_TRIM));
    		
    		parseChildRemoveAttr(this,node);
    	}finally{
			this.setAttribute(XML_SPACE_TRIM,oldSpace);
		}
	}
}
var moveList = ['parseclient','parse2client','seekclient'];
function buildMoved(tag){
	if(tag){
		var fn = Core[tag];
		HTML_EXT[tag]= fn;
		Core[tag] = function(){
			console.info("标签:"+tag+ " 已经从core到：html-ext上了！")
			fn.apply(this,arguments);
		}
		return true;
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
			elv = JSON.stringify(value);
		}
	}
	return elv;
}
function buildCheck2select(context,el,name_,checkName,multiple){
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
	if(multiple){
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
	}else{
		context.appendIf(name_+'+""===""+'+elv);
		el.setAttribute(checkName,checkName);
		context.next(el);
		context.appendEnd();
		context.appendElse(null);
		el.removeAttribute(checkName);
		context.next(el);
		context.appendEnd();
	}
	
}

/* html parse */
function preservedParse(node){
	var oldSpace = this.getAttribute(XML_SPACE_TRIM);
	this.setAttribute(XML_SPACE_TRIM,false);
	try{
		this.next(node);
	}finally{
		this.setAttribute(XML_SPACE_TRIM,oldSpace);
	}
}
function processJS(value){
	var value2 = value.replace(/^\s*\$\{([\s\S]+)\}\s*$/,'return $1');
	if(value2 != value){
		try{
			new Function(value2);//属性中直接插入的脚本（没有语句，不可能是json变量）
			//console.error(value2)
			return value;
		}catch(e){
		}
	}
	value = compressJS(value);
	return autoEncode(value,/^\s*JSON\s*\.*/,replaceJSON);
}
function replaceJSON(v){
	return "JSON.stringify("+v+")";
}
function replaceURI(v){
	return "encodeURI("+v+")";
}
function forceURIParse(attr){
	var value = attr.value;
	attr.value = autoEncode(value,/^\s*encodeURI*/,replaceURI,encodeURI);
	this.next(attr);
}

function parseHtmlScript(el){
	var oldSpace = this.getAttribute(XML_SPACE_TRIM);
	this.setAttribute(XML_SPACE_TRIM,false);
	try{
		if(!el.hasAttribute('src')){
			var child;
			var buf = [];
			while(child = el.firstChild){
				if(child.nodeType==3 || child.nodeType == 4){//text/cdata
					buf.push(child.data);
				}else{
					console.warn('script 中不能用嵌入html标签，建议将脚本放在 <![CDATA[...]]>中。');
				}
				el.removeChild(child);
			}
			buf = processJS(buf.join(''));
			var doc = el.ownerDocument;
			if(buf.search(/[<&]/)>=0){
				el.appendChild(doc.createTextNode('/*'));
				el.appendChild(doc.createCDATASection('*/'+buf+'//'));
			}else{
				el.appendChild(doc.createTextNode(buf));
			}
		}
		this.next(el);
	}finally{
		this.setAttribute(XML_SPACE_TRIM,oldSpace);
	}
}


function parseHtmlEventAttr(attr){
	attr.value = processJS(attr.value);
	this.next(attr);
}

/* 处理 script中的模板变量(JSON.stringify)*/
HTML.parseScript = parseHtmlScript;

/* 处理 html 事件中的模板变量(JSON.stringify) (ELEMENT_NODE == 2)*/
HTML["parse2on*"] = parseHtmlEventAttr;

/* 处理html 资源地址属性中的模板变量（encodeURI） */
//if(tagName=='link'){
//}else if(/^a/i.test(tagName)){
HTML.parse2href=forceURIParse;
//	if(/^form$/i.test(tagName)){
HTML.parse2action=forceURIParse;
//if(/^(?:script|img|button)$/i.test(tagName)){
//}else if(/^(?:a|frame|iframe)$/i.test(tagName)){
HTML.parse2src=forceURIParse;

/* 处理保留空白的html节点 */

HTML.parsePre = preservedParse;
HTML.parseTextArea = preservedParse;

function autoEncode(value,pattern,replacer,replacer2){
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
					var prefix = value.substring(0,p);
					if(replacer2){
						prefix = replacer2(prefix);
					}
					result.push(prefix,'${',el,'}');
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
	if(replacer2){
		value = replacer2(value);
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
if(typeof require == 'function'){
exports.HTML=HTML;
exports.HTML_EXT=HTML_EXT;
var XML_SPACE_TRIM=require('./xml-default-parser').XML_SPACE_TRIM;
var findELEnd=require('./el-util').findELEnd;
var parseChildRemoveAttr=require('./xml-core-parser').parseChildRemoveAttr;
var compressJS=require('./js-token').compressJS;
var loadLiteXML=require('./xml').loadLiteXML;
var findXMLAttribute=require('./xml').findXMLAttribute;
var URI=require('./resource').URI;
}