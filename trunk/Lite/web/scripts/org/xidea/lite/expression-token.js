/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

var BIT_PRIORITY = 15<<2;
var BIT_PRIORITY_SUB = 15<<12;
var BIT_PARAM = 3<<6;
	
//值类型（<=0）
//常量标记（String,Number,Boolean,Null）
var VALUE_CONSTANTS = -0x01;//c;
var VALUE_VAR = -0x02;//n;
var VALUE_NEW_LIST = -0x03;//[;
var VALUE_NEW_MAP = -0x04;//{;
	
//符号标记 ????? !!
//九：（最高级别的运算符号）
var OP_GET_PROP        = 0<<12 | 0<<8 | 1<<6 | 8<<2 | 0;
//0<<5 | 8<<2 |1;//.;
var OP_GET_STATIC_PROP = 0<<12 | 0<<8 | 0<<6 | 8<<2 | 1;
//1<<5 | 8<<1 |0;//.#;解析时可以忽略改节点，表达式优化的时候处理
var OP_INVOKE_METHOD                  = 0<<12 | 0<<8 | 1<<6 | 8<<2 | 2;
//2<<5 | 8<<1 |1;//()
var OP_INVOKE_METHOD_WITH_STATIC_PARAM= 0<<12 | 0<<8 | 0<<6 | 8<<2 | 3;
//3<<5 | 8<<1 |0;//()
var OP_INVOKE_METHOD_WITH_ONE_PARAM   = 0<<12 | 1<<8 | 1<<6 | 8<<2 | 0;
//4<<5 | 8<<1 |1;//()

//八：（次高级别）
var OP_NOT = 0<<12 | 0<<8 | 0<<6 | 7<<2 | 0;
//0<<5 | 7<<1 |0;//!;
var OP_POS = 0<<12 | 0<<8 | 0<<6 | 7<<2 | 1;
//1<<5 | 7<<1 |0;//+;//正数
var OP_NEG = 0<<12 | 0<<8 | 0<<6 | 7<<2 | 2;
//2<<5 | 7<<1 |0;//-;//负数

//七：
var OP_MUL = 0<<12 | 0<<8 | 1<<6 | 6<<2 | 0;
//0<<5 | 6<<1 |1;//*;
var OP_DIV = 0<<12 | 0<<8 | 1<<6 | 6<<2 | 1;
//1<<5 | 6<<1 |1;///;
var OP_MOD = 0<<12 | 0<<8 | 1<<6 | 6<<2 | 2;
//2<<5 | 6<<1 |1;//%;

//六：
//与正负符号共享了字面值
var OP_ADD = 0<<12 | 0<<8 | 1<<6 | 5<<2 | 0;
//0<<5 | 5<<1 |1;//+;//6
var OP_SUB = 0<<12 | 0<<8 | 1<<6 | 5<<2 | 1;
//1<<5 | 5<<1 |1;//-;
	

//五
var OP_LT =    0<<12 | 0<<8 | 1<<6 | 4<<2 | 0;
//0<<5 | 4<<1 |1;//<;
var OP_GT =    0<<12 | 0<<8 | 1<<6 | 4<<2 | 1;
//1<<5 | 4<<1 |1;//>;
var OP_LTEQ =  0<<12 | 0<<8 | 1<<6 | 4<<2 | 2;
//2<<5 | 4<<1 |1;//<=;
var OP_GTEQ =  0<<12 | 0<<8 | 1<<6 | 4<<2 | 3;
//3<<5 | 4<<1 |1;//>=;
	
//四:
var OP_EQ =    0<<12 | 0<<8 | 1<<6 | 3<<2 | 0;
//4<<5 | 3<<1 |1;//==;
var OP_NOTEQ = 0<<12 | 0<<8 | 1<<6 | 3<<2 | 1;

//三：
var OP_AND = 1<<12 | 0<<8 | 1<<6 | 2<<2 | 0;
//0<<5 | 2<<1 |1;//&&;
var OP_OR =  0<<12 | 0<<8 | 1<<6 | 2<<2 | 1;
//1<<5 | 2<<1 |1;//||;

//二：
var OP_QUESTION =        0<<12 | 0<<8 | 1<<6 | 1<<2 | 0;
//0<<5 | 1<<1 |1;//?;
var OP_QUESTION_SELECT = 0<<12 | 0<<8 | 1<<6 | 1<<2 | 1;
//1<<5 | 1<<1 |1;//:;

//一：
//与Map Join 共享字面量（map join 会忽略）
var OP_PARAM_JOIN = 0<<12 | 0<<8 | 1<<6 | 0<<2 | 0;
//0<<5 | 0<<1 |1;//,
//与三元运算符共享字面值
var OP_MAP_PUSH = 0<<12 | 0<<8 | 1<<6 | 0<<2 | 1;
//1<<5 | 0<<1 |1;//:,



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
		// group
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
	switch (type) {
	case VALUE_NEW_LIST:
	case VALUE_NEW_MAP:
		return 1;
	case VALUE_VAR:
	case VALUE_CONSTANTS:
		return 2;
	case OP_GET_STATIC_PROP:
	case OP_INVOKE_METHOD_WITH_STATIC_PARAM:
	case OP_MAP_PUSH:
		return 4;
	default:
		return getParamCount(type) + 1;
	}
}

function getParamCount(type) {
	var c = (type & BIT_PARAM) >> 6;
	return c + 1;
}