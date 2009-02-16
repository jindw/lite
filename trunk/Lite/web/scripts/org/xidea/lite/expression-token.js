//编译期间标记，compile time object
var BRACKET_BEGIN = 0xFFFE;//([{;
var BRACKET_END = 0xFFFF;//)]};
	
//值类型（<=0）
//常量标记（String,Number,Boolean,Null）
var VALUE_CONSTANTS = -0x00;//c;
var VALUE_VAR = -0x01;//n;
var VALUE_LAZY = -0x02;
var VALUE_NEW_LIST = -0x03;//[;
var VALUE_NEW_MAP = -0x04;//{;
	
//符号标记 ????? !!
//9
var OP_GET_PROP = 17;//0 | 16 | 1;
var OP_STATIC_GET_PROP = 48;//32 | 16 | 0;
var OP_INVOKE_METHOD = 81;//64 | 16 | 1;
//8
var OP_NOT = 14;//0 | 14 | 0;
var OP_POS = 46;//32 | 14 | 0;
var OP_NEG = 78;//64 | 14 | 0;
//7
var OP_MUL = 13;//0 | 12 | 1;
var OP_DIV = 45;//32 | 12 | 1;
var OP_MOD = 77;//64 | 12 | 1;
//6
var OP_ADD = 11;//0 | 10 | 1;
//5
var OP_SUB = 41;//32 | 8 | 1;
//4
var OP_LT = 7;//0 | 6 | 1;
var OP_GT = 39;//32 | 6 | 1;
var OP_LTEQ = 71;//64 | 6 | 1;
var OP_GTEQ = 103;//96 | 6 | 1;
var OP_EQ = 135;//128 | 6 | 1;
var OP_NOTEQ = 167;//160 | 6 | 1;
//3
var OP_AND = 5;//0 | 4 | 1;
var OP_OR = 37;//32 | 4 | 1;
//2
var OP_QUESTION = 3;//0 | 2 | 1;
var OP_QUESTION_SELECT = 35;//32 | 2 | 1;
//1
var OP_PARAM_JOIN = 1;//0 | 0 | 1;
var OP_MAP_PUSH = 33;//32 | 0 | 1;



var OP_LIST = [
OP_ADD, "+", OP_SUB, "-", OP_MUL, "*", OP_DIV, "/",
		OP_MOD,
		"%",// +-*/%
		OP_LT, "<", OP_GT, ">", OP_LTEQ, "<=", OP_GTEQ, ">=",
		OP_EQ,
		"==",// relative
		OP_NOTEQ, "!=", OP_NOT, "!", OP_AND, "&&",
		OP_OR,
		"||",// boolean
		OP_QUESTION, "?",
		OP_QUESTION_SELECT,
		":",// 3op
		OP_POS, "+",
		OP_NEG,
		"-",// +-
		BRACKET_BEGIN, "(",
		BRACKET_END,
		")", // group
		VALUE_NEW_LIST, "[", VALUE_NEW_MAP, "{", OP_MAP_PUSH, ":",
		OP_PARAM_JOIN,
		",",// map list,
		OP_GET_PROP,
		".",// prop
		OP_INVOKE_METHOD,"#()" // , OP_GET_GLOBAL_METHOD, "#"//method call

];

function findTokenType(op) {
	for (var i = 1; i < OP_LIST.length; i += 2) {
		if (op == (OP_LIST[i])) {
			return OP_LIST[i-1];
		}
	}
	return -1;
}
function findTokenText(op) {
	for (var i = 0; i < OP_LIST.length; i += 2) {
		if (op == (OP_LIST[i])) {
			return OP_LIST[i+1];
		}
	}
	return "#"+op;
}
function toTokenString(){
	var data = this.slice(0);
	var type = data[0];
	data[0] = findTokenText(type) 
	return type+","+data.join(',')+"\n";
}



function getTokenLength(type) {
	if (type > 0) {// op
		switch (type) {
		case OP_STATIC_GET_PROP:
		case OP_MAP_PUSH:
			return 2;
		}

	} else {
		switch (type) {
		case VALUE_NEW_LIST:
		case VALUE_NEW_MAP:
			return 1;
		default:
			return 2;
		}
	}
	return 1;
}