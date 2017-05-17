
/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
 
var findELEnd=require('./el-util').findELEnd;
var parseChildRemoveAttr=require('./syntax-util').parseChildRemoveAttr;
var compressJS=require('./js-token').compressJS;
var findXMLAttribute=require('./xml').findXMLAttribute;
var XML_SPACE_TRIM=require('./parse-xml').XML_SPACE_TRIM;
/**
 * false:preserved
 * true:trim all 
 * null:default trim muti-space to only one,(and trim first,last space)?
 * none: 
 */
var AUTO_FORM_PREFIX = "http://www.xidea.org/lite/attribute/h:autofrom" 
var AUTO_FORM_SELETED = "http://www.xidea.org/lite/attribute/h:autofrom#selected"
var HAS_LAZY_WIDGET = "http://www.xidea.org/lite/core/c:widget#has-lazy";
var WRAP_SCRIPT_USE = "http://www.xidea.org/lite/attribute/h:wrap_script_use"
var HTML = {
	xmlns : function(){},
	/* 处理IE条件注视 */
	parse$8:parseConditionComment,
	/* 处理自动表单填充 */
	parseSelect:parseSelect,
	parseOption:parseOption,
	parseInput:parseInput,
	parseTextArea:parseTextArea,
	/* 处理 script中的模板变量(JSON.stringify)*/
	parseScript : parseHtmlScript,
	parseHead:parseHtmlHead,
	parseHtml:parseHtml,
	parseLink:parseLink,

	/* 处理保留空白的html节点 */
	parsePre : preservedParse,
	//parseTextArea = preservedParse;//processed on auto form

	/* 处理 html 事件中的模板变量(JSON.stringify) (ELEMENT_NODE == 2)*/
	"onon*":parseHtmlEventAttr,
	onclass:parseHtmlClassAttr,
	
	/* 处理html 资源地址属性中的模板变量（encodeURI） */
	//if(tagName=='link'){
	//}else if(/^a/i.test(tagName)){
	onhref:autoURIEncoder,
	//	if(/^form$/i.test(tagName)){
	onaction:autoURIEncoder,
	//if(/^(?:script|img|button)$/i.test(tagName)){
	//}else if(/^(?:a|frame|iframe)$/i.test(tagName)){
	onsrc:autoURIEncoder,

}

var HTML_EXT = {
	xmlns : function(){},
	interceptAutoform:interceptAutoform,
	interceptTrim:interceptTrim,
	
	parseAutoform:interceptAutoform,
	parseTrim:interceptTrim,
}

exports.HTML=HTML;
exports.HTML_EXT=HTML_EXT;
exports.wrapScript = wrapScript;


/* html form auto value*/
function parseHtmlHead(head){
	this.setAttribute(parseHtmlHead,0);
	var doc = head.ownerDocument;
	this.next(head);
	this.setAttribute(parseHtmlHead,this.mark());
}
function __widget_arrived(id,data){
	if(data instanceof Function){
		if(/^lazy/.test(id)){
			__widget_arrived[id]=data;
		}else{
			data();
		}
	}else{
		document.querySelector('[data-widget='+id+']').innerHTML=data;
		if(data=__widget_arrived[id]){
			delete __widget_arrived[id];
			data();
		}
		
	}
}
function parseHtml(html){
	var links = [];
	this.setAttribute(parseHtml,links)
	this.next(html);
	var headMark = this.getAttribute(parseHtmlHead);
	if(headMark){
		var hasLazyWidget = this.getAttribute(HAS_LAZY_WIDGET);
		var result = this.reset(headMark);
		for(var i = 0;i<links.length;i++){
			this.next(links[i]);
		}
		if(hasLazyWidget){
			var createScript = "<script>var __widget_arrived=__widget_arrived||"+String(__widget_arrived).replace(/[\r\n\t]|\s*(=)\s*/g,'$1')+'</script>'
			this.appendText(createScript)
		}
		this.appendAll(result);
	}
	//console.log('#$$$$'+this.getAttribute(parseHtml))
}
function parseLink(link){
	var list = this.getAttribute(parseHtml)
	var headMark = this.getAttribute(parseHtmlHead);
	if(list && headMark > 0){
		list.push(link)
	}else{
		this.next(link);
	}
	//console.log(list,'#'+this.getAttribute(parseHtmlHead))
}
function parseInput(el){
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
}
function parseTextArea(el){
	var oldSpace = this.getAttribute(XML_SPACE_TRIM);
	this.setAttribute(XML_SPACE_TRIM,false);
	var autoform = this.getAttribute(AUTO_FORM_PREFIX);
	var hasValue = el.hasAttribute('value');//value added for textarea
	if(hasValue){
		el.textContent = el.getAttribute('value');
	}else if(autoform!=null && !el.hasChildNodes()){
		var name_ = el.getAttribute('name');
		el.textContent = "${"+ name_ + "}";
	}
	this.next(el);
	this.setAttribute(XML_SPACE_TRIM,oldSpace);
}
function parseSelect(el){
	var multiple = el.hasAttribute('multiple');
	this.setAttribute(AUTO_FORM_SELETED,[el.getAttribute('name'),multiple]);//不清理也无妨
	this.next(el);
}
function parseOption(el){
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
function interceptAutoform(node){
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
}



/**
 * <!--[if IE]><p>11</p><![endif]-->
 * 
 * <!--[if IE 8]><!-->
 *   <p>aa</p>
 * <!--<![endif]-->
 */
function parseConditionComment(comm){//comment
	var text = comm.textContent || comm.data;
	var match = text.match(/^\[if\s[^\]]+\]>|<!\[endif\]$/ig);
	if(match){
		if(match.length == 1){
			this.appendText('<!--'+text+'-->')
		}else{
			var len1 = match[0].length;
			var len2 = match[1].length
			var content = text.substring(len1,text.length - len2);
			try{
				if(/^\s*</.test(content)){
					content = this.loadXML(content);
				}
				//xml = this.xml.documentElement;
			}catch(e){
			}
			this.appendText('<!--'+match[0]);
			this.parse(content);
			this.appendText(match[1]+'-->');
		}
	}
	
}
/**
 * safe
 * any
 * none
 */
function interceptTrim(node){
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

function parseHtmlScript(el){
	var oldSpace = this.getAttribute(XML_SPACE_TRIM);
	this.setAttribute(XML_SPACE_TRIM,false);
	try{
		if(!el.hasAttribute('src')){
			var doc = el.ownerDocument;
			var wrap_script = this.getAttribute(WRAP_SCRIPT_USE);
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
			var source = processJS(this,buf.join(''));
			if(wrap_script){
				wrapScript(source,wrap_script)
			}
			if(source.search(/[<&]/)>=0){
				el.appendChild(doc.createTextNode('/*'));
				el.appendChild(doc.createCDATASection('*/'+source+'//'));
			}else{
				el.appendChild(doc.createTextNode(source));
			}
		}
		this.next(el);
	}finally{
		this.setAttribute(XML_SPACE_TRIM,oldSpace);
	}
}
function wrapScript(source,wrap_script_method){
	var callMatch = source.match(/^\s*([\w\.]+)\(([\s\S]*)\)\s*;?\s*$/);
	var callMethod = callMatch && callMatch[1];
	if(!/^(?:__widget_arrived|(?:\w+\.)?use)$/.test(callMethod)){
		var rexp = /\brequire\((['"][\.\-\w@\/]+["'])\)|^\s*\/(?:\/.*|\*[\s\S]*?\*\/)|'(?:[^'\\\r\n]|\\.)*'|"(?:[^"\\\r\n]|\\.)*"/g;
		var deps = [];
		var m;
		while(m = rexp.exec(source)){
			m[1] && deps.push(m[1]);
		}
		//if(deps.length){
		source = wrap_script_method + '(['+deps.join(',')+'],function(){'+source+'\n})'
		//}
	}
	return source;
}

function parseHtmlClassAttr(attr){
	attr.value = attr.value.replace(/\s*?[\r\n]\s*/g,' ');
	this.next(attr)
}
function parseHtmlEventAttr(attr){
	var value = attr.value.replace(/\s*?([\r\n])\s*/g,'$1');
	attr.value = processJS(this,value);
	this.next(attr);
}

function processJS(ctx,value){
	var value2 = value.replace(/^\s*\$\{([\s\S]+)\}\s*$/,'return $1');//当行属性脚本
	if(value2 != value){
		try{
			new Function(value2);//属性中直接插入的脚本（没有语句，不可能是json变量）
			//console.error(value2)
			return value;//直接当普通脚本输出
		}catch(e){
		}
	}
	value = compressJS(value);
	return autoEncode(value,/^\s*JSON\s*\.\s*stringify\s*\(/,replaceJSON);
}
function replaceJSON(v){
	return "JSON.stringify("+v+")";
}
function replaceURI(v){
	return "encodeURI("+v+")";
}
function autoURIEncoder(attr){
	var value = attr.value;
	attr.value = autoEncode(value,/^\s*encodeURI\s*\(/,replaceURI,encodeURI);
	this.next(attr);
}
/**
 * TODO: 自动编码压缩再解码
 */
function autoEncode(value,pattern,elReplacer,staticReplacer){
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
						el = elReplacer(el);
					}
					var prefix = value.substring(0,p);
					if(staticReplacer){
						prefix = staticReplacer(prefix);
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
	if(staticReplacer){
		value = staticReplacer(value);
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

