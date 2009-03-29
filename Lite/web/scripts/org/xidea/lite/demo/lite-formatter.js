var TemplateType = {};
var ExpressionToken = {};
TemplateType.EL_TYPE = 0;// [0,'el']
TemplateType.IF_TYPE = 1;// [1,[...],'test']
TemplateType.BREAK_TYPE = 2;// [2,depth]
TemplateType.XML_ATTRIBUTE_TYPE = 3;// [3,'value','name']
TemplateType.XML_TEXT_TYPE = 4;// [4,'el']
TemplateType.FOR_TYPE = 5;// [5,[...],'items','var','status']//status
TemplateType.ELSE_TYPE = 6;// [6,[...],'test']//test opt?
TemplateType.ADD_ONS_TYPE =7;// [7,[...],'var']
TemplateType.VAR_TYPE = 8;// [8,'value','name']
TemplateType.CAPTRUE_TYPE = 9;// [9,[...],'var']


//值类型（<=0）
//常量标记（String,Number,Boolean,Null）
ExpressionToken.VALUE_CONSTANTS = -0x00;//c;
ExpressionToken.VALUE_VAR = -0x01;//n;
ExpressionToken.VALUE_LAZY = -0x02;
ExpressionToken.VALUE_NEW_LIST = -0x03;//[;
ExpressionToken.VALUE_NEW_MAP = -0x04;//{;
	
//符号标记 ????? !!
//9
ExpressionToken.OP_GET_PROP = 17;//0 | 16 | 1;
ExpressionToken.OP_STATIC_GET_PROP = 48;//32 | 16 | 0;
ExpressionToken.OP_INVOKE_METHOD = 81;//64 | 16 | 1;
//8
ExpressionToken.OP_NOT = 14;//0 | 14 | 0;
ExpressionToken.OP_POS = 46;//32 | 14 | 0;
ExpressionToken.OP_NEG = 78;//64 | 14 | 0;
//7
ExpressionToken.OP_MUL = 13;//0 | 12 | 1;
ExpressionToken.OP_DIV = 45;//32 | 12 | 1;
ExpressionToken.OP_MOD = 77;//64 | 12 | 1;
//6
ExpressionToken.OP_ADD = 11;//0 | 10 | 1;
//5
ExpressionToken.OP_SUB = 41;//32 | 8 | 1;
//4
ExpressionToken.OP_LT = 7;//0 | 6 | 1;
ExpressionToken.OP_GT = 39;//32 | 6 | 1;
ExpressionToken.OP_LTEQ = 71;//64 | 6 | 1;
ExpressionToken.OP_GTEQ = 103;//96 | 6 | 1;
ExpressionToken.OP_EQ = 135;//128 | 6 | 1;
ExpressionToken.OP_NOTEQ = 167;//160 | 6 | 1;
//3
ExpressionToken.OP_AND = 5;//0 | 4 | 1;
ExpressionToken.OP_OR = 37;//32 | 4 | 1;
//2
ExpressionToken.OP_QUESTION = 3;//0 | 2 | 1;
ExpressionToken.OP_QUESTION_SELECT = 35;//32 | 2 | 1;
//1
ExpressionToken.OP_PARAM_JOIN = 1;//0 | 0 | 1;
ExpressionToken.OP_MAP_PUSH = 33;//32 | 0 | 1;
var ExpressionName = {};
for(var n in ExpressionToken){
    ExpressionName[ExpressionToken[n]] = n;
}
var TemplateName = {};
for(var n in TemplateType){
    TemplateName[TemplateType[n]] = n;
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
            buf.push(JSON.encode(item));
        }else{
            var j = 0;
            var type = item[j++];
            buf.push("[",type,",");
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
    		        buf.push(doFormatEL(item2));
    		    }else{
    		        buf.push(JSON.encode(item2));
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
    return (JSON.encode(json));
}
