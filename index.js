/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

/**
 * 如果传入的是json 数组 或者是函数对象，直接作为编译结果初始化，否则，作为源代码编译。
 * @param data 模板源代码或者编译结果
 * @param parser 解析器对象，或者类名（通过jsi导入），可选
 * <a href="http://code.google.com/p/lite/wiki/Template"> 模版基础指令说明</a>
 * @public
 */
function parseLite(data,params,builtInFn){
    var parseContext = new ParseContext();
    data = parseContext.loadXML(data);
    parseContext.parse(data);
    try{
    	var translator = new JSTranslator("",params,builtInFn);
    	//translator.liteImpl = "lite_impl"
    	var code = translator.translate(parseContext.toList(),false);
        data =  new Function('return '+code).apply();
        data.toString=function(){//_$1 encodeXML
            return code;
        }
    	return data;
	 }catch(e){
	 	console.log("<code>"+code+"</code>")
	 	console.error("翻译结果错误：",e,code)
	    throw e;
	 }
    
}


if(typeof require == 'function'){
exports.parseLite=parseLite;
exports.LiteEngine=require('./lite-engine').LiteEngine;
var JSTranslator=require('./parse/js-translator').JSTranslator;
var ParseContext=require('./parse/parse-context').ParseContext;
}