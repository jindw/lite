/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

function findStatus(code){
	var vs = new VarStatus();
	doFind(code,vs);
	return vs;
}

function ForStatus(code){
    this.code = code;
    this.index;
    this.lastIndex;
    this.ref;
}
function VarStatus(pvs){
    this.parentStatus = pvs;
	this.needReplacer = false;
	this.vars={};
	this.refs={};
	this.defs = [];
	if(pvs){
    	this.forInfos = pvs.forInfos;
    	this.forStack = pvs.forStack;
	}else{
    	this.forInfos = [];
    	this.forStack = [];
	}
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
    	var tokens = parseEL(el)
    	var result ={};
    	this.fillVar(tokens,result);
    	for(var n in result){
    		if(!this.vars[n]){
    			this.refs[n] = true;
    		}
    	}
    },
    fillVar : function(tokens,result){
    	for(var i=0;i<tokens.length;i++){
    		var item = tokens[i];
    		if(item instanceof Array){
    			if(item[0] == VALUE_VAR){
    			    var varName = item[1];
    				result[varName] = true;
    				if(varName == 'for'){
    				    var fs = this.forStack[this.forStack.length-1];
    				    var next = tokens[i+1]
    				    if(next!=null && next[0] == OP_STATIC_GET_PROP){
    				        if(next[1] == 'index'){
    				            fs.index = true
    				        }else if(next[1] == 'lastIndex'){
    				            fs.lastIndex = true
    				        }else{
    				            fs.ref = true;
    				        }
    				    }else{
    				        fs.ref = true;
    				    }
    				}
    			}else if(item[0] == VALUE_LAZY){
    				this.fillVar(item[1],result);
    			}
    		}
    	}
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
function parseEL(el){
	return new ExpressionTokenizer(el).toTokens();
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
	var vs = new VarStatus(pvs);
	var args = el.params.slice(0);
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
				if(item[3] == '#def'){
					doFindDef(item,vs)
				}
				break;
			case FOR_TYPE:
			    vs.vistEL(item[2]);
			    vs.enterFor(item);
				doFind(item[1],vs);
				vs.exitFor();
				break;
			case XML_ATTRIBUTE_TYPE:
			case VAR_TYPE:
			case XML_TEXT_TYPE:
				vs.setNeedReplacer();
			case EL_TYPE:
			    vs.vistEL(item[1]);
				break;
			case IF_TYPE:
			    vs.vistEL(item[2]);
				doFind(item[1],vs);
				break;
			case ELSE_TYPE:
				if (item[2] != null) {
			           vs.vistEL(item[2]);
				}
				doFind(item[1],vs);
				break;
			case CAPTRUE_TYPE:
				doFind(item[1],vs);
				vs.addVar(item[2]);
				break;
			case VAR_TYPE:
				vs.vistEL(item[1]);
				vs.addVar(item[2]);
				break;
			}
        }
    }
}