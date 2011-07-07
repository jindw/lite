/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var FOR_STATUS_KEY = '$__for';

var ID_PATTERN = /^[a-zA-Z_\$][_\$\w]*$/;
var NUMBER_CALL = /^(\d+)(\.\w+)$/;


/**
 * 将某一个token转化为表达式
 */
function stringifyJSEL(el,context){
	var type = el[0];
	if(type<=0){//value
		return stringifyValue(el,context)
	}else if(getTokenParamIndex(type) ==3){//两个操作数
		return stringifyInfix(el,context);
	}else{
		return stringifyPrefix(el,context);
	}
	
}
/**
 * 翻译常量字面量
 */
function stringifyValue(el,context){
	var param = el[1];
	switch(el[0]){
    case VALUE_CONSTANTS:
        return (param && param['class']=='RegExp' && param.source) || stringifyJSON(param);
    case VALUE_VAR:
    	if(param == 'for'){
    		var f = context && context.getForName();
    		if(f){
    			return f;
    		}
    	}
    	return param;
    case VALUE_LIST:
    	return "[]";
    case VALUE_MAP:
    	return "{}";
	}
}
/**
 * 翻译中缀运算符
 */
function stringifyInfix(el,context){
	var type = el[0];
	var opc = findTokenText(el[0]);
	var value1 = stringifyJSEL(el[1],context);
	var value2 = stringifyJSEL(el[2],context);
	//value1 = addELQute(el,el[1],value1);
	switch(type){
	case OP_INVOKE:
		value2 = value2.slice(1,-1);
		value1 = value1.replace(NUMBER_CALL,'($1)$2')
		return value1+"("+value2+')';
	case OP_GET:
		//value1 = toOperatable(el[1][0],value1);
		value1 = addELQute(el,el[1],value1)
		if(el[2][0] == VALUE_CONSTANTS){
			var p = getTokenParam(el[2])
			if(typeof p == 'string'){
				if(context && (p == 'index' || p == 'lastIndex')){
					var forAttr = context.getForAttribute(value1,p);
					if(forAttr){
						return forAttr;
					}
				}
				if(ID_PATTERN.test(p)){
					return value1+'.'+p;
				}
			}
		}
		return value1+'['+value2+']';
	case OP_JOIN:
		if("[]"==value1){
			return "["+value2+"]"
		}else{
			return value1.slice(0,-1)+','+value2+"]"
		}
	case OP_PUT:
		value2 = stringifyJSON(getTokenParam(el))+":"+value2+"}";
		if("{}"==value1){
			return "{"+value2
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
     	//?:已经是最低优先级了,无需qute,而且javascript 递归?: 也无需优先级控制
     	var el1 = el[1];
    	var test = stringifyJSEL(el1[1],context);
    	var value1 = stringifyJSEL(el1[2],context);
    	return test+'?'+value1+":"+value2;
	}
	value1 = addELQute(el,el[1],value1)
	value2 = addELQute(el,el[2],null,value2)
	return value1 + opc + value2;
}
/**
 * 翻译前缀运算符
 */
function stringifyPrefix(el,context){
	var type = el[0];
	var el1 = el[1];
	var value = stringifyJSEL(el1,context);
	var param = getTokenParam(el);
	value = addELQute(el,el1,null,value)
    var opc = findTokenText(type);
	return opc+value;
}
