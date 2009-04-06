var ID_PREFIX = "_$";

var SAFE_FOR_KEY = "_$0['for']";

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
            new Function(expression);
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
            new Function(codeBuf.join("f"));
            while(codeBuf.length>1 ){
                var codeTail = codeBuf.pop();
                var codePre = codeBuf.pop();
                var forTail = forBuf.pop();
                codeBuf.push(codePre+forTail+codeTail);
                try{
                    new Function(codeBuf.join("f"));
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
function buildNativeJS(code){
    var buf = [
        '\t_$1={};\n',
        '\tfor(_$2 in _$0){_$1[_$2]=_$0[_$2]};\n',
        '\t_$0=_$1,_$1=[];\n',
        '\t_$2=function(c){return "&#"+c.charCodeAt()+";";}\n',
        "\twith(_$0){"
    ];
    var idpool = new IDPool(2);
    try{
        appendCode(code,buf,idpool,2);
    }catch(e){
        //alert(["编译失败：",buf.join(""),code])
        throw e;
    }
    buf.push("\n\t}\n\treturn _$1.join('');");
    return buf.join('');
}
/**
 * _$0 context
 * _$1 buf
 * _$2 xmlReplacer
 */
function appendCode(code,buf,idpool,depth){
	for(var i=0;i<code.length;i++){
		var item = code[i];
		printIndex(buf,depth);
		if(typeof item == 'string'){
			buf.push("_$1.push(",encodeString(item),");")
		}else{
			switch(item[0]){
            case EL_TYPE:
                processEL(item,buf);
                break;
            case XML_TEXT_TYPE:
                processXMLText(item,buf);
			    break;
            case XML_ATTRIBUTE_TYPE:
                processXMLAttribute(item,buf,idpool,depth);
                break;
            case VAR_TYPE:
                processVar(item,buf);
                break;
            case CAPTRUE_TYPE:
                processCaptrue(item,buf,idpool,depth);
                break;
            case IF_TYPE:
                i = processIf(code,i,buf,idpool,depth);
                break;
            case FOR_TYPE:
                i = processFor(code,i,buf,idpool,depth);
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
	buf.push("_$1.push(",getEL(item[1]),");")
}
function processXMLText(item,buf){
    buf.push("_$1.push(String(",getEL(item[1]),").replace(/[<>&]/g,_$2));")
}
function processXMLAttribute(item,buf,idpool,depth){
    //[7,[[0,"value"]],"attribute"]
    var value = getEL(item[1]);
    var attributeName = item[2];
    if(attributeName){
        var testId = idpool.get();
        printIndex(buf,depth,"var ",testId,"=",value);
        printIndex(buf,depth,"if(",testId,"!=null){");
        printIndex(buf,depth+1,"_$1.push(' ",attributeName,"=\"',String(",testId,").replace(/<>&\"/g,_$2),'\"')");
        printIndex(buf,depth,"}");
        idpool.free(testId);
    }else{
    	buf.push("_$1.push(String(",value,").replace(/[<>&\"]/g,_$2));")
    }
}
function processVar(item,buf){
    buf.push("var ",item[2],"=",getEL(item[1]),";");
}
function processCaptrue(item,buf,idpool,depth){
    var childCode = item[1];
    var varName = item[2];
    var bufbak = idpool.get();
    buf.push("var ",bufbak,"=_$1;_$1=[];");
    appendCode(childCode,buf,idpool,depth);
    buf.push("var ",varName,"=_$1.join('');_$1=",bufbak,";");
    idpool.free(bufbak);
}
function processIf(code,i,buf,idpool,depth){
    var item = code[i];
    var childCode = item[1];
    var test = getEL(item[2]);
    buf.push("if(",test,"){");
    appendCode(childCode,buf,idpool,depth+1)
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
        appendCode(childCode,buf,idpool,depth+1)
        printIndex(buf,depth,"}");
        nextElse = code[i+1];
    }
    return i;
}
function processFor(code,i,buf,idpool,depth){
    var item = code[i];
    var indexId = idpool.get();
    var itemsId = idpool.get();
    var previousForValueId = idpool.get();
    var itemsEL = getEL(item[2]);
    var varNameId = item[3]; 
    //var statusNameId = item[4]; 
    var childCode = item[1];
    var childInfo = {}
    buildForChildInfo(item[1],childInfo);
    //初始化 items 开始
    printIndex(buf,depth,"var ",itemsId,"=",itemsEL,";");
    printIndex(buf,depth,"var ",indexId,"=0;")
    printIndex(buf,depth,"if(typeof ",itemsId," == 'number'){");
    printIndex(buf,depth+1,itemsId,"= new Array(",itemsId,");");
    printIndex(buf,depth,"}else if(!(",itemsId," instanceof Array)){");
    depth++
    //hack 重用变量名：previousForValueId as temp itemsId，varNameId as temp key ID
    printIndex(buf,depth,"var ",previousForValueId,"= [];");
    printIndex(buf,depth,"for(",varNameId," in ",itemsId,"){");
    printIndex(buf,depth+1,previousForValueId,".push({key:",varNameId,",value:",itemsId,"[",varNameId,"]});");
    printIndex(buf,depth,"}");
    printIndex(buf,depth,itemsId,"=",previousForValueId,";");
    depth--
    printIndex(buf,depth,"}");
    //初始化 items 结束
    
    //初始化 for状态
    var needForStatus = childInfo.hasForRef;
    if(needForStatus){
        printIndex(buf,depth,"var ",previousForValueId ,"=_$0['for'];");
        var forVar= [];
        forVar.push("_$0['for'] = {lastIndex:",itemsId,".length-1};");
        printIndex(buf,depth, forVar.join(""));
    }
    printIndex(buf,depth,"for(;",indexId,"<",itemsId,".length;",indexId,"++){");
    if(needForStatus){
        printIndex(buf,depth+1,"_$0['for'].index=",indexId,";");
    }
    printIndex(buf,depth+1,"var ",varNameId,"=",itemsId,"[",indexId,"];");
    appendCode(childCode,buf,idpool,depth+1)
    printIndex(buf,depth,"}");
    
    if(needForStatus){
       printIndex(buf,depth ,"_$0['for']=",previousForValueId);
    }
    idpool.free(itemsId);;
    idpool.free(previousForValueId);
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
        appendCode(childCode,buf,idpool,depth+1)
        printIndex(buf,depth,"}");
        nextElse = code[i+1];
    }
    idpool.free(indexId);
    return i;
}
/*
 * hasForRef
 * hasForChild
 */
function buildForChildInfo(code,childInfo){
    for(var i=0;i<code.length;i++){
        var item = code[i];
        if(item instanceof Array){
			switch (item[0]) {
			case ADD_ON_TYPE:
				continue;// continue for（not support）
			case XML_ATTRIBUTE_TYPE:
			case VAR_TYPE:
			case XML_TEXT_TYPE:
			case EL_TYPE:
			    if(!childInfo.hasForRef){
		           childInfo.hasForRef = checkForExpression(item[1]);
			    }
				break;
			case FOR_TYPE:
			    childInfo.hasForChild = true;
			    if(!childInfo.hasForRef){
		           childInfo.hasForRef = checkForExpression(item[2]);
			    }
			    if(childInfo.hasForRef){
			        return;
			    }
				buildForChildInfo(item[1],childInfo);
				break;
			case IF_TYPE:
			case ELSE_TYPE:
				if (item[2] != null) {
				    if(!childInfo.hasForRef){
			           childInfo.hasForRef = checkForExpression(item[2]);
				    }
				}
			case CAPTRUE_TYPE:
				// children
				buildForChildInfo(item[1],childInfo);
				break;
			}
        }
		if(childInfo.hasForRef && childInfo.hasForChild){
			return;
		}
    }
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
function IDPool(index){
    this.map = {};
    this.index = index?index:2
}
IDPool.prototype = {
    get:function(){
        var i = this.index;
        while(true){
            if(!this.map[i]){
                this.map[i] = true;
                return ID_PREFIX+i.toString(36);
            }
            i++;
        }
    },
    free:function(id){
        var i = id.substring(ID_PREFIX.length);
        delete this.map[i];
    }
}