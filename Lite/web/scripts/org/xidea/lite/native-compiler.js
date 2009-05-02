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
    var buf = [
    	'\n\tfunction _$replacer(k){return k in _$context?_$context[k]:this[k];}',
    	"\n\tvar _$context = arguments[0];",
    	"\n\tvar _$out = [];"];
    
    var context = new Context(code,0);
    for(var n in context.refs){
       if(n!= 'for'){
    	  buf.push('\n\tvar ',n,'=_$replacer("',n,'");')
       }
    }
    
    //add function
    for(var i=0;i<context.defs.length;i++){
        def = context.defs[i];
        var n = def.name;
        buf.push("\n\tfunction ",n,"(");
        for(var j=0;j<def.arguments.length;j++){
            buf.push(def.arguments[j]);
            buf.push(",")
        }
        buf.push("_$out){\n\t\tvar _$out=[];");
        appendCode(def.code,buf,context,2);
        buf.push("\n\t\treturn _$out.join('');\n\t}");
     	buf.push('\n\tif("',n,'" in _$context){',n,'=_$context["',n,'"];}')
    }
    if(context.hasFor){
        buf.push('\n\tfunction _$items(source,objectType){');
        buf.push('\n\t    if(objectType){');
        buf.push('\n\t        var result = [];');
        buf.push('\n\t        for(objectType in source){');
        buf.push('\n\t            result.push({key:objectType,value:source[objectType]})');
        buf.push('\n\t        }');
        buf.push('\n\t        return result;');
        buf.push('\n\t    }');
        buf.push('\n\t    objectType = typeof source;');
        buf.push('\n\t    return objectType == "number"? new Array(source):');
        buf.push('\n\t        objectType == "string"?source.split(""):');
        buf.push('\n\t        source instanceof Array?source:_$items(source,1);');
        buf.push('\n\t}');
    }
    if(context.needReplacer){
    	buf.push('\n\t_$replacer = function(c){return "&#"+c.charCodeAt()+";";}')
    }
    try{
        appendCode(code,buf,context,1);
    }catch(e){
        //alert(["编译失败：",buf.join(""),code])
        throw e;
    }
    buf.push("\n\treturn _$out.join('');");
    return buf.join('');
}
/**
 * _$context context
 * _$out buf
 * _$replacer xmlReplacer
 */
function appendCode(code,buf,context,depth){
	for(var i=0;i<code.length;i++){
		var item = code[i];
		printIndex(buf,depth);
		if(typeof item == 'string'){
			buf.push("_$out.push(",encodeString(item),");")
		}else{
			switch(item[0]){
            case EL_TYPE:
                processEL(item,buf);
                break;
            case XML_TEXT_TYPE:
                processXMLText(item,buf);
			    break;
            case XML_ATTRIBUTE_TYPE:
                processXMLAttribute(item,buf,context,depth);
                break;
            case VAR_TYPE:
                processVar(item,buf);
                break;
            case CAPTRUE_TYPE:
                processCaptrue(item,buf,context,depth);
                break;
            case IF_TYPE:
                i = processIf(code,i,buf,context,depth);
                break;
            case FOR_TYPE:
                i = processFor(code,i,buf,context,depth);
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
}
function processEL(item,buf){
	buf.push("_$out.push(",getEL(item[1]),");")
}
function processXMLText(item,buf){
    buf.push("_$out.push(String(",getEL(item[1]),").replace(/[<>&]/g,_$replacer));")
}
function processXMLAttribute(item,buf,context,depth){
    //[7,[[0,"value"]],"attribute"]
    var value = getEL(item[1]);
    var attributeName = item[2];
    if(attributeName){
        var testId = context.getVarId();
        printIndex(buf,depth,"var ",testId,"=",value);
        printIndex(buf,depth,"if(",testId,"!=null){");
        printIndex(buf,depth+1,"_$out.push(' ",attributeName,"=\"',String(",testId,").replace(/<>&\"/g,_$replacer),'\"')");
        printIndex(buf,depth,"}");
        context.freeVarId(testId);
    }else{
    	buf.push("_$out.push(String(",value,").replace(/[<>&\"]/g,_$replacer));")
    }
}
function processVar(item,buf){
    buf.push("var ",item[2],"=",getEL(item[1]),";");
}
function processCaptrue(item,buf,context,depth){
    var childCode = item[1];
    var varName = item[2];
    var bufbak = context.getVarId();
    buf.push("var ",bufbak,"=_$out;_$out=[];");
    appendCode(childCode,buf,context,depth);
    buf.push("var ",varName,"=_$out.join('');_$out=",bufbak,";");
    context.freeVarId(bufbak);
}
function processIf(code,i,buf,context,depth){
    var item = code[i];
    var childCode = item[1];
    var test = getEL(item[2]);
    buf.push("if(",test,"){");
    appendCode(childCode,buf,context,depth+1)
    printIndex(buf,depth,"}");
    var nextElse = code[i+1];
    var notEnd = true;
    while(nextElse && nextElse[0] == ELSE_TYPE){
        i++;
        var childCode = nextElse[1];
        var test = getEL(nextElse[2]);
        if(test){
            printIndex(buf,depth,"else if(",test,"){");
        }else{
            notEnd = false;
            printIndex(buf,depth,"else{");
        }
        appendCode(childCode,buf,context,depth+1)
        printIndex(buf,depth,"}");
        nextElse = code[i+1];
    }
    return i;
}
function processFor(code,i,buf,context,depth){
    var item = code[i];
    var indexId = context.getVarId();
    var itemsId = context.getVarId();
    var previousForValueId = context.getVarId();
    var itemsEL = getEL(item[2]);
    var varNameId = item[3]; 
    //var statusNameId = item[4]; 
    var childCode = item[1];
    var forInfo = context.getForStatus(code)
    //初始化 items 开始
    printIndex(buf,depth,"var ",itemsId,"=",itemsEL,";");
    printIndex(buf,depth,"var ",indexId,"=0;")
    printIndex(buf,depth,itemsId,"=_$items(",itemsId,")");
    
    //初始化 for状态
    var needForStatus = forInfo.ref || forInfo.index || forInfo.lastIndex;
    if(needForStatus){
        printIndex(buf,depth,"var ",previousForValueId ,"=_$context;");
        var forVar= [];
        forVar.push("_$context = {lastIndex:",itemsId,".length-1};");
        printIndex(buf,depth, forVar.join(""));
    }
    printIndex(buf,depth,"for(;",indexId,"<",itemsId,".length;",indexId,"++){");
    if(needForStatus){
        printIndex(buf,depth+1,"_$context.index=",indexId,";");
    }
    printIndex(buf,depth+1,"var ",varNameId,"=",itemsId,"[",indexId,"];");
    appendCode(childCode,buf,context,depth+1)
    printIndex(buf,depth,"}");
    
    if(needForStatus){
       printIndex(buf,depth ,"_$context=",previousForValueId);
    }
    context.freeVarId(itemsId);;
    context.freeVarId(previousForValueId);
    var nextElse = code[i+1];
    var notEnd = true;
    while(notEnd && nextElse && nextElse[0] == ELSE_TYPE){
        i++;
        var childCode = nextElse[1];
        var test = getEL(nextElse[2]);
        if(test){
            printIndex(buf,depth,"if(!",indexId,"&&",test,"){");
        }else{
            notEnd = false;
            printIndex(buf,depth,"if(!",indexId,"){");
        }
        appendCode(childCode,buf,context,depth+1)
        printIndex(buf,depth,"}");
        nextElse = code[i+1];
    }
    context.freeVarId(indexId);
    return i;
}
function printIndex(buf,depth){
    buf.push("\n");
    while(depth--){
        buf.push("\t")
    }
    if(arguments.length>2){
        depth=2;  
        while(depth<arguments.length){
            buf.push(arguments[depth++]);
        }
    }
}
function Context(code,index){
    var vs = this.vs = findStatus(code);
    this.hasFor = vs.forInfos.length;
    this.needReplacer = vs.needReplacer;
    this.defs = vs.defs;
    this.refs = vs.refs;
    this.idMap = {};
    this.index = index?index:3
}
Context.prototype = {
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
    }
}