//编译期间标记，compile time object
var BRACKET_BEGIN = 0xFFFE;//([{;
var BRACKET_END = 0xFFFF;//)]};
	
//值类型（<=0）
var VALUE_VAR = -0x00;//n;
//常量标记（String,Number,Boolean,Null）
var VALUE_CONSTANTS = -0x01;//c;
var VALUE_LAZY = -0x02;
var VALUE_NEW_LIST = -0x03;//[;
var VALUE_NEW_MAP = -0x04;//{;
	
//符号标记 ????? !!

//与正负符号共享字面值
var OP_ADD = (1<<2) +2;//+;
var OP_SUB = (2<<2) +2;//-;
	
var OP_MUL = (3<<2) +2;//*;
var OP_DIV = (4<<2) +2;///;
var OP_MOD = (5<<2) +2;//%;
var OP_QUESTION = (6<<2) +2;//?;
var OP_QUESTION_SELECT = (7<<2) +2;//:;

var OP_GET_PROP = (8<<2) +2;//.;
var OP_STATIC_GET_PROP = (9<<2) +1;//.#;
	
var OP_LT = (0xA<<2) +2;//<;
var OP_GT = (0xB<<2) +2;//>;
var OP_LTEQ = (0xC<<2) +2;//<=;
var OP_GTEQ = (0xD<<2) +2;//>=;
var OP_EQ = (0xE<<2) +2;//==;
var OP_NOTEQ = (0xF<<2) +2;//!=;
var OP_AND = (0x10<<2) +2;//&&;
var OP_OR = (0x12<<2) +2;//||;
	
	

var OP_NOT = (0x13<<2) +1;//!;
var OP_POS = (0x14<<2) +1;//+;//正数
var OP_NEG = (0x15<<2) +1;//-;//负数

var OP_GET_METHOD = (0x16<<2) +2;//.();
var OP_INVOKE_METHOD = (0x17<<2) +2;//()


//与Map Join 共享字面量（map join 会忽略）
var OP_PARAM_JOIN = (0x18<<2) +2;//,
//与三元运算符共享字面值
var OP_MAP_PUSH = (0x19<<2) +2;//:,



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
		OP_GET_METHOD, "#.", OP_INVOKE_METHOD,
		"#()" // , OP_GET_GLOBAL_METHOD, "#"//method call

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
	if (type >= 3) {// op
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