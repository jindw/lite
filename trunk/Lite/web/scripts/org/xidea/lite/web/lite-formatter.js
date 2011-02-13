function map(ks,ns){
	var m = {};
	var i = ks.length;
	ns = ns.split(',');
	while(i--){
		m[ks[i]] = ns[i].replace(/^VALUE_|^OP_|_TYPE$/,'');
	}
	return m;
}
var TIS = [VAR_TYPE,XA_TYPE,ELSE_TYPE,PLUGIN_TYPE,CAPTRUE_TYPE,IF_TYPE,EL_TYPE,BREAK_TYPE,XT_TYPE,FOR_TYPE];
var TNS = "VAR_TYPE,XA_TYPE,ELSE_TYPE,PLUGIN_TYPE,CAPTRUE_TYPE,IF_TYPE,EL_TYPE,BREAK_TYPE,XT_TYPE,FOR_TYPE"
var EIS = [VALUE_CONSTANTS,VALUE_VAR,VALUE_LIST,VALUE_MAP,OP_GET,OP_INVOKE,OP_NOT,OP_BIT_NOT,OP_POS,OP_NEG,OP_MUL,OP_DIV,OP_MOD,OP_ADD,OP_SUB,OP_LT,OP_GT,OP_LTEQ,OP_GTEQ,OP_EQ,OP_NE,OP_EQ_STRICT,OP_NE_STRICT,OP_BIT_AND,OP_BIT_XOR,OP_BIT_OR,OP_AND,OP_OR,OP_QUESTION,OP_QUESTION_SELECT,OP_JOIN,OP_PUT]
var ENS = "VALUE_CONSTANTS,VALUE_VAR,VALUE_LIST,VALUE_MAP,OP_GET,OP_INVOKE,OP_NOT,OP_BIT_NOT,OP_POS,OP_NEG,OP_MUL,OP_DIV,OP_MOD,OP_ADD,OP_SUB,OP_LT,OP_GT,OP_LTEQ,OP_GTEQ,OP_EQ,OP_NE,OP_EQ_STRICT,OP_NE_STRICT,OP_BIT_AND,OP_BIT_XOR,OP_BIT_OR,OP_AND,OP_OR,OP_QUESTION,OP_QUESTION_SELECT,OP_JOIN,OP_PUT"
var EIN = map(EIS,ENS);
var TIN = map(TIS,TNS);
//alert(TIN)

function liteFormat(json,showName){
    return doLiteFormat(json,"",showName);
}
function doLiteFormat(json,prefix,showName){
    var buf = [];
    buf.push(prefix,"[\n");
    for(var i=0;i<json.length;i++){
        buf.push(prefix+"\t");
        var item = json[i];
        if(typeof item == 'string'){
            buf.push(stringifyJSON(item));
        }else{
            var j = 0;
            var type = item[j++];
            buf.push("[",showName?TIN[type]:type,",");
            switch (type) {
    		case CAPTRUE_TYPE:
    		case IF_TYPE:
    		case ELSE_TYPE:
    		case FOR_TYPE:
    		    var child = item[j++];
    		    buf.push("\n",doLiteFormat(child,prefix+"\t\t",showName),",");
    			break;
    		}
    		while(j<item.length){
    		    var item2 = item[j++];
    		    if(item2 instanceof Array){
    		        buf.push(doFormatEL(item2,showName));
    		    }else{
    		        buf.push(stringifyJSON(item2));
    		    }
    		    if(j<item.length){
    		        buf.push(",");
    		    }
    		}
		    buf.push("]");
        }
		if(i+1<json.length){
		    buf.push(",\n");
		}else{
		    buf.push("\n");
		}
    }
    buf.push(prefix,"]");
    return buf.join("");
    
}

function doFormatEL(json,showName){
    if(showName){
        return stringifyLabel(json);
    }else{
        return stringifyJSON(json);
    }
}
function stringifyLabel(json){
	if(json instanceof Array){
		var buf = ["["]
	    for(var i =0;i<json.length;i++){
	        if(i){
	            buf.push(",",stringifyLabel(json[i]));
	        }else{
	        	var type = json[0];
	        	var type2 = EIN[type];
	        	if(type2){
	        		buf.push(type2);
	        	}else{
	        		buf.push(stringifyLabel(type));
	        	}
	        }
	    }
	    buf.push("]");
	    return  buf.join("");
	}
	return stringifyJSON(json);
}
