/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */


exports.OptimizeScope=OptimizeScope;

var Expression=require('js-el').Expression;
var VAR_TYPE=require('./template-token').VAR_TYPE;
var XA_TYPE=require('./template-token').XA_TYPE;
var ELSE_TYPE=require('./template-token').ELSE_TYPE;
var PLUGIN_TYPE=require('./template-token').PLUGIN_TYPE;
var CAPTURE_TYPE=require('./template-token').CAPTURE_TYPE;
var IF_TYPE=require('./template-token').IF_TYPE;
var EL_TYPE=require('./template-token').EL_TYPE;
var BREAK_TYPE=require('./template-token').BREAK_TYPE;
var XT_TYPE=require('./template-token').XT_TYPE;
var FOR_TYPE=require('./template-token').FOR_TYPE;
function OptimizeScope(code,params){

	this.code = code;
	/**
	 * @see org.xidea.lite.parse.OptimizeScope#getParams()
	 */
	this.params =  params?params.concat():[];
	/**
	 * @see org.xidea.lite.parse.OptimizeScope#getVars()
	 */
	this.vars = [];
	/**
	 * @see org.xidea.lite.parse.OptimizeScope#getCalls()
	 */
	this.calls = [];
	/**
	 * @see org.xidea.lite.parse.OptimizeScope#getRefs()
	 */
	this.refs = [];
	/**
	 * @see org.xidea.lite.parse.OptimizeScope#getExternalRefs()
	 */
	this.externalRefs = [];
	/**
	 * 所有for信息数组,按深度优先出现顺序放置[_forStack 描述的是当前for深度]
	 */
	this.fors = [];
	/**
	 * 所有函数定义数组
	 */
	this.defs = [];
	this.defMap = {};
	this.paramMap = listMap(this.params,{});
	this.varMap = {}
	this._forStack = [];
	vistLite(this,this.code = code);
    delete this._forStack;
	this.callMap =listMap(this.calls, {});
	this.refMap =listMap(this.refs, {});
	this.externalRefMap =listMap(this.externalRefs, {});
}
function listMap(list,map){
	var i = list.length;
	while(i--){
		var n = list[i];
		if(n in map){
			map[n]+=1;
		}else{
			map[n] = 1;
		}
	}
	return map;
}
function ForStatus(code){
    this.code = code;
    this.index;
    this.lastIndex;
    this.ref;
    this.depth
    //this.beforeStatus
}
function vistDef(context,item){
	var config = item[2];
	var params = config.params;
	var defaults = config.defaults;
	//def can not change!!. use cache
	var def = item[-1]||new OptimizeScope(item[1],params);
	def.name = config.name;
	def.defaults = config.defaults;
	context.fors = context.fors.concat(def.fors)
	context.defs.push(def.name);
	context.defMap[def.name] = def;
	def.defs = context.defs;
	def.defMap = context.defMap;
}
function vistLite(context,code){
	if(code == null){
		return null;
	}
    for(var i=0;i<code.length;i++){
        var item = code[i];
        if(item instanceof Array){
        	var type = item[0];
        	//el
        	switch (type) {
			case VAR_TYPE:
			case EL_TYPE:
			case XA_TYPE:
			case XT_TYPE:
				walkEL(context, item[1]);
				break;
			case IF_TYPE:
			case ELSE_TYPE:
			case FOR_TYPE:
				walkEL(context, item[2]);
				break;
			// case Template.PLUGIN_TYPE:
			// case Template.BREAK_TYPE:
			// case Template.CAPTURE_TYPE:
			// break;
			}
			//child
			switch (type) {
			case PLUGIN_TYPE:
				var className = item[2]['class'];
				if(className == 'org.xidea.lite.DefinePlugin'){
					vistDef(context,item);
				}else if(className == 'org.xidea.lite.parse.ClientPlugin'){
					//doFindClient(item);
				}else if(className == 'org.xidea.lite.EncodePlugin' 
					||className =='org.xidea.lite.DatePlugin'
					||className =='org.xidea.lite.ModulePlugin'){
					vistLite(context,item[1]);
				}else{
					console.info('unknow plugin',item[2])
				}
				break;
			case CAPTURE_TYPE:
			case IF_TYPE:
			case ELSE_TYPE:
				vistLite(context,item[1]);
				break;
			case FOR_TYPE:
			    enterFor(context,item);
			    addVar(context,item[3]);
				vistLite(context,item[1]);
				exitFor(context);
				break;
//			case XA_TYPE:
//			case XT_TYPE:
//			case EL_TYPE:
//			case VAR_TYPE:
			}
			//var
			switch(type){
			case CAPTURE_TYPE:
			case VAR_TYPE:
				addVar(context,item[2]);
				addVar(context,item[2]);
			}
        }
    }
}

function enterFor(context,forCode){
    var fs = new ForStatus(forCode);
    fs.depth = context._forStack.length;
    context.fors.push(fs)
    context._forStack.push(fs)
}
function exitFor(context){
    context._forStack.pop()
}
function addVar(context,n){
	context.vars.push(n);
	var map = context.varMap;
	if(n in map){
		map[n]+=1;
	}else{
		map[n] = 1;
	}
}


/* ============================= */
function walkEL(thiz,el){
	if(el == null){
		return;
	}
	var varMap = new Expression(el).getVarMap();
	//console.log(new Expression(el))
	for(var varName in varMap){
		var list = varMap[varName];
		var len = list.length;
		if(varName == 'for'){
		
			//
			for(var i =0;i<len;i++){
				var p = list[i];
				if(p == ''){
					setForStatus(thiz,'*');
				}else if(p == 'index' || p == 'lastIndex'){
					setForStatus(thiz,p);
				}else{
					console.error('for 不能有index，lastIndex 之外的其他属性');
					setForStatus(thiz,'*');
				}
			}
			
		}else{
			if(!(varName in thiz.varMap || varName in thiz.paramMap)){
				thiz.externalRefs.push(varName);
			}
			thiz.refs.push(varName);
		}
	}
	var callMap = new Expression(el).getCallMap();
	for(var callName in callMap){
		var list = callMap[callName];
		var len = list.length;
		for(var i =0;i<len;i++){
		if(varName in thiz.varMap 
			//@see javadoc OptimizeUtil#walkEL
			|| varName in thiz.paramMap
			){
			thiz.calls.push('*');
		}else if(varName){// ignore ''//constants
			thiz.calls.push(varName);
		}
		}
	}
}
function setForStatus(thiz,attrName){
	var fs = thiz._forStack[thiz._forStack.length-1];
	if(fs){
		if(attrName == 'index'){fs.index =true;}
		else if(attrName == 'lastIndex'){fs.lastIndex =true;}
		else if(attrName == '*'){fs.ref = true;}
		else{throw new Error("for不支持属性:"+attrName);}
	}else{
		throw new Error("for 变量不能在循环外使用:for."+attrName);
	}
}
///**
// * 遍历Lite的表达式结构，收集表达式信息
// */
//function ELStatus(tokens){
//	this.tree = tokens;
//	this.refs = [];
//	this.re
//	this.callMap = {};
//	/**
//	 * for  状态设置
//	 */
//	this.forIndex = false;
//	this.forLastIndex = false;
//	this.tree && walkEL(this,this.tree)
//}

