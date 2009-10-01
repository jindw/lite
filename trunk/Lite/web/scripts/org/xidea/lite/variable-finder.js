/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */


function VarStatus(code){
	this.needReplacer = false;
	this.vars={};
	this.refs={};
	this.defs = [];
    this.forInfos = [];
    this.forStack = [];
    code && doFind(code,this);
}
VarStatus.prototype = {
    setNeedReplacer : function(){
        var s = this;
        while(s){
            s.needReplacer = true;
            s = s.parentStatus;
        }
    },
    getForStatus : function(code){
        var fis = this.forInfos;
        var i = fis.length;
        while(i--){
            var fi = fis[i];
            if(fi.code == code){
                return fi;
            }
        }
    },
    addVar : function(n){
    	this.vars[n] = true;
    },
    vistEL : function(el){
    	el = new ELTranslator(el);
    	var fs = this.forStack[this.forStack.length-1];
	    if(fs){
		    fs.index = fs.index || el.forIndex;
		    fs.lastIndex = fs.lastIndex || el.forLastIndex;
		    fs.ref = fs.ref || el.forRef;
	    }
	    for(var n in el.varMap){
    		if(!this.vars[n]){
    			this.refs[n] = true;
    		}
    	}
    	return el;
    },
    enterFor:function(forCode){
        var fs = new ForStatus(forCode);
        fs.depth = this.forStack.length;
        this.forInfos.push(fs)
        this.forStack.push(fs)
    },
    exitFor:function(){
        this.forStack.pop()
    }
}

function ForStatus(code){
    this.code = code;
    this.index;
    this.lastIndex;
    this.ref;
}

/**
 * 函数定义只能在根上,即不能在 if,for之内的子节点中
 */
function doFindDef(item,pvs){
    if(pvs.parentNode != null){
        var error = "函数定义不能嵌套!!"
        $log.error(error)
        throw new Error(error)
    }
    if(pvs.forStack.length){
        var error = "函数定义不能在for 循环内!!"
        $log.error(error)
        throw new Error(error)
    }
	var el = this.eval('('+item[2]+')');
	pvs.addVar(el.name);
	var args = el.params.slice(0);
	var vs = new VarStatus(pvs);
	//forInfos，forStack 关联
    vs.parentStatus = pvs;
    vs.forInfos = pvs.forInfos;
    vs.forStack = pvs.forStack;
    
	vs.params = args;
	for(var i=0;i<args.length;i++){
	    vs.vars[args[i]] = true;
	}
	vs.name = el.name;
	vs.code = item[1];
	pvs.defs.push(vs);
	doFind(item[1],vs);
	for(var n in vs.refs){
		if(!vs.vars[n]){
			vs.refs[n] = true;
			if(!pvs.vars[n]){
			    pvs.refs[n] = true;
			}
		}
	}
}
function doFind(code,vs){
    for(var i=0;i<code.length;i++){
        var item = code[i];
        if(item instanceof Array){
			switch (item[0]) {
			case ADD_ON_TYPE:
				if(item[3] == 'org.xidea.lite.DefinePlugin'){
					doFindDef(item,vs)
				}
				break;
			case FOR_TYPE:
			    item[2] = vs.vistEL(item[2]);
			    vs.enterFor(item);
				doFind(item[1],vs);
				vs.exitFor();
				break;
			case XML_ATTRIBUTE_TYPE:
			case XML_TEXT_TYPE:
				vs.setNeedReplacer();
			case VAR_TYPE:
			case EL_TYPE:
			    item[1] = vs.vistEL(item[1]);
				break;
			case IF_TYPE:
			    item[2] = vs.vistEL(item[2]);
				doFind(item[1],vs);
				break;
			case ELSE_TYPE:
				if (item[2] != null) {
			           item[2] = vs.vistEL(item[2]);
				}
				doFind(item[1],vs);
				break;
			case CAPTRUE_TYPE:
				doFind(item[1],vs);
				vs.addVar(item[2]);
				break;
			case VAR_TYPE:
				item[1] = vs.vistEL(item[1]);
				vs.addVar(item[2]);
				break;
			}
        }
    }
}