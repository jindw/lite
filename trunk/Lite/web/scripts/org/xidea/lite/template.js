/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
/**
 * @public
 */
function lite__def(name,fn){
	lite__g[name] = fn||this[name];
}
/**
 * @public
 */
function lite__init(n,$_context){
	return $_context && n in $_context?$_context[n]:n in lite__g?lite__g[n]:this[n]
}
/**
 * @public
 */
function lite__list(source,result,type) {
	if (result){
		if(type == "number" && source>0){
			while(source--){
				result[source] = source+1;
			}
		}else{
			for(type in source){
				result.push(type);
			}
		}
		return result;
	}
	return source instanceof Array ? source
			: lite__list(source, [],typeof source);
}
/**
 * lite_encode(v1)
 * lite_encode(v1,/<&/g)
 * lite_encode(v1,/[<&"]|(&(?:[a-z]+|#\d+);)/ig)
 * @public
 */
function lite__encode(text,exp){
	return String(text).replace(exp||/[<&"]/g,function (c,a){
			return a || "&#"+c.charCodeAt(0)+";"
		});
}

var lite__g = {};
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
    	this.render = data(lite__def,lite__init,lite__list,lite__encode);
    }
}
//Template.prototype.render = function(context){
//	var data = this.data;
//	data(context,defs)
//}
