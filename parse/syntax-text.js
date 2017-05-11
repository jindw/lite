/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
 


var findELEnd=require('./el-util').findELEnd;
var PLUGIN_DEFINE=require('./template-token').PLUGIN_DEFINE;
var parseDefName = require('./syntax-util').parseDefName;
var findLiteParamMap=require('./syntax-util').findLiteParamMap;
var appendForStart = require('./syntax-util').appendForStart;

exports.seekxa = function(text){
	var end = findELEnd(text,0);
	if(end>0){
		try{
			var el = text.substring(1,end);
			if(/^\s*([\w\-]+|"[^"]+"|'[^']+')\s*\:/.test(el)){
				var map = findLiteParamMap(el);
				for(var n in map){
					this.appendXA(n,map[n]);
				}
			}else{
				this.appendXA(null,el)
			}
	    	return end;
		}catch(e){
			console.error("XML属性表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
			return -1;
		}
	}else{
		console.warn("XML属性表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
		return -1;
	}
};
exports.seekxt = function(text){
	var end = findELEnd(text,0);
	if(end>0){
		try{
			var el = text.substring(1,end);
			this.appendXT(el)
            return end;
		}catch(e){
			console.error("XML文本表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]",e)
			return -1;
		}
	}else{
		console.warn("XML文本表达式解析异常，请检查是否手误：[fileName:"+this.currentURI+",el:"+el+"]")
		return -1;
	}
}
exports.seekEnd= function(){
	this.appendEnd();
	return 0;
}
exports.seekDef=function (text){
    var end = findELEnd(text);
	if(end>0){
		var ns = text.substring(1,end);
	    var config = parseDefName(ns);
	    this.appendPlugin(PLUGIN_DEFINE,JSON.stringify(config));
    	return end;
	}
}
exports.seekVar=function (text){
	var end = findELEnd(text);
	if(end>0){
		var value = text.substring(1,end);
		if(/^\s*(?:\w+|['"][^"]+['"])\s*$/.test(value)){
	        this.appendCapture(value.replace(/['"]/g,''));
		}else{
			var map = findLiteParamMap(value);
			for(var n in map){
				this.appendVar(n,map[n]);
			}
		}
    	return end;
	}
}
exports.seekClient= function(text){
	var end = findELEnd(text);
	if(end>0){
		var ns = text.substring(1,end);
	    var config = parseDefName(ns);
	    this.appendPlugin("org.xidea.lite.parse.ClientPlugin",JSON.stringify(config));
    	return end;
	}
}
exports.seekIf=function (text){
	var end = findELEnd(text,0);
	if(end>0){
		this.appendIf(text.substring(1,end));
		return end;
	}
}
exports.seekElse=function (text){
	if(text.charAt() == '$'){
		this.appendEnd();
		this.appendElse(null);
		return 0;
	}else{
		var end = findELEnd(text);
		if(end>0){
			this.appendEnd();
			this.appendElse(text.substring(1,end)||null);
			return end;
		}
	}
}
exports.seekElif=function (text){
	var end = findELEnd(text);
	if(end>0){
		this.appendEnd();
		this.appendElse(text.substring(1,end)||null);
		return end;
	}
}
exports.seekFor=function (text){
	var end = findELEnd(text);
	if(end>0){
		var value = text.substring(1,end);
		appendForStart(this,value);
    	return end;
	}
}
exports.seekOut=function (text){
	var end = findELEnd(text);
	if(end>0){
		var value = text.substring(1,end);
		this.appendEL(value);
		return end;
	}
}


