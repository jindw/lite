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
    if(/for\s*\./g.test(expression)){
        expression = compileEL(expression)
    }
    return expression.replace(/^\s+|[\s]+$/g,'');;
}


function compileEL(el){
    if(!/'"\//.test(el)){
        return el.replace(/\bfor\s*\./g,SAFE_FOR_KEY+'.')
    }
    //好复杂的一段，这里还不够完善
    return el.replace(/(\bfor\s*\.)||'[\\.]*?'|"[\\.]*?"|[\s\S]*?/g,function(code,_for){
        if(_for){
            return FOR_KEY+'.';
        }else{
            return code;
        }
    })
}

function buildNativeJS(code){
    var buf = [
        '\tvar _$1=[];\n',
        '\tfunction _$2(c){return "&#"+c.charCodeAt()+";";}\n',
        "\twith(this){"
    ];
    var idpool = new IDPool(2);
    appendCode(code,buf,idpool,2);
    buf.push("\n\t}");
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
                printIndex(buf,depth);
			    buf.push("_$1.push(",getEL(item[1]),");")
                break;
            case XML_TEXT_TYPE:
			    buf.push("_$1.push(String(",getEL(item[1]),").replace(/[<>&]/g,_$2));")
                break;
                break;
            case XML_ATTRIBUTE_TYPE:
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
            case VAR_TYPE:
                buf.push("var ",item[2],"=",getEL(item[1]),";")
                break;
            case CAPTRUE_TYPE:
                var childCode = item[1];
                var varName = item[2];
                var bufbak = idpool.get();
                buf.push("var ",bufbak,"=_$1;_$1=[];");
                appendCode(childCode,buf,idpool,depth);
                buf.push("var ",varName,"=_$1.join('');_$1=",bufbak,";");
                idpool.free(bufbak);
                break;
            case IF_TYPE:
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
                break;
            case ELSE_TYPE:
                throw Error('valid status')
                break;
            case FOR_TYPE:
                var indexId = idpool.get();
                var itemsId = idpool.get();
                var previousForValueId = idpool.get();
                var itemsEL = getEL(item[2]);
                var varNameId = item[3]; 
                var statusNameId = item[4]; 
                var childCode = item[1];
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
                
                
                printIndex(buf,depth,"var ",previousForValueId ,"=_$0['for']");
                var forVar= statusNameId?["var ",statusNameId ,"="]:[];
                forVar.push("_$0['for'] = {lastIndex:",itemsId,".length-1,depth:",previousForValueId,"?",previousForValueId,".depth+1:0};");
                printIndex(buf,depth, forVar.join(""));
                
                printIndex(buf,depth,"for(;",indexId,"<",itemsId,".length;",indexId,"++){");
                printIndex(buf,depth+1,"_$0['for'].index=",indexId,";");
                printIndex(buf,depth+1,"var ",varNameId,"=",itemsId,"[",indexId,"];");
                appendCode(childCode,buf,idpool,depth+1)
                printIndex(buf,depth,"}");
                
                
                printIndex(buf,depth ,"_$0['for']=",previousForValueId);
                
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
                break;
            }
		}
	}
}
function getEL(el){
    if(typeof el == 'string'){
        return el;
    }else{
        throw new Error("json->el 尚未实现：（\n"+el);
    }
}
function printIndex(buf,i){
    buf.push("\n");
    while(i--){
        buf.push("\t")
    }
    if(arguments.length>2){
        var i=2;  
        while(i<arguments.length){
            buf.push(arguments[i++]);
        }
    }
}
function IDPool(index){
    this.map = {};
    this.index = index||1
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