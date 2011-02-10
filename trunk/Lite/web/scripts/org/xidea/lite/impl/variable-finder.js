/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */


function VarStatus(code){
	this.needReplacer = false;
	this.varMap={};
	this.refMap={};
	this.defs = [];
    this.forInfos = [];
    if(code){
    	vistCode(this,code,[])
    }
}

function ForStatus(code){
    this.code = code;
    this.index;
    this.lastIndex;
    this.ref;
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
	var vs = new VarStatus(null);
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
	vistCode(vs,item[1],pvs._forStack);
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
function vistCode(vs,code,forStack){
	vs._forStack = forStack;
	doFind(vs,vs.code = code);
    delete vs._forStack;
}

function vistEL(vs,el){
	el = new ELTranslator(el);
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


