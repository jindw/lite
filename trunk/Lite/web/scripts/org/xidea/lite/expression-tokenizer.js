var STATUS_BEGIN = -100;
var STATUS_EXPRESSION = -101;
var STATUS_OPERATOR = -102;
var fns = {
	toTokens :function() {
		return this.expression;//.slice(0).reverse();// reversed
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
				    default:
    				    this.skipSpace(0);
    					if (this.previousType == OP_GET_PROP) {
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
					throw new Error("语法错误:" + this.value + "@"
							+ this.start);
				}
			}
			this.skipSpace(0);
		}
	},

	findOperator :function() {// optimize json ,:[{}]
		switch (this.value.charAt(this.start)) {
		case '!':// !,!=
		case '>':// >,>=
		case '<':// <,<=
			if (this.value.charAt(this.start + 1) == '=') {
				return this.value.substring(this.start, this.start += 2);
			}
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
		case '+':// 5op
		case '-':
		case '*':
		case '/':
		case '%':
			return this.value.substring(this.start, this.start += 1);

		case '=':// ==
		case '&':// &&
		case '|':// ||
    		if(":debug"){
    			if (this.value.charAt(this.start) != this.value.charAt(this.start + 1)){
    			    $log.error("Error OP:" + this.value.substring(this.start,this.start+2))
    			}
    		}
			return this.value.substring(this.start, this.start += 2);
		}
		alert(this.value.substring(this.start))
		return null;
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
				if (type == OP_MAP_PUSH
						|| type == VALUE_NEW_MAP) {// (
					// <#newMap>
					// <#push>
					return true;
				} else if (type == OP_PARAM_JOIN) {// (
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
					this.addToken([OP_INVOKE_METHOD, null]);
					if (this.skipSpace(')')) {
						this.addToken([VALUE_CONSTANTS,
								[]]);
						this.start++;
					} else {
						this.addList();
					}

				} else {
					this.addToken([BRACKET_BEGIN, null]);
				}
				break;
			case '[':
				if (this.status == STATUS_BEGIN || this.status == STATUS_OPERATOR) {// list
					this.addList();

				} else if (this.status == STATUS_EXPRESSION) {// getProperty
					this.addToken([OP_GET_PROP, null]);
					this.addToken([BRACKET_BEGIN, null]);
				} else {
					throw new Error("语法错误:" + this.value + "@"
							+ this.start);
				}
				break;
			case '{':
				this.addMap();
				break;
			case '}':
			case ']':
			case ')':
				this.addToken([BRACKET_END, null]);
				break;
			case '+'://
				this.addToken([
						this.status == STATUS_OPERATOR ? OP_POS
								: OP_ADD, null]);
				// this.addToken(OperatorToken.getToken(SKIP_AND));
				break;
			case '-':
				this.addToken([
						this.status == STATUS_OPERATOR ? OP_NEG
								: OP_SUB, null]);
				// this.addToken(OperatorToken.getToken(SKIP_AND));
				break;
			case '?':// ?:
				this.addToken([OP_QUESTION, null]);
				// this.addToken(OperatorToken.getToken(SKIP_QUESTION));
				this.addToken([VALUE_LAZY, null]);
				break;
			case ':':// :(object_setter is skiped)
				this.addToken([OP_QUESTION_SELECT, null]);
				this.addToken([VALUE_LAZY, null]);
				break;
			case ',':// :(object_setter is skiped,',' should
				// be skip)
				if (!this.isMapMethod()) {
					this.addToken([OP_PARAM_JOIN, null]);

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
				}
			default:
				this.addToken([findTokenType(op), null]);
			}
		} else if (op == "||") { // ||
			this.addToken([OP_OR, null]);
			this.addToken([VALUE_LAZY, null]);
			// this.addToken(LazyToken.LAZY_TOKEN_END);
		} else if (op == "&&") {// &&
			this.addToken([OP_AND, null]);
			this.addToken([VALUE_LAZY, null]);
			// this.addToken(OperatorToken.getToken(SKIP_AND));
		} else {
			this.addToken([findTokenType(op), null]);
		}

	},

	addToken :function(token) {
		switch (token[0]) {
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
		this.previousType = token[0];
		this.tokens.push(token);
	},

	addKeyOrObject :function(object, isVar) {
		if (this.skipSpace(':') && this.isMapMethod()) {// object key
			this.addToken([OP_MAP_PUSH, object]);
			this.start++;// skip :
		} else if (isVar) {
			this.addToken([VALUE_VAR, object]);
		} else {
			this.addToken([VALUE_CONSTANTS, object]);
		}
	},

	addList :function() {
		this.addToken([BRACKET_BEGIN, null]);
		this.addToken([VALUE_NEW_LIST, null]);
		if (!this.skipSpace(']')) {
			this.addToken([OP_PARAM_JOIN, null]);
		}
	},

	addMap :function() {
		this.addToken([BRACKET_BEGIN, null]);
		this.addToken([VALUE_NEW_MAP, null]);
	}
};
var pt = new JSONTokenizer('');
for(var n in fns){
    pt[n] = fns[n]
}
function ExpressionTokenizer(value){
    this.value = value.replace(/^\s+|\s+$/g,'');
	this.start = 0;
	this.end = this.value.length;
    this.status = STATUS_BEGIN;
	this.previousType = STATUS_BEGIN;
	this.tokens = [];
	this.parseEL();
	this.expression = trimToken(right(this.tokens));
	
}
ExpressionTokenizer.prototype = pt;



// 将中序表达式转换为右序表达式
function right(tokens) {
	var rightStack = [[]];
	var buffer = [];

	for (var i = 0;i<tokens.length;i++) {
		var item = tokens[i];
		if (item[0] > 3) {
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
					addRightOperator(rightStack, operator);
				}
			} else {
				while (buffer.length!=0
						&& rightEnd(item[0], buffer[buffer.length-1][0])) {
					var operator = buffer.pop();
					// if (operator[0] !=
					// BRACKET_BEGIN){
					addRightOperator(rightStack, operator);
				}
				buffer.push(item);
			}
		} else {// lazy begin value exp
			addRightToken(rightStack, item);
		}
	}
	while (buffer.length !=0) {
		var operator = buffer.pop();
		addRightOperator(rightStack, operator);
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
function addRightOperator(rightStack,
		operator) {
	switch (operator[0]) {
	case OP_OR:
	case OP_AND:
	case OP_QUESTION:
	case OP_QUESTION_SELECT:
		var children = rightStack.pop();
		var list = rightStack[rightStack.length-1];
		if (children.length == 1) {
			list[list.length - 1]= children[0];
		} else {
			var token = list[list.length - 1];
			token[1]=trimToken(children);//.slice(0).reverse();;

		}
	}
	addRightToken(rightStack, operator);
}

function addRightToken(rightStack,
		token) {
	var list = rightStack[rightStack.length-1];
	if (token[0] == OP_GET_PROP) {
	    var last = list.length-1;
	    if(last>=0){
	        var previous = list[last];
	        if(previous[0] == VALUE_CONSTANTS){
	            list.length--;
	            token = [OP_STATIC_GET_PROP,previous[1]]; 
	        }
	    }
	}else if (token[0] == VALUE_LAZY) {
		rightStack.push([]);
	}
	token.toString = toTokenString;
	list.push(token);
}

function getPriority(type) {
	switch (type) {
	case BRACKET_BEGIN:
	case BRACKET_END:
		return Math.MIN_VALUE;
	case OP_GET_PROP:
	//case OP_GET_METHOD:
	//case OP_GET_GLOBAL_METHOD:
	case OP_INVOKE_METHOD:
	case VALUE_NEW_LIST:
	case VALUE_NEW_MAP:
		return 12;

	case OP_NOT:
	case OP_POS:
	case OP_NEG:
		return 8;

	case OP_MUL:
	case OP_DIV:
	case OP_MOD:
		return 4;

	case OP_ADD:
	case OP_SUB:
		return 1;

	case OP_LT:
	case OP_GT:
	case OP_LTEQ:
	case OP_GTEQ:
	case OP_EQ:
	case OP_NOTEQ:
		return 0;

	case OP_AND:
		return -1;
	case OP_OR:
		return -2;

	case OP_QUESTION:
	case OP_QUESTION_SELECT:
		return -4;// !!

	case OP_MAP_PUSH:
		return -7;// !!
	case OP_PARAM_JOIN:
		return -8;
	}
	throw new Error("unsupport token:" + type);
}

function rightEnd(itemType, priviousType) {
	return getPriority(itemType) <= getPriority( priviousType);
}