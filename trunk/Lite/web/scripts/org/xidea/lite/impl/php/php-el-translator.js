/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var VAR_TEMP = "$__lite_tmp"
/**
 * 将Lite的表达式结构转化为php表达式
 */
function stringifyPHPEL(el){
	var type = el[0];
	if(type<=0){//value
		return stringifyValue(el)
	}else if(getTokenParamIndex(type) ==3){//两个操作数
		return stringifyInfix(el);
	}else{
		return stringifyPrefix(el);
	}
}
function stringifyValue(el){
		var param = el[1];
		switch(el[0]){
        case VALUE_CONSTANTS:
            return stringifyPHP(param);
        case VALUE_VAR:
        	if(param == 'for'){
        		return FOR_STATUS_KEY;
        	}else{
        		return '$'+param;
        	}
        case VALUE_LIST:
        case VALUE_MAP:
        	return "array()";
		}
}
/**
 * 翻译中缀运算符
 */
function stringifyInfix(el){
	var type = el[0];
	var opc = findTokenText(el[0]);
	var value1 = stringifyPHPEL(el[1]);
	var value2 = stringifyPHPEL(el[2]);
	if(getELPriority(el[1])<getELPriority(el)){
		value1 = '('+value1+')';
	}
	switch(type){
	case OP_ADD://+
	//case OP_NOT://! infix
    case OP_EQ://==
    case OP_NOTEQ://!=
	case OP_GET://.
		//return value1+'['+value2+']';
		return "$__engine->op("+type+","+value1+","+value2+")";
	case OP_INVOKE:
		var arg1 = el[1];
		var type1 = arg1[0];
		if(type1 == OP_GET){
			value1 = value1.replace(/.*?,([\s\S]+)\)/,'array($1)');
		}else if(type1 == VALUE_VAR){
			value2 = value2.replace('array(','($__engine,');
			return value1+value2;
		}else{
			throw new Error("Invalid Invoke EL");
		}
		return "$__engine->op("+type+","+value1+","+value2+")";
	case OP_JOIN:
		if("array()"==value1){
			return "array("+value2+")"
		}else{
			return value1.slice(0,-1)+','+value2+")"
		}
	case OP_PUT:
		value2 = stringifyPHP(getTokenParam(el))+"=>"+value2+")";
		if("array()"==value1){
			return "array("+value2
		}else{
			return value1.slice(0,-1)+','+value2
		}
    case OP_QUESTION:
    	//1?2:3 => [QUESTION_SELECT,
    	// 					[QUESTION,[CONSTANTS,1],[CONSTANTS,2]],
    	// 					[CONSTANTS,3]
    	// 			]
    	//throw new Error("表达式异常：QUESTION 指令翻译中应该被QUESTION_SELECT跳过");
    	return null;//前面有一个尝试，此处应返回null，而不是抛出异常。
    case OP_QUESTION_SELECT:
    /**
 ${a?b:c}
 ${a?b1?b2:b3:c}
 ${222+2|a?b1?b2:b3:c}
     */
     	var arg1 = el[1];
    	var test = stringifyPHPEL(arg1[1]);    	var value1 = stringifyPHPEL(arg1[2]);
    	//return '($__engine->op('+OP_NOT+','+test+')?'+value2+":"+value1+')';
    	return '('+toBoolean(test,arg1[1])+'?'+value1+':'+value2+')'
    case OP_AND://&&
    	if(value1.match(/^[\w_\$]+$/)){
    		return '('+toBoolean(value1,el[1])+'?'+value2+':'+value1+')'
    	}
    	return '('+toBoolean(value1,el[1],true)+'?'+value2+':'+VAR_TEMP+')'
    case OP_OR://||
    	if(value1.match(/^[\w_\$]+$/)){
    		return '('+toBoolean(value1,el[1])+'?'+value1+':'+value2+')'
    	}
    	return '('+toBoolean(value1,el[1],true)+'?'+VAR_TEMP+':'+value2 +')'
	}
	if(getELPriority(el)>=getELPriority(el[2])){
		value2 = '('+value2+')';
	}
	return value1 + opc + value2;
}


function stringifyPHP(value) {
    switch (typeof value) {
        case 'string':
	    	return '\'' + value.replace(/[\\']/g,"\\$&")+ '\'';
        case 'number':
            if(isNaN(value)){
                value = 'null';
            }
        case 'undefined':
        	return 'null';
        case 'object':
            if (!value) {
                return 'null';
            }
            var buf = [];
            if (value instanceof Array) {
                var i = value.length;
                while (i--) {
                    buf[i] = stringifyPHP(value[i]) || 'null';
                }
                return 'array(' + buf.join(',') + ')';
            }else if(value instanceof RegExp){
            	return "array('class'=>'RegExp','source'=>'"+value.replace(/[\\']/g,"\\$&")+"')";
            }
            for (var k in value) {
                var v = stringifyPHP(value[k]);
                if (v) {
                    buf.push(stringifyPHP(k) + '=>' + v);
                }
            }
            return 'array(' + buf.join(',') + ')';
        default://boolean
            return String(value);
    }
}

/**
 * 翻译前缀运算符
 */
function stringifyPrefix(el){
	var type = el[0];
	var el1 = el[1];
	var value = stringifyJSEL(el1);
	var param = getTokenParam(el);
	if(type == OP_NOT){//!
		//return value1+'['+value2+']';
		return "$__engine->op("+type+","+value+")";
	}
	if(getELPriority(el)>=getELPriority(el1)){
		value = '('+value+')';
	}
    var opc = findTokenText(type);
	return opc+value;
}
function getType(el){
	var op = el[0];
	var type;
	if(op<=0){
		switch(op){
		case VALUE_CONSTANTS:
			return typeof el[1];
		case VALUE_VAR:
			return null;
		case VALUE_LIST:
		case VALUE_MAP:
		default:
			return 'object';
		}
	}else{
		var arg1 = el[1];
		var arg2 = el[2];
		switch(op[0]){
		case OP_ADD:
			//if(isNumberAdder(arg1)&&isNumberAdder(arg2)){
			//	//return 'number';
			//}else{
			return 'string,number';
			//}
		case OP_POS:
		case OP_NEG:
		case OP_MUL:
		case OP_DIV:
		case OP_MOD:
		case OP_SUB:
			return 'number';
		case OP_NOT:
		case OP_LT:
		case OP_GT:
		case OP_LTEQ:
		case OP_GTEQ:
		case OP_EQ:
		case OP_NE:
		case OP_EQ_STRICT:
		case OP_NE_STRICT:
//		case OP_AND:
//		case OP_OR:
			return 'boolean';
//		case OP_GET:
//			if(arg1[0] == VALUE_VAR && arg1[1] == 'for'){
//				if(op[1] == 'index' || op[1] == 'lastIndex'){
//					return 'number';
//				}
//			}
		}
	}
}
/**
 * 如果不是变量或者常量，则必须设置零时变量
 */
function toBoolean(value,el,setTempVar){
	var op = el[0];
	if(op<=0){
		switch(op){
		case VALUE_CONSTANTS:
			if(setTempVar){
				if(el[1]){
					return '(('+VAR_TEMP+'='+value+')||true)';
				}else{
					return '(('+VAR_TEMP+'='+value+')&&false)';
				}
			}else{
				return !!el[1]+'';
			}
			
		//case VALUE_VAR:
		case VALUE_LIST:
		case VALUE_MAP:
		default:
			if(setTempVar){
				return '(('+VAR_TEMP+'='+value+')||true)'
			}
			return 'true';
		}
	}
	var type = getType();
	if(!setTempVar){//持续优化
		if(type == 'boolean' || type =='number'){
			return value;
		}
		if(value.match(/^[\w_\$]+$/)){
			return '('+value+' || '+value+">0 || '0' === "+value+')'
		}
	}
	if(type == 'boolean' || type =='number'){
		return '('+VAR_TEMP +'='+value+')';
	}
	return "(("+VAR_TEMP +"="+ value+") || "+VAR_TEMP+">0 || "+VAR_TEMP+" === '0')";
}
/**
 * 获取某个运算符号的优先级
 */
function getELPriority(el) {
	return getPriority(el[0]);
}