/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var g = {}
function lite__impl_get(n,c){
	return (c && n in c ? c:n in g?g:this)[n];
};
g[1] = function(source,result,type) {
	if(source instanceof Array){
		return source;
	}
	var result = [];
	if(typeof source == 'number'){
		source = parseInt(source);
		while(source >0){
			result[--source] = source+1;
		}
	}else{
		for(source in source){
			result.push(source);
		}
	}
	return result;
};
//function replacer0(c){return "&#"+c.charCodeAt(0)+";"}
var map = {'"':'&#34;','<':'&lt;','&':'&#38;'}
function replacer(c){return map[c]||c}
g[0] = function(txt,type){
	return String(txt).replace(
		type?/&(?:\w+|#\d+|#x[\da-f]+);|[<&"]/ig:/&(?:\w+|#\d+|#0x[\da-f]+);|[<&]/ig
		,replacer);
};

function lite__impl_def(n,fn){
	g[n]=fn;
};
/**
 * 如果传入的是json 数组 或者是函数对象，直接作为编译结果初始化，否则，作为源代码编译。
 * @param data 模板源代码或者编译结果
 * @param parseContext 解析器对象，或者类名（通过jsi导入），默认为 :org.xidea.lite.parse:ParseContext
 * <a href="http://code.google.com/p/lite/wiki/Template"> 模版基础指令说明</a>
 * @public
 */
function Template(data,parseContext,runAsLiteCode){
    if(typeof data == 'function'){
	    /**
	     * 模板数据
	     * @private
	     * @tyoeof string
	     */
    	this.render = data(lite__impl_def,lite__impl_get);
    }else{
    	var impl = $import("org.xidea.lite.impl:TemplateImpl",{});
        return new impl(data,parseContext,runAsLiteCode)
    }
}
//Template.prototype.render = function(context){
//	var data = this.data;
//	data(context,defs)
//}
