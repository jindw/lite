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
var OP_LT= 336;
var OP_GT= 337;
var OP_LTEQ= 338;
var OP_GTEQ= 339;
var OP_EQ= 80;
var OP_NE= 81;
var OP_EQ_STRICT= 82;
var OP_NE_STRICT= 83;
var OP_BIT_AND= 588;
var OP_BIT_XOR= 332;
var OP_BIT_OR= 76;
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
//9
addToken(OP_GET,".");
//8
addToken(OP_NOT,"!");
addToken(OP_BIT_NOT,"~");
addToken(OP_POS,"+");
addToken(OP_NEG,"-");
//7
addToken(OP_MUL,"*");
addToken(OP_DIV,"/");
addToken(OP_MOD,"%");
//6
addToken(OP_ADD,"+");
addToken(OP_SUB,"-");
//5
addToken(OP_LT,"<");
addToken(OP_GT,">");
addToken(OP_LTEQ,"<=");
addToken(OP_GTEQ,">=");
addToken(OP_EQ,"==");
addToken(OP_EQ_STRICT,"===");
addToken(OP_NE,"!=");
addToken(OP_NE_STRICT,"!==");
//4
addToken(OP_BIT_AND,"&");
addToken(OP_BIT_XOR,"^");
addToken(OP_BIT_OR,"|");
//3
addToken(OP_AND,"&&");
addToken(OP_OR,"||");

//2
addToken(OP_QUESTION,"?");
addToken(OP_QUESTION_SELECT,":");//map 中的：被直接skip了
//1
addToken(OP_JOIN,",");


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