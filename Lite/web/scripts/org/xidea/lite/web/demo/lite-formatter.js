var TIT = {};
var EIT = {};
TIT.EL_TYPE = 0;// [0,'el']
TIT.IF_TYPE = 1;// [1,[...],'test']
TIT.BREAK_TYPE = 2;// [2,depth]
TIT.XML_ATTRIBUTE_TYPE = 3;// [3,'value','name']
TIT.XML_TEXT_TYPE = 4;// [4,'el']
TIT.FOR_TYPE = 5;// [5,[...],'items','var','status']//status
TIT.ELSE_TYPE = 6;// [6,[...],'test']//test opt?
TIT.PLUGIN_TYPE =7;// [7,[...],'var']
TIT.VAR_TYPE = 8;// [8,'value','name']
TIT.CAPTRUE_TYPE = 9;// [9,[...],'var']


//值类型（<=0）
//常量标记（String,Number,Boolean,Null）
//符号标记 ????? !!
//9

EIT.VALUE_CONSTANTS= -1;
EIT.VALUE_VAR= -2;
EIT.VALUE_NEW_LIST= -3;
EIT.VALUE_NEW_MAP= -4;
var OP_GET= 96;
EIT.OP_GET_STATIC_PROP= 33;
EIT.OP_INVOKE= 98;
EIT.OP_INVOKE_WITH_STATIC_PARAM= 35;
EIT.OP_INVOKE_WITH_ONE_PARAM= 352;
EIT.OP_NOT= 28;
EIT.OP_BIT_NOT= 29;
EIT.OP_POS= 30;
EIT.OP_NEG= 31;
EIT.OP_MUL= 88;
EIT.OP_DIV= 89;
EIT.OP_MOD= 90;
EIT.OP_ADD= 84;
EIT.OP_SUB= 85;
EIT.OP_LT= 4176;
EIT.OP_GT= 4177;
EIT.OP_LTEQ= 4178;
EIT.OP_GTEQ= 4179;
EIT.OP_EQ= 80;
EIT.OP_NOTEQ= 81;
EIT.OP_BIT_AND= 8268;
EIT.OP_BIT_XOR= 4172;
EIT.OP_BIT_OR= 76;
EIT.OP_AND= 4168;
EIT.OP_OR= 73;
EIT.OP_QUESTION= 68;
EIT.OP_QUESTION_SELECT= 69;
EIT.OP_PARAM_JOIN= 64;
EIT.OP_MAP_PUSH= 65;

var EIN = {};
for(var n in EIT){
    EIN[EIT[n]] = n;
}
var TIN = {};
for(var n in TIT){
    TIN[TIT[n]] = n;
}
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
    		case TIT.CAPTRUE_TYPE:
    		case TIT.IF_TYPE:
    		case TIT.ELSE_TYPE:
    		case TIT.FOR_TYPE:
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
