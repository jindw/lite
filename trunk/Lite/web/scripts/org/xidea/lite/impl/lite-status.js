/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */


function LiteStatus(code){
	this.needReplacer = false;
	this.varMap={};
	this.refMap={};
	this.defs = [];
    this.forInfos = [];
    if(code){
    	walkCode(this,code,[])
    }
}

function ForStatus(code){
    this.code = code;
    this.index;
    this.lastIndex;
    this.ref;
}
/**
 * 遍历Lite的表达式结构，收集表达式信息
 */
function ELStatus(tokens){
	this.tree = tokens;
	this.refMap = {};
	/**
	 * for  状态设置
	 */
	this.forIndex = false;
	this.forLastIndex = false;
	this.tree && walkEL(this,this.tree)
}
var offset = 0
var TYPE_FIX = 1<<offset++;
var TYPE_FIX2 = 1<<offset++;
var TYPE_NULL = 1<<offset++;
var TYPE_ARRAY = 1<<offset++;
var TYPE_MAP = 1<<offset++;
var TYPE_OBJECT = 1<<offset++;
var TYPE_NUMBER = 1<<offset++;
var TYPE_STRING = 1<<offset++;
var TYPE_BOOLEAN = 1<<offset++;
function getType(el){
	var op = el[0];
	var type;
	if(op<=0){
		switch(op){
		case VALUE_CONSTANTS:
			var v= el[1];
			if(v == null){
				return TYPE_FIX|TYPE_NULL;
			}
			switch(typeof v){
			case 'boolean':
				return TYPE_FIX|TYPE_BOOLEAN;
			case 'number':
				return TYPE_FIX|TYPE_NUMBER;
			case 'string':
				return TYPE_FIX|TYPE_STRING;
			case 'object':
				if(v instanceof Array){
					return TYPE_FIX|TYPE_ARRAY;
				}
				return TYPE_FIX|TYPE_OBJECT;
			}
			return typeof el[1];
		case VALUE_VAR:
			return 0;
		case VALUE_LIST:
		case VALUE_MAP:
		default:
			return TYPE_FIX;
		}
	}else{
		var arg1 = el[1];
		var arg2 = el[2];
		switch(op[0]){
		case OP_JOIN:
			return TYPE_FIX|TYPE_ARRAY;
		case OP_PUT:
			return TYPE_FIX|TYPE_MAP;
		case OP_ADD:
			//if(isNumberAdder(arg1)&&isNumberAdder(arg2)){
			//	//return 'number';
			//}else{
			return TYPE_FIX2|TYPE_NUMBER|TYPE_STRING;
			//}
		case OP_POS:
		case OP_NEG:
		case OP_MUL:
		case OP_DIV:
		case OP_MOD:
		case OP_SUB:
			return  TYPE_FIX|TYPE_NUMBER;
		case OP_NOT:
		case OP_LT:
		case OP_GT:
		case OP_LTEQ:
		case OP_GTEQ:
		case OP_EQ:
		case OP_NE:
		case OP_EQ_STRICT:
		case OP_NE_STRICT:
//		case OP_AND:
//		case OP_OR:
			return  TYPE_FIX|TYPE_BOOLEAN;
//		case OP_GET:
//			if(arg1[0] == VALUE_VAR && arg1[1] == 'for'){
//				if(op[1] == 'index' || op[1] == 'lastIndex'){
//					return 'number';
//				}
//			}
		}
	}
}


function walkEL(thiz,el){
	var op = el[0];
	if(op<=0){
		if(op == VALUE_VAR){
			var varName = el[1];
			thiz.refMap[varName] = true;
		}
		return;
	}else{
		var arg1 = el[1];
		if(op == OP_GET){
			var arg2 = el[2];
			if(arg1[0] == VALUE_VAR && arg1[1] == 'for' && arg2[0] == VALUE_CONSTANTS){
				var param = arg2[1];
				if(param == 'index'){
					thiz.forIndex = true;
				}else if(param == 'lastIndex'){
					thiz.forLastIndex = true;
				}else{
					throw new Error("for不支持属性:"+param);
				}
				return ;
			}
		}
		arg1 && walkEL(thiz,arg1);
		
		var pos = getTokenParamIndex(el[0]);
		if(pos>2){
			walkEL(thiz, el[2]);
		}
	}
}

function walkCode(vs,code,forStack){
	vs._forStack = forStack;
	doFind(vs,vs.code = code);
    delete vs._forStack;
}

function doFind(vs,code){
    for(var i=0;i<code.length;i++){
        var item = code[i];
        if(item instanceof Array){
			switch (item[0]) {
			case PLUGIN_TYPE:
				if(item[3] == 'org.xidea.lite.DefinePlugin'){
					doFindDef(vs,item)
				}
				break;
			case FOR_TYPE:
			    item[2] = vistEL(vs,item[2]);
			    enterFor(vs,item);
				doFind(vs,item[1]);
				exitFor(vs);
				break;
			case XA_TYPE:
			case XT_TYPE:
				setNeedReplacer(vs);
			case VAR_TYPE:
			case EL_TYPE:
			    item[1] = vistEL(vs,item[1]);
				break;
			case IF_TYPE:
			    item[2] = vistEL(vs,item[2]);
				doFind(vs,item[1]);
				break;
			case ELSE_TYPE:
				if (item[2] != null) {
			           item[2] = vistEL(vs,item[2]);
				}
				doFind(vs,item[1]);
				break;
			case CAPTRUE_TYPE:
				doFind(vs,item[1]);
				addVar(vs,item[2]);
				break;
			case VAR_TYPE:
				item[1] = vistEL(vs,item[1]);
				addVar(vs,item[2]);
				break;
			}
        }
    }
}
/**
 * 函数定义只能在根上,即不能在 if,for之内的子节点中
 */
function doFindDef(pvs,item){
    if(pvs.parentNode != null){
        var error = "函数定义不能嵌套!!"
        $log.error(error)
        throw new Error(error)
    }
    if(pvs._forStack.length){
        var error = "函数定义不能在for 循环内!!"
        $log.error(error)
        throw new Error(error)
    }
	var vs = new LiteStatus(null);
	var el = evaluate(item[2],{});
	var args = el.params.slice(0);
	addVar(pvs,vs.name = el.name);
	//forInfos，forStack 关联
    vs.parentStatus = pvs;
    vs.forInfos = pvs.forInfos;
	vs.params = args;
	for(var i=0;i<args.length;i++){
	    vs.varMap[args[i]] = true;
	}
	//
	
	pvs.defs.push(vs);
	walkCode(vs,item[1],pvs._forStack);
    delete vs._forStack;
    	
	for(var n in vs.refMap){
		if(!vs.varMap[n]){
			vs.refMap[n] = true;
			if(!pvs.varMap[n]){
			    pvs.refMap[n] = true;
			}
		}
	}
}
function vistEL(vs,el){
	el = new ELStatus(el);
	var fs = vs._forStack[vs._forStack.length-1];
    if(fs){
    	if(el.forIndex){fs.index =true;}
    	if(el.forLastIndex){fs.lastIndex =true;}
    	if(el.refMap['for']){fs.ref =true;}
    }
    for(var n in el.refMap){
		if(!vs.varMap[n]){
			vs.refMap[n] = true;
		}
	}
	return el;
}
function enterFor(vs,forCode){
    var fs = new ForStatus(forCode);
    fs.depth = vs._forStack.length;
    vs.forInfos.push(fs)
    vs._forStack.push(fs)
}
function exitFor(vs){
    vs._forStack.pop()
}
function addVar(vs,n){
	vs.varMap[n] = true;
}
function setNeedReplacer(vs){
    while(vs){
        vs.needReplacer = true;
        vs = vs.parentStatus;
    }
}


