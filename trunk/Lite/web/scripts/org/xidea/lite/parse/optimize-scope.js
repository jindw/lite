/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

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
	def.params = config.params;
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
					||className =='org.xidea.lite.DatePlugin'){
					vistLite(context,item[1]);
				}else{
					console.info(item[2])
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
		return null;
	}
	var op = el[0];
	if(op<=0){
		if(op == VALUE_VAR){
			var varName = el[1];
			if(varName == 'for'){
				setForStatus(thiz,'*');
			}else{
				if(!(varName in thiz.varMap || varName in thiz.paramMap)){
					thiz.externalRefs.push(varName);
				}
				thiz.refs.push(varName);
			}
		}
		return;
	}else{
		var arg1 = el[1];
		if(op == OP_INVOKE){
			if(arg1[0] == VALUE_VAR){
				var varName = arg1[1];
				if(varName in thiz.varMap 
					//@see javadoc OptimizeUtil#walkEL
					//|| varName in thiz.paramMap)
					){
					thiz.calls.push('*');
					walkEL(thiz,arg1);
				}else{
					thiz.calls.push(varName);
				}
				//thiz.callMap[varName] = true;
			}else if(arg1[0] == OP_GET){//member
				var arg1Right = arg1[2];
				if (arg1Right[0] == VALUE_CONSTANTS
						&& (typeof arg1Right[1] == 'string')) {
					// member call
				} else {
					console.info("表达式函数调用:"+stringifyJSEL(arg1));
					thiz.calls.push("*");
				}
				walkEL(thiz, arg1);
			}else{
				//TODO:...
				walkEL(thiz,arg1);
				thiz.calls.push("*");
			}
		}else{
			if(op == OP_GET){
				var arg2 = el[2];
				if(arg1[0] == VALUE_VAR && arg1[1] == 'for' && arg2[0] == VALUE_CONSTANTS){
					var param = arg2[1];
					setForStatus(thiz,param)
					return ;
				}
			}
			arg1 && walkEL(thiz,arg1);
		}
		var pos = getTokenParamIndex(el[0]);
		if(pos>2){
			walkEL(thiz, el[2]);
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

