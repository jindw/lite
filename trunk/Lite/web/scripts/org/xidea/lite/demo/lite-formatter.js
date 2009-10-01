var TIT = {};
var EIT = {};
TIT.EL_TYPE = 0;// [0,'el']
TIT.IF_TYPE = 1;// [1,[...],'test']
TIT.BREAK_TYPE = 2;// [2,depth]
TIT.XML_ATTRIBUTE_TYPE = 3;// [3,'value','name']
TIT.XML_TEXT_TYPE = 4;// [4,'el']
TIT.FOR_TYPE = 5;// [5,[...],'items','var','status']//status
TIT.ELSE_TYPE = 6;// [6,[...],'test']//test opt?
TIT.ADD_ON_TYPE =7;// [7,[...],'var']
TIT.VAR_TYPE = 8;// [8,'value','name']
TIT.CAPTRUE_TYPE = 9;// [9,[...],'var']


//值类型（<=0）
//常量标记（String,Number,Boolean,Null）
EIT.VALUE_CONSTANTS = -0x00;//c;
EIT.VALUE_VAR = -0x01;//n;
EIT.VALUE_LAZY = -0x02;
EIT.VALUE_NEW_LIST = -0x03;//[;
EIT.VALUE_NEW_MAP = -0x04;//{;
	
//符号标记 ????? !!
//9
EIT.OP_GET_PROP = 17;//0 | 16 | 1;
EIT.OP_GET_STATIC_PROP = 48;//32 | 16 | 0;
EIT.OP_INVOKE_METHOD = 81;//64 | 16 | 1;
//8
EIT.OP_NOT = 14;//0 | 14 | 0;
EIT.OP_POS = 46;//32 | 14 | 0;
EIT.OP_NEG = 78;//64 | 14 | 0;
//7
EIT.OP_MUL = 13;//0 | 12 | 1;
EIT.OP_DIV = 45;//32 | 12 | 1;
EIT.OP_MOD = 77;//64 | 12 | 1;
//6
EIT.OP_ADD = 11;//0 | 10 | 1;
//5
EIT.OP_SUB = 41;//32 | 8 | 1;
//4
EIT.OP_LT = 7;//0 | 6 | 1;
EIT.OP_GT = 39;//32 | 6 | 1;
EIT.OP_LTEQ = 71;//64 | 6 | 1;
EIT.OP_GTEQ = 103;//96 | 6 | 1;
EIT.OP_EQ = 135;//128 | 6 | 1;
EIT.OP_NOTEQ = 167;//160 | 6 | 1;
//3
EIT.OP_AND = 5;//0 | 4 | 1;
EIT.OP_OR = 37;//32 | 4 | 1;
//2
EIT.OP_QUESTION = 3;//0 | 2 | 1;
EIT.OP_QUESTION_SELECT = 35;//32 | 2 | 1;
//1
EIT.OP_PARAM_JOIN = 1;//0 | 0 | 1;
EIT.OP_MAP_PUSH = 33;//32 | 0 | 1;
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
        var buf = ["["]
        for(var i =0;i<json.length;i++){
            if(i){
                buf.push(",");
            }
            var item = json[i];
            var type = item[0];
            buf.push("[",EIN[type]);
            if(type == EIT.VALUE_LAZY){
                buf.push(",");
                buf.push(doFormatEL(item[1],showName));
            }else{
                for(var j = 1;j<item.length;j++){
                    buf.push(",",stringifyJSON(item[j]));
                }
            }
            buf.push("]");
        }
        buf.push("]");
        return  buf.join("");
    }else{
        return stringifyJSON(json);
    }

}
