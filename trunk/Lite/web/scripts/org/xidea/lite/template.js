/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var g = {};
function toList(source,result,type) {
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
function replacer(c,a){return a || "&#"+c.charCodeAt(0)+";"}
function lite__impl(type,arg1,arg2){
	if(type==3){
		//list
		return source instanceof Array ? source
			: toList(source,[],typeof source);
	}else if(type ==2){
		//encode
		return String(arg1).replace(arg2||/[<&"]/g,replacer);
	}else if(type){
		//get
		return (arg2 && arg1 in arg2 ? arg2:arg1 in g?g:this)[arg1];
	}else{
		//set
		g[arg1]=arg2;
	}
}
/**
 * 如果传入的是json 数组 或者是函数对象，直接作为编译结果初始化，否则，作为源代码编译。
 * @param data 模板源代码或者编译结果
 * @param parseContext 解析器对象，或者类名（通过jsi导入），默认为 :org.xidea.lite.parse:ParseContext
 * <a href="http://code.google.com/p/lite/wiki/Template"> 模版基础指令说明</a>
 * @public
 */
function Template(data,parseContext,runAsLiteCode){
    if(":debug"){
    	var impl = $import("org.xidea.lite.impl:TemplateImpl",{});
        return new impl(data,parseContext,runAsLiteCode)
    }else{
	    /**
	     * 模板数据
	     * @private
	     * @tyoeof string
	     */
    	this.render = data(lite__impl);
    }
}
//Template.prototype.render = function(context){
//	var data = this.data;
//	data(context,defs)
//}
