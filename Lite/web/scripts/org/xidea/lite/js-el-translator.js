/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

/**
 * 将Lite的逆波兰式序列转化为php表达式
 */
function ELTranslator(tokens){
	if(tokens instanceof ELTranslator){
		return tokens;
	}
	this.tree = toTree(tokens);
	this.varMap = {};
	walkTree(this,this.tree)
}

ELTranslator.prototype = {
	/**
	 * 将TOKEN转化为EL 表达式
	 */
	toString:function(){
		return this.stringify(this.tree);
	},
	/**
	 * 获取某个运算符号的优先级
	 */
	getPriority:function(el) {
		return type & 30;
	},
	/**
	 * 将某一个token转化为表达式
	 */
	stringify:function(el){
		var op = el[0];
		var arg1 = el[1];
		if(op<=0){//value
			switch(op){
            case VALUE_CONSTANTS:
	            return stringifyJSON(arg1);
            case VALUE_VAR:
            	if(arg1 == 'for'){
            		return "__context__";
            	}else{
            		return arg1;
            	}
            case VALUE_NEW_LIST:
            	return "[]";
            case VALUE_NEW_MAP:
            	return "{}";
			}
		}else if(op[0] & 1){//两个操作数
			var arg2 = el[2];
			return this.toBothSide(op,arg1,arg2);
		}else{
			return this.toPrefix(op,arg1);
		}
		
	},
	/**
	 * 翻译中缀运算符
	 */
	stringifyInfix:function(op){
		var type = op[0];
		var opc = findTokenText(op[0]);
		var value1 = this.stringify(arg1);
		var value2 = this.stringify(arg2);
		switch(type){
		case OP_INVOKE_METHOD:
			if(arg1[0] == VALUE_VAR){//globals
				if(value2 instanceof Array){
					value2 = "("+value2.join(',')+")";
				}else{
					value2 = value2.substring("array".length)
				}
				return "lite__"+arg1[1]+value2;
			}else if(arg1[0][0] == OP_STATIC_GET_PROP){//members
				var memberName = arg1[0][1];
				var value1 = this.stringify(arg1[1]);
				return "lite_member_"+memberName+"("+value1+","+value2.substring("array(".length);
			}else{
				throw Error("只能支持全局函数调用和静态属性函数调用");
			}
		case OP_PARAM_JOIN:
			if(/\(\)$/.test(value1)){
				return value1.slice(0,-1)+value2+")"
			}else{
				return value1.slice(0,-1)+','+value2+")"
			}
			//return value1.replace(/(,?)\)$/,'$1')+value2+")"
		case OP_MAP_PUSH:
			value2 = unevalPHPString(op[1])+"=>"+value2+")";
			if(/\(\)$/.test(value1)){
				return value1.slice(0,-1)+value2
			}else{
				return value1.slice(0,-1)+','+value2
			}
			//return value1.replace(/(,?)\)$/,'$1')+unevalPHPString(op[1])+"=>"+value2+")"
        case OP_GET_PROP:
            return "lite_op_get_property("+value1+","+value2+")";
        //case OP_QUESTION:
        //   return "lite_op_and("+value1+","+value2+")";
        case OP_QUESTION_SELECT:
        /**
     ${a?b:c}
     ${a?b1?b2:b3:c}
     ${222+2|a?b1?b2:b3:c}
         */
        	var test = this.stringify(arg1[1]);        	var value1 = this.stringify(arg1[2]);
        	//alert(uneval([op,arg1,arg2]))
        	switch(findELType(arg1[1])){
        	case 'boolean':
        	case 'number':
        	break;
        	default:
	        	if(/^[\w+\$\.'"\[\]]$/.test(test)){
	        		test = test +"=== '0' || "+test;
	        	}else{
	        		//TODO:可以展开
	        		test = "lite_op_and("+test+",true)"
	        	}
        	}
        	return test+'?'+value1+":"+value2;
            //return "lite_op_select("+test+","+value1+","+value2+")";
        case OP_AND:
            return "lite_op_and("+value1+","+value2+")";
        case OP_OR:
            return "lite_op_or("+value1+","+value2+")";
		case OP_ADD:
			if(isNumberAdder(arg1)&&isNumberAdder(arg2)){
				return value1+"+"+value2;
			}else if(isStringAdder(arg1) && isStringAdder(arg2)){
				return value1+"."+value2;
			}
            return "lite_op_add("+value1+","+value2+")";
		}
		if(this.getPriority(arg1)<this.getPriority(op)){
			value1 = '('+value1+')';
		}
		if(this.getPriority(op)>=this.getPriority(arg2)){
			value2 = '('+value2+')';
		}
		return value1 + opc + value2;
	},
	/**
	 * 翻译前缀运算符
	 */
	stringifyPrefix:function(op){
		var type = op[0];
		var el1 = op[1];
		var opc = findTokenText(type);
		var value = this.stringify(el1);
		if(OP_STATIC_GET_PROP == type) {
			var key = op[3];
			if(typeof key == 'number'){
				return value+'['+key+']';
			}else{
				if(/^[a-zA-Z_\$]\w*$/.test(key)){
					return value+'.'+key;
				}
				return value+'['+stringifyJSON(''+key)+']';
			}
		}
		if(this.getPriority(op)>=this.getPriority(el1)){
			value = '('+value+')';
		}
		return opc+value;
	}
}


function walkTree(thiz,el){
	var op = el[0];
	if(op<=0){
		if(op == VALUE_VAR){
			var varName = el[1];
			thiz.varMap[varName] = true;
			if(varName == "for"){
				thiz.forRef = true;
			}
		}
		return;
	}else{
		var arg1 = el[1];
		var arg2 = el[2];
		if(op[0] == OP_STATIC_GET_PROP){
			if(arg1[0] == VALUE_VAR && arg1[1] == 'for'){
				if(op[1] == 'index'){
					thiz.forIndex = true;
				}else if(op[1] == 'lastIndex'){
					thiz.forLastIndex = true;
				}else{
					throw new Error("for不支持属性:"+op[1]);
				}
				return ;
			}
		}
		arg1 && walkTree(thiz,arg1);
		arg2 && walkTree(thiz,arg2);
	}
}
