/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */


var ID_PREFIX = "$_";
var XML_ENCODE_XA = 1;
var XML_ENCODE_XT = 2;
/**
 * @extends LiteContext
 */
function TranslateContext(){
	LiteContext.apply(this,arguments)
    this.idMap = {};
    this.depth = 0;
}
function LiteContext(code,params,parentContext,name){
	/**
	 * 是否需要XMLEncode 支持
	 */
	this.xmlType = 0;
	/**
	 * 本地申明变量表[参看:var 语法]
	 */
	this.varMap={};
	/**
	 * 外部引用变量表[模板中使用到的外部变量名(不包括模板申明的变量)]
	 */
	this.refMap={};
	/**
	 * 所有函数定义数组
	 */
	this.defs = [];
	/**
	 * 所有for信息数组,按深度优先出现顺序放置[_forStack 描述的是当前for深度]
	 */
    this.fors = [];
    /**
     * 当前域下的参数表[可以为null,null和空数组表示的意思不同]
     */
    this.params = params;
    this.parentContext = parentContext;
    this.name = name;
	if(parentContext){
		addVar(this,name);
		addVar(parentContext,name);
		this.fors = parentContext.fors;
		parentContext.defs.push(this);
	}
    walkCode(this,code)

}

function walkCode(vs,code){
	if(vs.params){
		for(var i=0;i<vs.params.length;i++){
		    vs.varMap[vs.params[i]] = true;
		}
	}
	vs._forStack = [];
	doFind(vs,vs.code = code);
	for(var n in vs.varMap){
    	if(vs.refMap[n]){
			vs.refMap[n] = false;
    	}
	}
    delete vs._forStack;
}
/**
 * 函数定义只能在根上,即不能在 if,for之内的子节点中
 * 函数定义是上下文无关的.
 */
function doFindDef(parentContext,item){
    if(parentContext.parentNode != null){
        var error = "函数定义不能嵌套!!"
        $log.error(error)
        throw new Error(error)
    }
    if(parentContext._forStack.length){
        var error = "函数定义不能在for 循环内!!"
        $log.error(error)
        throw new Error(error)
    }
	var el = evaluate(item[2],{});
	var params = el.params.slice(0);
	var vs = new LiteContext(item[1],params,parentContext,el.name);

}
TranslateContext.prototype = {
    findForStatus:function(code){
	    var fis = this.fors;
	    var i = fis.length;
	    while(i--){
	        var fi = fis[i];
	        if(fi.code == code){
	            return fi;
	        }
	    }
        //return this.vs.getForStatus(forCode);
    },
    allocateId:function(id){
    	if(id && /^([\w\$_]+|[\d\.]+)$/.test(id)){
    		return id;
    	}
        var i = 0;
        while(true){
            if(!this.idMap[i]){
                this.idMap[i] = true;
                return ID_PREFIX+i.toString(36);
            }
            i++;
        }
    },
    freeId:function(id){
    	var len = ID_PREFIX.length;
        if(id.substring(0,len) == ID_PREFIX){
        	delete this.idMap[id.substring(len)];
        }
    },
    /**
     */
    appendCode:function(code){
    	for(var i=0;i<code.length;i++){
    		var item = code[i];
    		if(typeof item == 'string'){
    			this.appendStatic(item)
    		}else{
    			switch(item[0]){
                case EL_TYPE:
                    this.appendEL(item);
                    break;
                case XT_TYPE:
                    this.appendXT(item);
    			    break;
                case XA_TYPE:
                    this.appendXA(item);
                    break;
                case VAR_TYPE:
                    this.appendVar(item);
                    break;
                case CAPTRUE_TYPE:
                    this.appendCaptrue(item);
                    break;
    			case PLUGIN_TYPE://not support
    				var pn = item[3]
    				if(/^(?:baidu\.)?org\.xidea\.lite\.EncodePlugin$/.test(pn)){
    					this.appendEncodePlugin(item[1][0]);
    				}else{
    					$log.error("插件类型尚未支持:"+pn);
    				}
    				break;
                case IF_TYPE:
                    i = this.processIf(code,i);
                    break;
                case FOR_TYPE:
                    i = this.processFor(code,i);
                    break;
                case ELSE_TYPE:
                	i = this.processElse(code,i);
    				break;
                default:
                    throw Error('无效指令：'+item)
                }
    		}
    	}
    },
    processElse:function(code,i){
    	throw Error('问题指令(无主else,else 指令必须紧跟if或者for)：'+code,i);
    },
    append:function(){
        var depth = this.depth;
        this.out.push("\n");
        while(depth--){
            this.out.push("\t")
        }
        for(var i=0;i<arguments.length;i++){
            this.out.push(arguments[i]);
        }
    },
    reset:function(){
    	var text = this.out.join('');
    	this.out.length=0;
    	return text;
    }
}
function ForStatus(code){
    this.code = code;
    this.index;
    this.lastIndex;
    this.ref;
    this.depth
}


function doFind(vs,code){
    for(var i=0;i<code.length;i++){
        var item = code[i];
        if(item instanceof Array){
			switch (item[0]) {
			case XA_TYPE:
				item[1] = vistEL(vs,item[1]);
				setXMLEncode(vs,XML_ENCODE_XA);
				break;
			case XT_TYPE:
				setXMLEncode(vs,XML_ENCODE_XT);
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
			case VAR_TYPE:
				item[1] = vistEL(vs,item[1]);
				addVar(vs,item[2]);
				break;
			case CAPTRUE_TYPE:
				doFind(vs,item[1]);
				addVar(vs,item[2]);
				break;
			case FOR_TYPE:
			    item[2] = vistEL(vs,item[2]);
			    enterFor(vs,item);
				doFind(vs,item[1]);
				exitFor(vs);
				break;
			case PLUGIN_TYPE:
				if(item[3] == 'org.xidea.lite.DefinePlugin'){
					doFindDef(vs,item)
				}
				break;
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
    vs.fors.push(fs)
    vs._forStack.push(fs)
}
function exitFor(vs){
    vs._forStack.pop()
}
function addVar(vs,n){
	vs.varMap[n] = true;
}
function setXMLEncode(vs,type){
    while(vs){
        vs.xmlType |= type;
        vs = vs.parentContext;
    }
}

/* ============================= */
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


