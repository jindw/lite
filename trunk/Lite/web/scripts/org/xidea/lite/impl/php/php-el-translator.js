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
	return (t1 & a) == a && (t2 & a) == a;
}
function stringifyADD(el,context){
	var t = getELType(el);
	var value1 = stringifyPHPEL(el[1],context);
	var value2 = stringifyPHPEL(el[2],context);
	if(t == TYPE_NUMBER){
		return value1+'+'+value2;//动态部分要考虑 array的问题,这里不需要考虑
	}else if(t == TYPE_STRING){
		if(/^[\d\.]+$/.test(value1)){
			value1+=' ';
		}
		if(/^[\d\.]+$/.test(value2)){
			value2=' '+value2;
		}
		return value1+'.'+value2;
	}
	
	var t1 = getELType(el[1]);
	var t2 = getELType(el[2]);
	if(typesOnly(t1,t1,TYPE_NULL,TYPE_NUMBER,TYPE_BOOLEAN)){
		return "lite_op__add_nx("+value1+','+value2+")"
	}
	if(typesOnly(t2,t2,TYPE_NULL,TYPE_NUMBER,TYPE_BOOLEAN)){
		return "lite_op__add_nx("+value2+','+value1+")"
	}
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
function stringifyGET(el,context){
	var arg1 = el[1];
	var arg2 = el[2];
	var value1 = stringifyPHPEL(el[1],context);
	var value2 = stringifyPHPEL(el[2],context);
	if(arg2[0] == VALUE_CONSTANTS){
		if( arg2[1] != 'length'){
			return value1+'['+value2+']';
		}
	}
	return "lite_op__get("+value1+','+value2+")"
}
function stringifyINVOKE(el,context){
	var arg1 = el[1];
	var type1 = arg1[0];
	//var value1 = stringifyPHPEL(arg1,context);
	var value2 = stringifyPHPEL(el[2],context);//array(arg1,arg2...)
	if(type1 == OP_GET){//member_call
		var oel = arg1[1];
		var pel = arg1[2];
		if(oel[0] == VALUE_VAR){
			var varName = oel[1];
		}
		if(pel[0] == VALUE_CONSTANTS){
			var prop = pel[1];
		}
		if(varName && prop){
//			random=>rand(0, PHP_INT_MAX
//			sin,sqrt,tan,cos,acos,asin,atan,atan2 
//			max,min,,floor,round,abs,ceil,exp,log,pow,
			if(varName == 'Math'){
				var mp = /^(?:sin|sqrt|tan|cos|acos|asin|atan|atan2|max|min||floor|round|abs|ceil|exp|log|pow)$/;
				if(prop == 'random'){
					return 'rand(0, PHP_INT_MAX)';
				}else if(mp.test(prop)){
					return value2.replace('array',prop);
				}else{
					$log.warn("Math 不支持方法:"+prop+";Math 支持的方法有:random|"+mp.source.replace(/[^\w\|]/g,''))
				}
			}else if(varName == 'JSON'){
				if(prop == "parse"){
					return value2.replace('array','json_parse').slice(0,-1)+',true)';
				}else if(prop =='stringify'){
					return value2.replace('array','json_encode');
				}else{
					$log.warn("JSON 不支持方法:"+prop+";JSON 只支持:stringify和parse方法")
				}
			}
		}
		return "lite_op__invoke("+stringifyPHPEL(oel,context)+","+stringifyPHPEL(pel,context)+","+value2+")"
		//value1 = value1.replace(/.*?,([\s\S]+)\)/,'array($1)');
	}else if(type1 == VALUE_VAR){
		var varName = arg1[1];
		if(varName in GLOBAL_DEF_MAP || varName in context.scope.defMap){
			//静态编译方式
			return value2.replace('array',"lite__"+varName)
		}else{
			//动态调用方式
			var fn = "isset($"+varName+")?$"+varName+":'"+varName+"'"
			return 'lite_op__invoke('+fn+',null,'+value2+')';
		}
	}else{	
		var value1 = stringifyPHPEL(arg1,context);
		return 'lite_op__invoke('+value1+',null,'+value2+')';
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