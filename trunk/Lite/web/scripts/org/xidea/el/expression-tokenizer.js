if(typeof require == 'function'){
var JSONTokenizer=require('./json-tokenizer').JSONTokenizer;
}/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

//编译期间标记，compile time object
var BRACKET_BEGIN = 0xFFFE;//([{;
var BRACKET_END = 0xFFFF;//)]};

var STATUS_BEGIN = -100;
var STATUS_EXPRESSION = -101;
var STATUS_OPERATOR = -102;
var fns = {
	getResult :function() {
		return this.expression;
		//return optimizeEL(this.expression);//.slice(0).reverse();// reversed
	},

	parseEL :function() {
		this.skipSpace(0);
		while (this.start < this.end) {
			var c = this.value.charAt(this.start);
			if (c == '"' || c == '\'') {
				var text = this.findString();
				this.addKeyOrObject(text, false);
			} else if (c >= '0' && c <= '9') {
				var number = this.findNumber();
				this.addKeyOrObject(number, false);
			} else if (/[\w$_]/.test(c)) {
				var id = this.findId();
				switch(id){
				    case 'true':
				        this.addToken([VALUE_CONSTANTS,true]);
				        break;
				    case 'false':
				        this.addToken([VALUE_CONSTANTS,false]);
				        break;
				    case 'null':
				        this.addToken([VALUE_CONSTANTS,null]);
				        break;
//				    case 'in':
//				        this.addToken([OP_IN,null]);
//				        break;
				    default:
    				    this.skipSpace(0);
    					if (this.previousType == OP_GET) {
    						this.addToken([VALUE_CONSTANTS,
    								id]);
    					} else {
    						this.addKeyOrObject(id, true);
    					}
				}
			} else {
				var op = this.findOperator();
				// if (this.value.startsWith(op, this.start))
				this.parseOperator(op);
				if (op == null) {
					this.parseError("未知操作符:");
				}
			}
			this.skipSpace(0);
		}
	},
	parseError:function(msg){
	    msg = msg+"\n@"+ this.start + "\n"
				+ this.value.substring(this.start)+"\n----\n"+this.value
		console.error(msg);
		throw new Error(msg);
	},
	findOperator :function() {// optimize json ,:[{}]
		var c = this.value.charAt(this.start);
		var end = this.start+1;
		var next = this.value.charAt(end);
		switch (c) {
		case ',':// optimize for json
		case ':':// 3op,map key
		case '[':// list
		case ']':
		case '{':// map
		case '}':
		case '(':// quote
		case ')':
		case '.':// prop
		case '?':// 3op
		case '~':
		case '^':
			break;
		case '+':// 5op
		case '-':
		case '*':
		case '/':
		case '%':
			if(next == '=' ){
				this.parseError("不支持赋值操作:");
			}else if(next == c){
				this.parseError("不支持自增自减操作:");
			}
			break;
		case '=':// ==
			if(next == '='){
				end++;
				if(this.value.charAt(end) == '='){
					end++;
				}
			}else{
				this.parseError("不支持赋值操作:");
			}
			break;
		case '!':// !,!=
			if(next == '='){
				end++;
				if(this.value.charAt(end) == '='){
					end++;
				}
			}
			break;
		case '>':// >,>=
		case '<':// <,<=
			if (next == '=') {
				end++;
			}else if(next == c){
				if(this.value.charAt(end) == c){
					end++;
				}
			}
			break;
		case '&':// && / &
		case '|':// || /|
			if( (c == next)){
				end++;
			}
			break;
		default:
			return null;
		}
		return this.value.substring(this.start, this.start = end);
	},

	/**
	 * 碰見:和,的時候，就需要檢查是否事map的間隔符號了
	 * 
	 * @return
	 */
	isMapMethod :function() {
		var i = this.tokens.length - 1;
		var depth = 0;
		for (; i >= 0; i--) {
			var token = this.tokens[i];
			var type = token[0];
			if (depth == 0) {
				if (type == OP_PUT
						|| type == VALUE_MAP) {// (
					// <#newMap>
					// <#push>
					return true;
				} else if (type == OP_JOIN) {// (
					// <#newList>
					// <#param_join>
					return false;
				}
			}
			if (type == BRACKET_BEGIN) {
				depth--;
			} else if (type == BRACKET_END) {
				depth++;
			}
		}
		return false;
	},

	parseOperator :function(op) {
		if (op.length == 1) {
			switch (op.charAt(0)) {
			case '(':
				if (this.status == STATUS_EXPRESSION) {
					this.addToken([OP_INVOKE]);
					if (this.skipSpace(')')) {
						this.addToken([VALUE_CONSTANTS,
								[]]);
						this.start++;
					} else {
						this.addList();
					}

				} else {
					this.addToken([BRACKET_BEGIN]);
				}
				break;
			case '[':
				if (this.status == STATUS_EXPRESSION) {// getProperty
					this.addToken([OP_GET]);
					this.addToken([BRACKET_BEGIN]);
				}else {// list
					this.addList();
				}
				break;
			case '{':
				this.addMap();
				break;
			case '}':
			case ']':
			case ')':
				this.addToken([BRACKET_END]);
				break;
			case '+'://
				this.addToken([
						this.status == STATUS_EXPRESSION ? OP_ADD : OP_POS]);
				break;
			case '-':
				this.addToken([
						this.status == STATUS_EXPRESSION ? OP_SUB
								: OP_NEG]);
				break;
			case ':':
				this.addToken([OP_QUESTION_SELECT]);// map : is skipped
				break;
			case ',':// :(object_setter is skiped,',' should
				// be skip)
				if (this.isMapMethod()) {
					
					this.status = STATUS_OPERATOR;
				}else{
					this.addToken([OP_JOIN]);

				}
				break;
			case '/':
				var next = this.value.charAt(this.start);
				if (next == '/') {
					var end1 = this.value.indexOf('\n', this.start);
					var end2 = this.value.indexOf('\r', this.start);
					var cend = Math.min(end1, end2);
					if (cend < 0) {
						cend = Math.max(end1, end2);
					}
					if (cend > 0) {
						this.start = cend;
					} else {
						this.start = this.end;
					}
					break;
				} else if (next == '*') {
					var cend = this.value.indexOf("*/", this.start);
					if (cend > 0) {
						this.start = cend + 2;
					} else {
						throw new Error("未結束注釋:" + this.value
								+ "@" + this.start);
					}
					break;
				}else if(this.status != STATUS_EXPRESSION){
					var end = findRegExp(this.value,this.start);
					if(end>0){
						this.addToken([VALUE_CONSTANTS,
							toValue(
								this.value.substring(this.start-1,end))]);
						this.start = end;
						break;
					}else{
						throw new Error("异常正则:"+this.value+'@'+this.start)
					}
				//}else{
				//	this.addToken([findTokenType(op)]);// /
				}
			default:
				this.addToken([findTokenType(op)]);
			}
		} else {
			this.addToken([findTokenType(op)]);
		}
	},

	addToken :function(token) {
		var type= token[0];
		if(type == VALUE_VAR){
			if("in" == token[1]){
				token[0] = type = OP_IN;
			}
		}
		
		switch (type) {
		case BRACKET_BEGIN:
			this.status = STATUS_BEGIN;
			break;
		case VALUE_CONSTANTS:
		case VALUE_VAR:
		case BRACKET_END:
			this.status = STATUS_EXPRESSION;
			break;
		default:
			this.status = STATUS_OPERATOR;
			break;
		}
		// previousType2 = this.previousType;
		this.previousType = type;
		this.tokens.push(token);
	},

	addKeyOrObject :function(object, isVar) {
		if (this.skipSpace(':') && this.isMapMethod()) {// object key
			this.addToken([OP_PUT, object]);
			this.start++;// skip :
		} else if (isVar) {
			this.addToken([VALUE_VAR, object]);
		} else {
			this.addToken([VALUE_CONSTANTS, object]);
		}
	},

	addList :function() {
		this.addToken([BRACKET_BEGIN]);
		this.addToken([VALUE_LIST]);
		if (!this.skipSpace(']')) {
			this.addToken([OP_JOIN]);
		}
	},

	addMap :function() {
		this.addToken([BRACKET_BEGIN]);
		this.addToken([VALUE_MAP]);
	}
};
var pt = new JSONTokenizer('');
for(var n in fns){
    pt[n] = fns[n]
}
function toValue(s){
    var v= this.eval(s);
    if(v instanceof RegExp){
    	v = {
            "class":"RegExp",
    		'literal':s+''
    	}
    }
    return v;
}
function findRegExp(text,start){
	var depth=0,c;
	while(c = text.charAt(start++)){
	    if(c=='['){
	    	depth = 1;
	    }else if(c==']'){
	    	depth = 0;
	    }else if (c == '\\') {
	        start++;
	    }else if(depth == 0 && c == '/'){
	    	while(c = text.charAt(start++)){
	    		switch(c){
	    			case 'g':
	    			case 'i':
	    			case 'm':
	    			break;
	    			default:
	    			return start-1;
	    		}
	    	}
	    	
	    }
	}
}
/**
 * 表达式解析器，将JS表达式文本解析成JSON中间代码
 */
function ExpressionTokenizer(value){
    this.value = value.replace(/^\s+|\s+$/g,'');
	this.start = 0;
	this.end = this.value.length;
    this.status = STATUS_BEGIN;
	this.previousType = STATUS_BEGIN;
	this.tokens = [];
	this.parseEL();
	prepareSelect(this.tokens)
	this.expression = buildTree(trimToken(right(this.tokens)));
}

function prepareSelect(tokens) {
	var p1 = tokens.length;
	while (p1--) {
		var type1 = tokens[p1][0];
		if (type1 == OP_QUESTION) { // (a?b
			var pos = getSelectRange(tokens,p1, -1, -1);
			tokens.splice(pos+1,0, [BRACKET_BEGIN]);
			p1++;
		} else if (type1 == OP_QUESTION_SELECT) {
			var end = tokens.length;
			var pos = getSelectRange(tokens,p1, 1, end);
			tokens.splice(pos,0, [BRACKET_END]);
		}
	}
}
function getSelectRange(tokens,p2, inc, end) {
	var dep = 0;
	while ((p2 += inc) != end) {
		var type2 = tokens[p2][0];
		if (type2 > 0) {// op
			if (type2 == BRACKET_BEGIN) {
				dep += inc;
			} else if (type2 == BRACKET_END) {
				dep -= inc;
			} else if (dep == 0 && getPriority(type2) <= getPriority(OP_QUESTION)) {
				return p2;
			}
			if (dep < 0) {
				return p2;
			}
		}
	}
	return inc > 0 ? end : -1;
}
function buildTree(tokens){
	var stack = [];
    for(var i=0;i<tokens.length;i++){
        var item = tokens[i]
        var type = item[0];
        switch(type){
            case VALUE_CONSTANTS:
            case VALUE_VAR:
            case VALUE_LIST:
            case VALUE_MAP:
                stack.push(item);
                break;
            default://OP
                if(getTokenParamIndex(type) ==3){//两个操作数
                    var arg2 = stack.pop();
                    var arg1 = stack.pop();
                    var el = [type,arg1,arg2]
                }else{//一个操作树
                	var arg1 = stack.pop();
                	var el = [type,arg1]
                }
                if(hasTokenParam(type)){
					el[getTokenParamIndex(type)] = item[1];
                }
                stack.push(el)
        }
    }
    return stack[0];
}
ExpressionTokenizer.prototype = pt;



// 将中序表达式转换为右序表达式
function right(tokens) {
	var rightStack = [[]];
	var buffer = [];

	for (var i = 0;i<tokens.length;i++) {
		var item = tokens[i];
		if (item[0] > 0) {
			if (buffer.length == 0) {
				buffer.push(item);
			} else if (item[0] == BRACKET_BEGIN) {// ("(")
				buffer.push(item);
			} else if (item[0] == BRACKET_END) {// .equals(")"))
				while (true) {
					var operator = buffer.pop();
					if (operator[0] == BRACKET_BEGIN) {
						break;
					}
					addRightToken(rightStack, operator);
				}
			} else {
				while (buffer.length!=0
						&& rightEnd(item[0], buffer[buffer.length-1][0])) {
					var operator = buffer.pop();
					// if (operator[0] !=
					// BRACKET_BEGIN){
					addRightToken(rightStack, operator);
				}
				buffer.push(item);
			}
		} else {
			addRightToken(rightStack, item);
		}
	}
	while (buffer.length !=0) {
		var operator = buffer.pop();
		addRightToken(rightStack, operator);
	}
	return rightStack[rightStack.length-1];
}
function trimToken(tokens){
	for(var i=0;i<tokens.length;i++){
		var token = tokens[i];
		token.length = getTokenLength(token[0]);
	}
	return tokens;
}

function addRightToken(rightStack,
		token) {
	var list = rightStack[rightStack.length-1];
//	if (token[0] == OP_GET) {
//	    var last = list.length-1;
//	    if(last>=0){
//	        var previous = list[last];
//	        if(previous[0] == VALUE_CONSTANTS){
//	            list.length--;
//	            token = [OP_GET_STATIC_PROP,previous[1]]; 
//	        }
//	    }
//	}
	list.push(token);
}

function getPriority(type) {
	switch (type) {
	case BRACKET_BEGIN:
	case BRACKET_END:
		return Math.MIN_VALUE;
	default:
		return (type & BIT_PRIORITY)<<4 | (type & BIT_PRIORITY_SUB)>>8;
	}
}
/**
 */
function rightEnd(currentType, priviousType) {
	var priviousPriority = getPriority(priviousType);
	var currentPriority = getPriority(currentType);
	return currentPriority <= priviousPriority;
}
if(typeof require == 'function'){
exports.getPriority=getPriority;
exports.ExpressionTokenizer=ExpressionTokenizer;
var hasTokenParam=require('./expression-token').hasTokenParam;
var getTokenParam=require('./expression-token').getTokenParam;
var hasTokenParam=require('./expression-token').hasTokenParam;
var getTokenParamIndex=require('./expression-token').getTokenParamIndex;
var getTokenLength=require('./expression-token').getTokenLength;
var findTokenType=require('./expression-token').findTokenType;
var BIT_PRIORITY=require('./expression-token').BIT_PRIORITY;
var BIT_PRIORITY_SUB=require('./expression-token').BIT_PRIORITY_SUB;
var OP_ADD=require('./expression-token').OP_ADD;
var OP_GET=require('./expression-token').OP_GET;
var OP_IN=require('./expression-token').OP_IN;
var OP_INVOKE=require('./expression-token').OP_INVOKE;
var OP_JOIN=require('./expression-token').OP_JOIN;
var OP_NE=require('./expression-token').OP_NE;
var OP_NEG=require('./expression-token').OP_NEG;
var OP_POS=require('./expression-token').OP_POS;
var OP_PUT=require('./expression-token').OP_PUT;
var OP_QUESTION=require('./expression-token').OP_QUESTION;
var OP_QUESTION_SELECT=require('./expression-token').OP_QUESTION_SELECT;
var OP_SUB=require('./expression-token').OP_SUB;
var VALUE_CONSTANTS=require('./expression-token').VALUE_CONSTANTS;
var VALUE_LIST=require('./expression-token').VALUE_LIST;
var VALUE_MAP=require('./expression-token').VALUE_MAP;
var VALUE_VAR=require('./expression-token').VALUE_VAR;
}