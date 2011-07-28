/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var g = {};
function lite__impl_def(n,fn){
	g[n]=fn;
}
function lite__impl_get(n,c){
	return (c && n in c ? c:n in g?g:this)[n];
}
function replacer(c,a){return a || "&#"+c.charCodeAt(0)+";"}
function lite__impl_encode(txt,pattern){
	return String(txt).replace(pattern||/[<&"]/g,replacer);
}
function lite__impl_list(source,result,type) {
	if(source instanceof Array){
		return source;
	}
	if(result){
		if(type == "number"){
			while(source >0){
				result[--source] = source+1;
			}
		}else{
			for(type in source){
				result.push(type);
			}
		}
		return result;
	}
	return lite__impl_list(source,[],typeof source);
}
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
    	this.render = data(lite__impl_def,lite__impl_get,lite__impl_encode,lite__impl_list);
    }else{
    	var impl = $import("org.xidea.lite.impl:TemplateImpl",{});
        return new impl(data,parseContext,runAsLiteCode)
    }
}
//Template.prototype.render = function(context){
//	var data = this.data;
//	data(context,defs)
//}
