/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

/**
 * 将Lite的表达式结构转化为php表达式
 */
 
function PHPELTranslator(tokens){
	ELTranslator.call(this,tokens)
}
PHPELTranslator.prototype = new ELTranslator();
PHPELTranslator.prototype.stringifyValue = function(el){
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
};
/**
 * 翻译中缀运算符
 */
PHPELTranslator.prototype.stringifyInfix = function(el){
	var type = el[0];
	var opc = findTokenText(el[0]);
	var value1 = this.stringify(el[1]);
	var value2 = this.stringify(el[2]);
	if(this.getPriority(el[1])<this.getPriority(el)){
		value1 = '('+value1+')';
	}
	switch(type){
	case OP_ADD://+
	case OP_NOT://!
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
	case OP_PUSH:
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
    	var test = this.stringify(arg1[1]);    	var value1 = this.stringify(arg1[2]);
    	return '($__engine->op('+OP_NOT+','+test+')?'+value2+":"+value1+')';
    	
    case OP_AND://&&
    	if(value1.match(/^[\w_\$]+$/)){
    		return '($__engine->op('+OP_NOT+','+value1 +')?'+value1+':'+value2+')'
    	}
    	return '($__engine->op('+OP_NOT+',$__el='+value1 +')?$__el:'+value2+')'
    case OP_OR://||
    	if(value1.match(/^[\w_\$]+$/)){
    		return '($__engine->op('+OP_NOT+','+value1 +')?'+value2+':'+value1+')'
    	}
    	return '(!$__engine->op('+OP_NOT+',$__el'+value1 +')?$__el:'+value2+')'
	}
	if(this.getPriority(el)>=this.getPriority(el[2])){
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
