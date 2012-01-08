/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var BIT_PRIORITY= 60;
var BIT_PRIORITY_SUB= 3840;
var BIT_ARGS= 192;
var POS_INC= 12;
var VALUE_CONSTANTS= -1;
var VALUE_VAR= -2;
var VALUE_LIST= -3;
var VALUE_MAP= -4;
var OP_GET= 96;
var OP_INVOKE= 97;
var OP_NOT= 28;
var OP_BIT_NOT= 29;
var OP_POS= 30;
var OP_NEG= 31;
var OP_MUL= 88;
var OP_DIV= 89;
var OP_MOD= 90;
var OP_ADD= 84;
var OP_SUB= 85;
var OP_LSH= 80;
var OP_RSH= 81;
var OP_URSH= 82;
var OP_LT= 332;
var OP_GT= 333;
var OP_LTEQ= 334;
var OP_GTEQ= 335;
var OP_IN= 4428;
var OP_EQ= 76;
var OP_NE= 77;
var OP_EQ_STRICT= 78;
var OP_NE_STRICT= 79;
var OP_BIT_AND= 1096;
var OP_BIT_XOR= 840;
var OP_BIT_OR= 584;
var OP_AND= 328;
var OP_OR= 72;
var OP_QUESTION= 68;
var OP_QUESTION_SELECT= 69;
var OP_JOIN= 64;
var OP_PUT= 65;






var TYPE_TOKEN_MAP = {}
var TOKEN_TYPE_MAP = {}
function addToken(type,token){
	TYPE_TOKEN_MAP[type] = token;
	TOKEN_TYPE_MAP[token] = type;
}

addToken(VALUE_CONSTANTS ,"value");
addToken(VALUE_VAR       ,"var");
addToken(VALUE_LIST      ,"[]");
addToken(VALUE_MAP       ,"{}");


//九：（最高级别的运算符号）
addToken(OP_GET      ,".");
addToken(OP_INVOKE   ,"()");

//八
addToken(OP_NOT     ,"!");
addToken(OP_BIT_NOT ,"~");
addToken(OP_POS     ,"+");
addToken(OP_NEG     ,"-");

//七：
addToken(OP_MUL ,"*");
addToken(OP_DIV ,"/");
addToken(OP_MOD ,"%");

//六：
//与正负符号共享了字面值
addToken(OP_ADD ,"+");
addToken(OP_SUB ,"-");

//五:移位
addToken(OP_LSH   ,"<<");
addToken(OP_RSH   ,">>");
addToken(OP_URSH   ,">>>");

//四:比较
addToken(OP_LT   ,"<");
addToken(OP_GT   ,">");
addToken(OP_LTEQ ,"<=");
addToken(OP_GTEQ ,">=");
addToken(OP_IN   ," in ");

//四:等不等比较
addToken(OP_EQ        ,"==");
addToken(OP_NE        ,"!=");
addToken(OP_EQ_STRICT ,"===");
addToken(OP_NE_STRICT ,"!==");

//三:按位与或
addToken(OP_BIT_AND ,"&");
addToken(OP_BIT_XOR ,"^");
addToken(OP_BIT_OR  ,"|");
//三:与或
addToken(OP_AND ,"&&");
addToken(OP_OR  ,"||");

//二：
//?;
addToken(OP_QUESTION        ,"?");
//:;
addToken(OP_QUESTION_SELECT ,":");

//一：
//与Map Join 共享字面量（map join 会忽略）
addToken(OP_JOIN   ,",");
//与三元运算符共享字面值
addToken(OP_PUT   ,":");



function findTokenType(token) {
	return TOKEN_TYPE_MAP[token];
}
function findTokenText(type) {
	return TYPE_TOKEN_MAP[type];
}

function hasTokenParam(type) {
	switch (type) {
	case VALUE_VAR:
	case VALUE_CONSTANTS:
//	case OP_GET_STATIC_PROP:
//	case OP_INVOKE_WITH_STATIC_PARAM:
//	case OP_INVOKE_WITH_ONE_PARAM:
	case OP_PUT:
		return true;
	default:
		return  false;
	}
}
function getTokenParam(el) {
	return el[getTokenParamIndex(el[0])]
}

function getTokenLength(type) {
	var size = getTokenParamIndex(type);
	return hasTokenParam(type)?size+1:size;

}
//function optimizeEL(el){
//	var type = el[0];
//	var end = getTokenParamIndex(type) ;
//	if (end > 1) {//2,3
//	
//		el[1] = optimizeEL(el[1]);
//		var co = canOptimize(el[1][0]);
//		if(end>2){
//			el[2] = optimizeEL(el[2]);
//			co = co &&  canOptimize(el[2][0]);
//		}
//		if(co){
//			var o = evaluate(el, []);
//			var type = typeof o;
//			switch(type){
//				case 'string':
//				case 'boolean':
//					break;
//				case 'number':
//					if(isFinite(o)){
//						break;
//					}
//				default:
//					if(o != null){//object undefined
//						return el;
//					}
//			}
//			return [VALUE_CONSTANTS,o]
//		}
//	}
//	return el;
//}
//
//function canOptimize(type) {
//	return type == VALUE_CONSTANTS;
//}
function getTokenParamIndex(type) {
	if(type<0){
		return 1;
	}
	var c = (type & BIT_ARGS) >> 6;
	return c + 2;
}

var offset = 0
var TYPE_NULL = 1<<offset++;
var TYPE_BOOLEAN = 1<<offset++;
var TYPE_NUMBER = 1<<offset++;
var TYPE_STRING = 1<<offset++;
var TYPE_ARRAY = 1<<offset++;
var TYPE_MAP = 1<<offset++;
var TYPE_ANY = (1<<offset++) -1;

//var TYPE_NULL = 1<<offset++;
//var TYPE_BOOLEAN = 1<<offset++;
//var TYPE_NUMBER = 1<<offset++;

//var TYPE_STRING = 1<<offset++;
//var TYPE_ARRAY = 1<<offset++;
//var TYPE_MAP = 1<<offset++;
/**
 * number return true
 * string return false;
 */
function isNTSFAN(type){
	var isN = (type & TYPE_NULL) ||(type & TYPE_BOOLEAN) ||(type & TYPE_NUMBER);
	var isS = (type & TYPE_STRING) ||(type & TYPE_ARRAY) ||(type & TYPE_MAP);
	if(!isS ){
		return true;
	}
	if(!isN ){
		return false;
	}
	return null;
}
function getAddType(arg1,arg2){
	var t1 = getELType(arg1);
	var t2 = getELType(arg2);
	var ns1 = isNTSFAN(t1);
	var ns2 = isNTSFAN(t2);
	//alert([ns1,ns2])
	
	if(ns1 === false || ns2 === false){
		return TYPE_STRING;
	}
	if(ns1 === true && ns2 === true){
		return TYPE_NUMBER;
	}
	return TYPE_NUMBER|TYPE_STRING;
}
function getELType(el){
	var op = el[0];
	var type;
	if(op>0){
		var arg1 = el[1];
		var arg2 = el[2];
		switch(op){
		case OP_JOIN:
			return TYPE_ARRAY;
		case OP_PUT:
			return TYPE_MAP;
		case OP_ADD:
			//if(isNumberAdder(arg1)&&isNumberAdder(arg2)){
			//	//return 'number';
			//}else{
			return getAddType(arg1,arg2)
			//}
		case OP_POS:
		case OP_NEG:
		case OP_MUL:
		case OP_DIV:
		case OP_MOD:
		case OP_SUB:
		case OP_BIT_AND:
		case OP_BIT_XOR:
		case OP_BIT_OR:
		case OP_BIT_NOT:
			return  TYPE_NUMBER;
		case OP_NOT:
		case OP_LT:
		case OP_GT:
		case OP_LTEQ:
		case OP_GTEQ:
		case OP_EQ:
		case OP_NE:
		case OP_EQ_STRICT:
		case OP_NE_STRICT:
			return  TYPE_BOOLEAN;
		case OP_AND:
		case OP_OR:
			return  getELType(arg1) | getELType(arg2);
		case OP_GET:
			if(arg2[0] == VALUE_CONSTANTS){
				if(arg1[0] == VALUE_VAR && arg1[1] == 'for'){
					if(arg2[1] == 'index' || arg2[1] == 'lastIndex'){
						return TYPE_NUMBER;
					}
				}else if( arg2[1] == 'length'){
					var t1 = getELType(arg1);
	//var TYPE_NULL = 1<<offset++;
	//var TYPE_BOOLEAN = 1<<offset++;
	//var TYPE_NUMBER = 1<<offset++;
	
	//var TYPE_STRING = 1<<offset++;
	//var TYPE_ARRAY = 1<<offset++;
	
	//var TYPE_MAP = 1<<offset++;
					if(t1 & TYPE_MAP){
						return TYPE_ANY;
					}else if((t1 & TYPE_ARRAY) || (t1 & TYPE_STRING)){
						if((t1 & TYPE_STRING) || (t1 & TYPE_BOOLEAN)||(t1 & TYPE_NUMBER)){
							return TYPE_NULL|TYPE_NUMBER;
						}else{
							return TYPE_NUMBER;
						}
					}else{//only TYPE_STRING TYPE_BOOLEAN TYPE_NUMBER
						return TYPE_NULL;
					}
				}
			}
			return TYPE_ANY;
		case OP_INVOKE:
			if(arg1[0] == VALUE_VAR){
				switch(arg1[1]){
					case "encodeURI":
					case "encodeURIComponent":
					case "decodeURI":
					case "decodeURIComponent":
						return TYPE_STRING;
					case "parseInt":
					case "parseInt":
						return TYPE_NUMBER;
					case "isFinite":
					case "isNaN":
						return TYPE_BOOLEAN;
				}
			}else if(arg1[0] == OP_GET){
				//console.warn(uneval(arg1));
				arg2 = arg1[2];
				arg1 = arg1[1];
				if(arg2[0] == VALUE_CONSTANTS){
					var method = arg2[1];
					if(arg1[0] == VALUE_VAR){
						var owner = arg1[1];
						if(owner == 'JSON'){
							if(method == 'stringify'){
								return TYPE_STRING;
							}
						}else if(owner == 'Math'){
							return TYPE_NUMBER;
						}
					}
				}
			}
			return TYPE_ANY;
		default:
			return TYPE_ANY;
		}
	}else{
		switch(op){
		case VALUE_CONSTANTS:
			var v= el[1];
			if(v == null){
				return TYPE_NULL;
			}
			switch(typeof v){
			case 'boolean':
				return TYPE_BOOLEAN;
			case 'number':
				return TYPE_NUMBER;
			case 'string':
				return TYPE_STRING;
			case 'object':
				if(v instanceof Array){
					return TYPE_ARRAY;
				}
				return TYPE_MAP;
			}
			return TYPE_ANY;
		case VALUE_VAR:
			return TYPE_ANY;
		case VALUE_LIST:
			return TYPE_ARRAY;
		case VALUE_MAP:
			return TYPE_MAP;
		default:
			return TYPE_ANY;
		}
	}
}

/**
 * 获取某个运算符号的优先级
 */
function addELQute(parentEl,childEL,value1,value2){
	var pp = getPriority(parentEl[0]);
	var cp = getPriority(childEL[0]);
	if(value1){
		if(cp<pp){
			value1 = '('+value1+')';
		}
		return value1;
	}else if(value2 && pp>=cp){
		value2 = '('+value2+')';
	}
	return value2;
}

if(typeof require == 'function'){
exports.getTokenParam=getTokenParam;
exports.hasTokenParam=hasTokenParam;
exports.getTokenParamIndex=getTokenParamIndex;
exports.getTokenLength=getTokenLength;
exports.findTokenType=findTokenType;
exports.findTokenText=findTokenText;
exports.getELType=getELType;
exports.addELQute=addELQute;
exports.BIT_ARGS=BIT_ARGS;
exports.BIT_PRIORITY=BIT_PRIORITY;
exports.BIT_PRIORITY_SUB=BIT_PRIORITY_SUB;
exports.OP_ADD=OP_ADD;
exports.OP_AND=OP_AND;
exports.OP_BIT_AND=OP_BIT_AND;
exports.OP_BIT_NOT=OP_BIT_NOT;
exports.OP_BIT_OR=OP_BIT_OR;
exports.OP_BIT_XOR=OP_BIT_XOR;
exports.OP_DIV=OP_DIV;
exports.OP_EQ=OP_EQ;
exports.OP_EQ_STRICT=OP_EQ_STRICT;
exports.OP_GET=OP_GET;
exports.OP_GT=OP_GT;
exports.OP_GTEQ=OP_GTEQ;
exports.OP_IN=OP_IN;
exports.OP_INVOKE=OP_INVOKE;
exports.OP_JOIN=OP_JOIN;
exports.OP_LSH=OP_LSH;
exports.OP_LT=OP_LT;
exports.OP_LTEQ=OP_LTEQ;
exports.OP_MOD=OP_MOD;
exports.OP_MUL=OP_MUL;
exports.OP_NE=OP_NE;
exports.OP_NEG=OP_NEG;
exports.OP_NE_STRICT=OP_NE_STRICT;
exports.OP_NOT=OP_NOT;
exports.OP_OR=OP_OR;
exports.OP_POS=OP_POS;
exports.OP_PUT=OP_PUT;
exports.OP_QUESTION=OP_QUESTION;
exports.OP_QUESTION_SELECT=OP_QUESTION_SELECT;
exports.OP_RSH=OP_RSH;
exports.OP_SUB=OP_SUB;
exports.OP_URSH=OP_URSH;
exports.TYPE_ANY=TYPE_ANY;
exports.TYPE_ARRAY=TYPE_ARRAY;
exports.TYPE_BOOLEAN=TYPE_BOOLEAN;
exports.TYPE_MAP=TYPE_MAP;
exports.TYPE_NULL=TYPE_NULL;
exports.TYPE_NUMBER=TYPE_NUMBER;
exports.TYPE_STRING=TYPE_STRING;
exports.TYPE_TOKEN_MAP=TYPE_TOKEN_MAP;
exports.VALUE_CONSTANTS=VALUE_CONSTANTS;
exports.VALUE_LIST=VALUE_LIST;
exports.VALUE_MAP=VALUE_MAP;
exports.VALUE_VAR=VALUE_VAR;
var evaluate=require('./expression').evaluate;
var getPriority=require('./expression-tokenizer').getPriority;
}