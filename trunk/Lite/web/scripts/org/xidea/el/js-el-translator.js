/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var ID_PATTERN = /^[a-zA-Z_\$][_\$\w]*$/;
/**
 * 将Lite的逆波兰式序列转化为php表达式
 */
function ELTranslator(tokens){
	if(tokens instanceof ELTranslator){
		return tokens;
	}
	this.tree = tokens;
	this.varMap = {};
	walkTree(this,this.tree)
}

ELTranslator.prototype = {
	/**
	 * 将TOKEN转化为EL 表达式
	 */
	toString:function(){
		//print("\n"+stringifyJSON(this.tree)+this.tree+"%%%\n")
		return this.stringify(this.tree);
	},
	/**
	 * 获取某个运算符号的优先级
	 */
	getPriority:function(el) {
		return getPriority(el[0]);
	},
	/**
	 * 将某一个token转化为表达式
	 */
	stringify:function(el){
		var type = el[0];
		if(type<=0){//value
			return this.stringifyValue(el)
		}else if(getTokenParamIndex(type) ==3){//两个操作数
			return this.stringifyInfix(el);
		}else{
			return this.stringifyPrefix(el);
		}
		
	},
	stringifyValue:function(el){
		var param = el[1];
		switch(el[0]){
        case VALUE_CONSTANTS:
            return (param && param.source) || stringifyJSON(param);
        case VALUE_VAR:
        	if(param == 'for'){
        		return "_$context";
        	}else{
        		return param;
        	}
        case VALUE_LIST:
        	return "[]";
        case VALUE_MAP:
        	return "{}";
		}
	},
	/**
	 * 翻译中缀运算符
	 */
	stringifyInfix:function(el){
		var type = el[0];
		var opc = findTokenText(el[0]);
		var value1 = this.stringify(el[1]);
		var value2 = this.stringify(el[2]);
		var param = getTokenParam(el);
		if(this.getPriority(el[1])<this.getPriority(el)){
			value1 = '('+value1+')';
		}
		switch(type){
//		case OP_INVOKE_WITH_ONE_PARAM:
//			value2="["+value2+']';
		case OP_INVOKE:
			value2 = value2.slice(1,-1);
			return value1+"("+value2+')';
		case OP_GET:
			value1 = toOperatable(el[1][0],value1);
			if(el[2][0] == VALUE_CONSTANTS){
				var p = getTokenParam(el[2])
				if(typeof p == 'string'){
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
			//return value1.replace(/(,?)\)$/,'$1')+value2+")"
		case OP_PUSH:
			value2 = stringifyJSON(param)+":"+value2+"}";
			if("{}"==value1){
				return "{"+value2
			}else{
				return value1.slice(0,-1)+','+value2
			}
        case OP_QUESTION:
        	return null;
        case OP_QUESTION_SELECT:
        /**
     ${a?b:c}
     ${a?b1?b2:b3:c}
     ${222+2|a?b1?b2:b3:c}
         */
         	var el1 = el[1];
        	var test = this.stringify(el1[1]);        	var value1 = this.stringify(el1[2]);
        	return test+'?'+value1+":"+value2;
		}
		if(this.getPriority(el)>=this.getPriority(el[2])){
			value2 = '('+value2+')';
		}
		return value1 + opc + value2;
	},
	/**
	 * 翻译前缀运算符
	 */
	stringifyPrefix:function(el){
		var type = el[0];
		var el1 = el[1];
		$log.warn(JSON.stringify(el))
		var value = this.stringify(el1);
		var param = getTokenParam(el);
		if(this.getPriority(el)>=this.getPriority(el1)){
    		value = '('+value+')';
    	}
	    var opc = findTokenText(type);
		return opc+value;
	}
}
function toOperatable(type,value1){
	if(type == VALUE_CONSTANTS){
		switch(value1.charAt()){
			case '"'://string
			case "'"://string
			case '/'://regexp
			break;
			default://number,boolean..
			value1 = '('+value1+')';
		}
	}
	return  value1;
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
		if(op == OP_GET){
			var arg2 = el[2];
			if(arg1[0] == VALUE_VAR && arg1[1] == 'for' && arg2[0] == VALUE_CONSTANTS){
				var param = arg2[1];
				if(param == 'index'){
					thiz.forIndex = true;
				}else if(param == 'lastIndex'){
					thiz.forLastIndex = true;
				}else{
					throw new Error("for不支持属性:"+param);
				}
				return ;
			}
		}
		arg1 && walkTree(thiz,arg1);
		
		var pos = getTokenParamIndex(el[0]);
		if(pos>2){
			walkTree(thiz, el[2]);
		}
	}
}
