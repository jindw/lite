/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var VAR_LITE_EL_TEMP = "$__el_tmp"
var FOR_STATUS_KEY = '$__for';
/**
 * 将Lite的表达式结构转化为php表达式
 */
function stringifyPHPEL(el,context){
	var type = el[0];
	if(type<=0){//value
		return stringifyValue(el,context)
	}else if(getTokenParamIndex(type) ==3){//两个操作数
		return stringifyInfix(el,context);
	}else{
		return stringifyPrefix(el,context);
	}
}
function stringifyValue(el,context){
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

function typesOnly(t1,t2){
	var i = arguments.length;
	var a = 0;
	while(--i>1){
		a |= arguments[i];
	}
	var t = t1 | t2;
	return (t & a) == t;
}
function stringifyADD(el,context){
	var t = getELType(el);
	var value1 = stringifyPHPEL(el[1],context);
	var value2 = stringifyPHPEL(el[2],context);
	if(t == TYPE_NUMBER){
		return value1+'+'+value2;//动态部分要考虑 array的问题,这里不需要考虑
	}else if(t == TYPE_STRING){
		if(/[\d]$/.test(value1)){
			value1+=' ';
		}
		if(/^[\d]/.test(value2)){
			value2=' '+value2;
		}
		//还需要处理 null,true,false 字面量的问题
		
		var t1 = getELType(el[1]);
		var t2 = getELType(el[2]);
		//console.error(t1,t2)
		if(typesOnly(t1,t2,TYPE_STRING,TYPE_NUMBER)){
			return value1+'.'+value2;
		}
	}
	//字符串加法不复合交换律
//	if(typesOnly(t1,t1,TYPE_NULL,TYPE_NUMBER,TYPE_BOOLEAN)){
//		return "lite_op__add_nx("+value1+','+value2+")"
//	}
//	if(typesOnly(t2,t2,TYPE_NULL,TYPE_NUMBER,TYPE_BOOLEAN)){
//		return "lite_op__add_nx("+value2+','+value1+")"
//	}
	return "lite_op__add("+value1+','+value2+")"
}

//var TYPE_NULL = 1<<offset++;

//var TYPE_BOOLEAN = 1<<offset++;
//var TYPE_NUMBER = 1<<offset++;

//var TYPE_STRING = 1<<offset++;
//var TYPE_ARRAY = 1<<offset++;
//var TYPE_MAP = 1<<offset++;
function stringifyEQ(el,context,opc){
	var t1 = getELType(el[1]);
	var t2 = getELType(el[2]);
	var value1 = stringifyPHPEL(el[1],context);
	var value2 = stringifyPHPEL(el[2],context);
	opc = opc || '==';
	if(t1 ==  TYPE_STRING ||  t2 == TYPE_STRING){//0 == 'ttt'=>false
        return "strcmp("+value1+","+value2+") "+opc+"0";
    }
    
    if(t1 === TYPE_NULL || t2 === TYPE_NULL){
    	return value1+opc+'='+value2;
    }
    if(typesOnly(t1,t2,TYPE_NUMBER,TYPE_BOOLEAN)
//    		||typesOnly(t1,t2,TYPE_BOOLEAN,TYPE_STRING)//'0' ==false;
//    		||typesOnly(t1,t2,TYPE_NUMBER,TYPE_STRING)//'0' == 0
//    		||typesOnly(t1,t2,TYPE_NUMBER,TYPE_BOOLEAN)//'0' ==false;"
    		||typesOnly(t1,t2,TYPE_ARRAY,TYPE_MAP,TYPE_STRING)//'' ==array() => false,忽略array.toString ==//php ;
    		||t1.toString(2).replace(/0/g,'').length==1 && t1 == t2){
        return value1+opc+value2;
    }
    return (opc=='!='?'!':'')+"lite_op__eq("+value1+','+value2+")"
}
var math = {
	"E":2.718281828459045,
	"PI":3.141592653589793,
	"LN2":0.6931471805599453,
	"LN10":2.302585092994046,
	"LOG2E":1.4426950408889634,
	"LOG10E":0.4342944819032518,
	"SQRT1_2":0.7071067811865476,
	"SQRT2":1.4142135623730951
}
function stringifyGET(el,context){
	var arg1 = el[1];
	var arg2 = el[2];
	var value1 = stringifyPHPEL(el[1],context);
	var value2 = stringifyPHPEL(el[2],context);
	if(arg2[0] == VALUE_CONSTANTS){
		var prop = arg2[1];
		if( prop != 'length'){
			//这里有可能要抛警告
			if(arg1[0] == VALUE_VAR){
				var owner = arg1[1];
				if(owner == 'Math' && !(owner in context.scope.defMap && owner in context.scope.varMap && owner in context.scope.paramMap)){
					if(typeof math[prop] == 'number'){
						return '('+math[prop]+')';
					}
				}
			}
			if(!/^[^(][\s\S]*\)$/.test(value1) && !/^(true|false|null|[\d\.]+)$/.test(value1)){//php bug method(args)[index]非法
				return value1+'['+value2+']';
			}
			
		}
	}
	return "lite_op__get("+value1+','+value2+")"
}
/**
 * return [owner,prop,args]
 */
function parseInvoke(el){
	var method = el[1];
	if(method[0] == OP_GET){//member_call
		var ownerEL = method[1];
		var propEL = method[2];
		if(ownerEL[0] == VALUE_VAR){
			var varName = ownerEL[1];
		}
		if(propEL[0] == VALUE_CONSTANTS){
			var prop = propEL[1];
		}
		return [varName||ownerEL,prop||propEL,el[2]];
	}else{//function_call
		if(method[0] == VALUE_VAR){
			var varName = method[1];
		}
		return [varName||method,null,el[2]]
	}
}
function stringifyPHPEL2ID(el,context,id){
	if(typeof el != 'string'){
		return stringifyPHPEL(el,context)
	}else if(id){
		return '$'+el;
	}
	return "'"+el+"'";
}
function stringifyINVOKE(el,context){
	var info = parseInvoke(el);
	var owner = info[0];
	var prop = info[1];
	var args = stringifyPHPEL(info[2],context);
	if(prop){//member_call
		if(typeof prop == 'string'){
//			random=>rand(0, PHP_INT_MAX
//			sin,sqrt,tan,cos,acos,asin,atan,atan2 
//			max,min,,floor,round,abs,ceil,exp,log,pow,
			if(owner === 'Math'){
				var mp = /^(?:sin|sqrt|tan|cos|acos|asin|atan|atan2|max|min||floor|round|abs|ceil|exp|log|pow)$/;
				if(prop == 'random'){
					return '(rand(0, 0xFFFF)/0xFFFF)';
				}else if(mp.test(prop)){
					return args.replace('array',prop);
				}else{
					console.warn("Math 不支持方法:"+prop+";Math 支持的方法有:random|"+mp.source.replace(/[^\w\|]/g,''))
				}
			}else if(owner === 'JSON'){
				if(prop == "parse"){
					return args.replace('array','json_decode').slice(0,-1)+',true)';
				}else if(prop =='stringify'){
					return args.replace('array','json_encode');
				}else{
					console.warn("JSON 不支持方法:"+prop+";JSON 只支持:stringify和parse方法")
				}
			}else if(prop == 'reverse' && args == 'array()' && owner[0] == OP_INVOKE){
				var info2 = parseInvoke(owner);
				//console.error(info2);
				if(info2[1] == 'concat'){
					owner = info2[0];
					owner = stringifyPHPEL2ID(owner,context,true)
					args = stringifyPHPEL(info2[2],context);
					return "lite_op__invoke("+owner+",'concat_reverse',"+args+")"
				}
			}
		}
		owner = stringifyPHPEL2ID(owner,context,true)
		prop = stringifyPHPEL2ID(prop,context)
		return "lite_op__invoke("+owner+","+prop+","+args+")"
		//value1 = value1.replace(/.*?,([\s\S]+)\)/,'array($1)');
	}else if(typeof owner == 'string'){
		if((owner in GLOBAL_DEF_MAP || owner in context.scope.defMap)
			&& !(owner in context.scope.varMap || owner in context.scope.paramMap)){
			//静态编译方式
			return args.replace('array',"lite__"+owner)
		}else{
			//动态调用方式
			//console.error("!!!!!!!!!!!!",context.scope.varMap);
			if(owner in context.scope.varMap || owner in context.scope.paramMap){
				var fn = '$'+owner;
			}else{
				var fn = "isset($"+owner+")?$"+owner+":'"+owner+"'";
			}
			return 'lite_op__invoke('+fn+',null,'+args+')';
		}
	}else{
		//console.error("??????????",typeof owner,owner,context.scope.varMap);
		owner = stringifyPHPEL2ID(owner,context,true)
		//console.error(owner);
		return 'lite_op__invoke('+owner+',null,'+args+')';
		//throw new Error("Invalid Invoke EL");
	}
}
/**
 * 翻译中缀运算符
 */
function stringifyInfix(el,context){
	var type = el[0];
	if(type == OP_ADD){
		return stringifyADD(el,context)
	}else if(type == OP_EQ){
		return stringifyEQ(el,context,'==')
	}else if(type == OP_NE){
		return stringifyEQ(el,context,'!=');
	}else if(type == OP_GET){
		return stringifyGET(el,context);
	}else if(type == OP_INVOKE){
		return stringifyINVOKE(el,context);
	}
	var opc = findTokenText(el[0]);
	var value1 = stringifyPHPEL(el[1],context);
	var value2 = stringifyPHPEL(el[2],context);
	switch(type){
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
    	var test = stringifyPHPEL(arg1[1],context);    	var value1 = stringifyPHPEL(arg1[2],context);
    	//return '('+LITE_INVOKE+OP_NOT+','+test+')?'+value2+":"+value1+')';
    	return '('+php2jsBoolean(arg1[1],test)+'?'+value1+':'+value2+')'
    case OP_AND://&&
    	if(isSimplePHPEL(value1)){
    		return '('+php2jsBoolean(el[1],value1)+'?'+value2+':'+value1+')'
    	}
    	return '(('+php2jsBoolean(el[1],value1,VAR_LITE_EL_TEMP)+')?'+value2+':'+VAR_LITE_EL_TEMP+')'
    case OP_OR://||
    	if(isSimplePHPEL(value1)){
    		return '('+php2jsBoolean(el[1],value1)+'?'+value1+':'+value2+')'
    	}
    	return '(('+php2jsBoolean(el[1],value1,VAR_LITE_EL_TEMP)+')?'+VAR_LITE_EL_TEMP+':'+value2 +')'
	}
	value1 = addELQute(el,el[1],value1)
	value2 = addELQute(el,el[2],null,value2)
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
            return ''+value;
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
function stringifyPrefix(el,context){
	var type = el[0];
	var el1 = el[1];
	var value2 = stringifyPHPEL(el1,context);
	var param = getTokenParam(el,context);
	if(type == OP_NOT){//!
		//return value1+'['+value2+']';
		var rtv = php2jsBoolean(el1,value2);
		if(!isSimplePHPEL(rtv)){
			rtv = '('+rtv+')';
		}
		return '!'+rtv;
	}
	value2 = addELQute(el,el[1],null,value2)
    var opc = findTokenText(type);
	return opc+value2;
}

/**
 * 如果不是变量或者常量，则必须设置零时变量
 */
function php2jsBoolean(el,value,keepValue,context){
	if(!value){
		value = stringifyPHPEL(el,context);
	}
	var op = el[0];
	if(op<=0){
		switch(op){
		case VALUE_CONSTANTS:
			
			if(keepValue){
				if(el[1]){
					return '(('+keepValue+'='+value+')||true)';
				}else{
					return '(('+keepValue+'='+value+')&&false)';
				}
//			}else if(booleanVar){//keepValue 和 booleanVar 只选一个
//				if(el[1]){
//					return '('+booleanVar+'=true)';
//				}else{
//					return '('+booleanVar+'=false)';
//				}
			}else{
				return !!el[1]+'';
			}
			
		case VALUE_VAR:
			break;
		case VALUE_LIST:
		case VALUE_MAP:
		default:
			if(keepValue){
				return '(('+keepValue+'='+value+')||true)'
			}else{
				return 'true';
			}
		}
	}
//var TYPE_NULL = 1<<offset++;
//var TYPE_BOOLEAN = 1<<offset++;
//var TYPE_NUMBER = 1<<offset++;

//var TYPE_STRING = 1<<offset++;
//var TYPE_ARRAY = 1<<offset++;
//var TYPE_MAP = 1<<offset++;
//var TYPE_ANY = (1<<offset++) -1;
	var type = getELType(el);
	
	if(!((type & TYPE_STRING)||(type & TYPE_ARRAY)||(type & TYPE_MAP))){
		if(!keepValue){//持续优化
			return value;
		}else{
			return '('+keepValue +'='+value+')';
		}
	}
	if(isSimplePHPEL(value) && !keepValue){
		var rtv = value;
		keepValue = value;
	}else{
		keepValue = keepValue || VAR_LITE_EL_TEMP;
		var rtv = "("+keepValue +"="+ value+")"
	}
	if((type & TYPE_ARRAY)||(type & TYPE_MAP)){
		rtv+=' || 0 < '+keepValue;
	}
	if((type & TYPE_STRING)){
		rtv+=" || '0' ==="+keepValue;
	}
	return '('+rtv+')'
}
function isSimplePHPEL(value){
	return value.match(/^([\w_\$]+|[\d\.]+)$/)
}
/**
 * 获取某个运算符号的优先级
 */
function getELPriority(el) {
	return getPriority(el[0]);
}
if(typeof require == 'function'){
exports.stringifyPHPEL=stringifyPHPEL;
exports.stringifyPHP=stringifyPHP;
exports.php2jsBoolean=php2jsBoolean;
exports.isSimplePHPEL=isSimplePHPEL;
var getTokenParam=require('js-el/expression-token').getTokenParam;
var getTokenParamIndex=require('js-el/expression-token').getTokenParamIndex;
var findTokenText=require('js-el/expression-token').findTokenText;
var getELType=require('js-el/expression-token').getELType;
var addELQute=require('js-el/expression-token').addELQute;
var OP_ADD=require('js-el/expression-token').OP_ADD;
var OP_AND=require('js-el/expression-token').OP_AND;
var OP_EQ=require('js-el/expression-token').OP_EQ;
var OP_GET=require('js-el/expression-token').OP_GET;
var OP_IN=require('js-el/expression-token').OP_IN;
var OP_INVOKE=require('js-el/expression-token').OP_INVOKE;
var OP_JOIN=require('js-el/expression-token').OP_JOIN;
var OP_NE=require('js-el/expression-token').OP_NE;
var OP_NOT=require('js-el/expression-token').OP_NOT;
var OP_OR=require('js-el/expression-token').OP_OR;
var OP_PUT=require('js-el/expression-token').OP_PUT;
var OP_QUESTION=require('js-el/expression-token').OP_QUESTION;
var OP_QUESTION_SELECT=require('js-el/expression-token').OP_QUESTION_SELECT;
var TYPE_ANY=require('js-el/expression-token').TYPE_ANY;
var TYPE_ARRAY=require('js-el/expression-token').TYPE_ARRAY;
var TYPE_BOOLEAN=require('js-el/expression-token').TYPE_BOOLEAN;
var TYPE_MAP=require('js-el/expression-token').TYPE_MAP;
var TYPE_NULL=require('js-el/expression-token').TYPE_NULL;
var TYPE_NUMBER=require('js-el/expression-token').TYPE_NUMBER;
var TYPE_STRING=require('js-el/expression-token').TYPE_STRING;
var VALUE_CONSTANTS=require('js-el/expression-token').VALUE_CONSTANTS;
var VALUE_LIST=require('js-el/expression-token').VALUE_LIST;
var VALUE_MAP=require('js-el/expression-token').VALUE_MAP;
var VALUE_VAR=require('js-el/expression-token').VALUE_VAR;
var getPriority=require('js-el/expression-tokenizer').getPriority;
var GLOBAL_DEF_MAP=require('../parse/js-translator').GLOBAL_DEF_MAP;
}