var ID_PREFIX = "_$";

var SAFE_FOR_KEY = "_$context";

/**
 * IE 好像容易出问题，可能是线程不安全导致。
 * @internal
 */
var stringRegexp = /["\\\x00-\x1f\x7f-\x9f]/g;
/**
 * 转义替换字符
 * @internal
 */
var charMap = {
    '\b': '\\b',
    '\t': '\\t',
    '\n': '\\n',
    '\f': '\\f',
    '\r': '\\r',
    '"' : '\\"',
    '\\': '\\\\'
};
/**
 * 转义替换函数
 * @internal
 */
function charReplacer(item) {
    var c = charMap[item];
    if (c) {
        return c;
    }
    c = item.charCodeAt().toString(16);
    return '\\u00' + (c.length>1?c:'0'+c);
}
function encodeString(value){
	stringRegexp.lastIndex = 0;
    return '"' + (stringRegexp.test(value) ?
                    value.replace(stringRegexp,charReplacer) :
                    value)
               + '"';
}

function parseNativeEL(expression){
    if(checkForExpression(expression)){
        expression = compileEL(expression)
    }
    return expression.replace(/^\s+|\s+$/g,'');;
}

function checkForExpression(expression){
    if(/\bfor\b/.test(expression)){
        try{
            checkEL(expression);
        }catch(e){
            return true;
        }
    }
}

function safeForKeyReplacer(){
    return SAFE_FOR_KEY;
}
function compileEL(el){
    if(!/['"\/]/.test(el)){
        return el.replace(/\bfor\b/g,safeForKeyReplacer)
    }
    el = " "+el;//ie6 split buf
    var forPattern = /\bfor\b/g;
    var codeBuf = el.split(forPattern);
    var forBuf= el.match(forPattern);
    if(forBuf){
        try{
            checkEL(codeBuf.join("f"));
            while(codeBuf.length>1 ){
                var codeTail = codeBuf.pop();
                var codePre = codeBuf.pop();
                var forTail = forBuf.pop();
                codeBuf.push(codePre+forTail+codeTail);
                try{
                    checkEL(codeBuf.join("f"));
                }catch(e){
                    codeBuf.pop();
                    codeBuf.push(codePre+SAFE_FOR_KEY+codeTail);
                }
            }
            return codeBuf[0];
        }catch(e){
            $log.debug(e)
            throw new Error("invalid el:"+el)
        }
    }
}

function getEL(el){
    if(typeof el == 'string'){
        return parseNativeEL(el);
    }else if(el!=null){//else 可能没有 test 属性
        throw new Error("json->el 尚未实现：（\n"+el);
    }
}



/**
 * <code>
function(context){
    function _$items(source,objectType){
        if(objectType){
            var result = [];
            for(objectType in source){
                result.push({key:objectType,value:source[objectType]})
            }
            return result;
        }
        objectType = typeof source;
        return objectType == "number"? new Array(source):
            objectType == "string"?source.split(""):
            source instanceof Array?source:_$items(source,1);
    }

	function replacer(k){return k in context?context[k]:this[k];}
	var var1 = replacer("var1")
	var var2 = replacer("var2")
	replace = function(c){return "&#"+c.charCodeAt()+";";}</code>
 */
function buildNativeJS(code){
    var context = new Context(code,0);
    context.parse();
    return context.toString();
}

function Context(code,index){
    var vs = this.vs = findStatus(code);
    this.code = code;
    this.hasFor = vs.forInfos.length;
    this.needReplacer = vs.needReplacer;
    this.defs = vs.defs;
    this.refs = vs.refs;
    this.idMap = {};
    this.depth = 1;
    this.index = index?index:3
}
Context.prototype = {
	parse:function(){
		var code = this.code;
	    this.out = [
	    	'\n\tfunction _$replacer(k){return k in _$context?_$context[k]:this[k];}',
	    	"\n\tvar _$context = arguments[0];",
	    	"\n\tvar _$out = [];"];
		for(var n in this.refs){
	       if(n!= 'for'){
	    	  this.append('var ',n,'=_$replacer("',n,'");')
	       }
	    }
	    //add function
	    for(var i=0;i<this.defs.length;i++){
	        var def = this.defs[i];
	        var n = def.name;
	        this.append("function ",n,"(",def.params.join(','),'){')
	        this.depth++;
	        this.append('var _$out=[];');
	        this.appendCode(def.code);
	        this.append("return _$out.join('');");
	        this.depth--;
	        this.append("}");
	     	this.append('if("',n,'" in _$this){',n,'=_$context["',n,'"];}')
	    }
	    if(this.hasFor){
	        this.append('function _$items(source,objectType){');
	        this.append('    if(objectType){');
	        this.append('        var result = [];');
	        this.append('        for(objectType in source){');
	        this.append('            result.push({key:objectType,value:source[objectType]})');
	        this.append('        }');
	        this.append('        return result;');
	        this.append('    }');
	        this.append('    objectType = typeof source;');
	        this.append('    return objectType == "number"? new Array(source):');
	        this.append('        objectType == "string"?source.split(""):');
	        this.append('        source instanceof Array?source:_$items(source,1);');
	        this.append('}');
	    }
	    if(this.needReplacer){
	    	this.append('_$replacer = function(c){return "&#"+c.charCodeAt()+";";}')
	    	this.append('_$replace = function(text){return String(text).replace(/[<>&"]/g,_$replacer)}')
	    }
	    try{
	        this.appendCode(code);
	    }catch(e){
	        //alert(["编译失败：",buf.join(""),code])
	        throw e;
	    }
	    this.append("return _$out.join('');");
	},
    getForStatus:function(forCode){
        return this.vs.getForStatus(forCode);
    },
    getVarId:function(){
        var i = this.index;
        while(true){
            if(!this.idMap[i]){
                this.idMap[i] = true;
                return ID_PREFIX+i.toString(36);
            }
            i++;
        }
    },
    freeVarId:function(id){
        var i = id.substring(ID_PREFIX.length);
        delete this.idMap[i];
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
    /**
     */
    appendCode:function(code){
    	for(var i=0;i<code.length;i++){
    		var item = code[i];
    		if(typeof item == 'string'){
    			this.append("_$out.push(",encodeString(item),");")
    		}else{
    			switch(item[0]){
                case EL_TYPE:
                    this.processEL(item);
                    break;
                case XML_TEXT_TYPE:
                    this.processXMLText(item);
    			    break;
                case XML_ATTRIBUTE_TYPE:
                    this.processXMLAttribute(item);
                    break;
                case VAR_TYPE:
                    this.processVar(item);
                    break;
                case CAPTRUE_TYPE:
                    this.processCaptrue(item);
                    break;
                case IF_TYPE:
                    i = this.processIf(code,i);
                    break;
                case FOR_TYPE:
                    i = this.processFor(code,i);
                    break;
    			case ADD_ON_TYPE://not support
    				break;
                //case ELSE_TYPE:
                default:
                    throw Error('valid status')
                    break;
                }
    		}
    	}
    },
    processEL:function(item){
    	this.append("_$out.push(",getEL(item[1]),");")
    },
    processXMLText:function(item){
        this.append("_$out.push(_$replace(",getEL(item[1]),"));")
    },
    processXMLAttribute:function(item){
        //[7,[[0,"value"]],"attribute"]
        var value = getEL(item[1]);
        var attributeName = item[2];
        if(attributeName){
            var testId = this.getVarId();
            this.append("var ",testId,"=",value);
            this.append("if(",testId,"!=null){");
            this.depth++;
            this.append("_$out.push(' ",attributeName,"=\"',_$replace(",testId,"),'\"')");
            this.depth--;
            this.append("}");
            this.freeVarId(testId);
        }else{
        	this.append("_$out.push(_$replace(",value,"));")
        }
    },
    processVar:function(item){
        this.append("var ",item[2],"=",getEL(item[1]),";");
    },
    processCaptrue:function(item){
        var childCode = item[1];
        var varName = item[2];
        var bufbak = this.getVarId();
        this.append("var ",bufbak,"=_$out;_$out=[];");
        this.appendCode(childCode);
        this.append("var ",varName,"=_$out.join('');_$out=",bufbak,";");
        this.freeVarId(bufbak);
    },
    processIf:function(code,i){
        var item = code[i];
        var childCode = item[1];
        var test = getEL(item[2]);
        this.append("if(",test,"){");
        this.depth++;
        this.appendCode(childCode)
        this.depth--;
        this.append("}");
        var nextElse = code[i+1];
        var notEnd = true;
        while(nextElse && nextElse[0] == ELSE_TYPE){
            i++;
            var childCode = nextElse[1];
            var test = getEL(nextElse[2]);
            if(test){
                this.append("else if(",test,"){");
            }else{
                notEnd = false;
                this.append("else{");
            }
            this.depth++;
            this.appendCode(childCode)
            this.depth--;
            this.append("}");
            nextElse = code[i+1];
        }
        return i;
    },
    processFor:function(code,i){
        var item = code[i];
        var indexId = this.getVarId();
        var itemsId = this.getVarId();
        var itemsEL = getEL(item[2]);
        var varNameId = item[3]; 
        //var statusNameId = item[4]; 
        var childCode = item[1];
        var forInfo = this.getForStatus(item)
        if(forInfo.depth){
            var previousForValueId = this.getVarId();
        }
        //初始化 items 开始
        this.append("var ",itemsId,"=",itemsEL,";");
        this.append("var ",indexId,"=0;")
        this.append(itemsId,"=_$items(",itemsId,")");
        
        //初始化 for状态
        var needForStatus = forInfo.ref || forInfo.index || forInfo.lastIndex;
        if(needForStatus){
            if(forInfo.depth){
                this.append("var ",previousForValueId ,"=_$context;");
            }
            this.append("_$context = {lastIndex:",itemsId,".length-1};");
        }
        this.append("for(;",indexId,"<",itemsId,".length;",indexId,"++){");
        this.depth++;
        if(needForStatus){
            this.append("_$context.index=",indexId,";");
        }
        this.append("var ",varNameId,"=",itemsId,"[",indexId,"];");
        this.appendCode(childCode);
        this.depth--;
        this.append("}");
        
        if(needForStatus && forInfo.depth){
           this.append("_$context=",previousForValueId);
        }
        this.freeVarId(itemsId);;
        if(forInfo.depth){
            this.freeVarId(previousForValueId);
        }
        var nextElse = code[i+1];
        var notEnd = true;
        while(notEnd && nextElse && nextElse[0] == ELSE_TYPE){
            i++;
            var childCode = nextElse[1];
            var test = getEL(nextElse[2]);
            if(test){
                this.append("if(!",indexId,"&&",test,"){");
            }else{
                notEnd = false;
                this.append("if(!",indexId,"){");
            }
            this.depth++;
            this.appendCode(childCode)
            this.depth--;
            this.append("}");
            nextElse = code[i+1];
        }
        this.freeVarId(indexId);
        return i;
    },
    toString:function(){
        return this.out.join('')
    }
}