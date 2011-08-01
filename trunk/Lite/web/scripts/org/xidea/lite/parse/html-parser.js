/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var HTML = {
	xmlns : function(){},
	
	parseScript:function(node){
		if(!node.hasAttribute('src')){
			var text = node.textContent;
			if(text!=null){
				node.textContent = processJS(text);
			}else{
				node.text = processJS(node.text);
			}
		}
		this.next(node);
	},
	"parse2on*":function(attr){
		attr.value = processJS(attr.value)
	},
	//parseStyle : function(node){
	//},
	//parse2style:function(attr){
	//},

}
function processJS(value){
	return autoEncode(attr.value,/^\s*JSON\s*\.*/,replaceJSON);
}
function replaceJSON(v){
	return "JSON.stringify("+v+")";
}
function replaceURI(v){
	return "encodeURI("+v+")";
}
function forceURIParse(attr){
	attr.value = autoEncode(attr.value,/^\s*encodeURI*/,replaceURI);
}

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