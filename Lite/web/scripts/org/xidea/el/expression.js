/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
/**
 * 表达式对象，可以单步解释表达式中间代码
 */
function Expression(el){
	if(typeof el == 'string'){
		el = new ExpressionTokenizer(el).getResult();
	}
	this.el = el;
}
Expression.prototype.evaluate = function(context){
     return evaluate(this.el,context);
}
/**
 * 表达式单步解析函数实现
 */
function evaluate(el,context){
     var result = _evaluate(el,context)
     return realValue(result);
}


function _evaluate(item,context){
    var type = item[0];
    switch(type){
    case VALUE_LIST:
        return [];
    case VALUE_MAP:
        return {};
    case VALUE_VAR:
        arg1 = item[1]
        return (arg1 in context?context:this)[arg1];
    case VALUE_CONSTANTS:
        return item[1];
    ///* and or */
    case OP_AND:
        return realValue(_evaluate(item[1],context)) && (_evaluate(item[2],context));
    case OP_OR:
        return realValue(_evaluate(item[1],context)) || (_evaluate(item[2],context));
    case OP_QUESTION://// a?b:c -> a?:bc -- >a?b:c
        if(realValue(_evaluate(item[1],context))){
            return _evaluate(item[2],context);
        }else{
            return PropertyValue;//use as flag
        }
    case OP_QUESTION_SELECT:
    	arg1 = realValue(_evaluate(item[1],context));
        if(arg1 == PropertyValue){//use as flag
            return _evaluate(item[2],context);
        }else{
            return arg1;
        }
    }
    var arg1=_evaluate(item[1],context);
    if(getTokenParamIndex(type) ==3){
        var arg2=realValue(_evaluate(item[2],context));
    }
    if(type == OP_INVOKE){
    	if(arg1 instanceof Function){
            return arg1.apply(context,arg2);
    	}else if(arg1 instanceof PropertyValue){
            return arg1[0][arg1[1]].apply(arg1[0],arg2);
    	}else{
    		throw new Error("not a fn!!"+arg1)
    	}
    }
    arg1 = realValue(arg1);
    switch(type){
    //op
//    case OP_GET_STATIC_PROP:
//        arg2 =getTokenParam(item);
    case OP_GET:
        return new PropertyValue(arg1,arg2);
    case OP_NOT:
        return !arg1;
    case OP_POS:
        return +arg1;
    case OP_NEG:
        return -arg1;
        ///* +-*%/ */
    case OP_ADD:
        return arg1+arg2;
    case OP_SUB:
        return arg1-arg2;
    case OP_MUL:
        return arg1*arg2;
    case OP_DIV:
        return arg1/arg2;
    case OP_MOD:
        return arg1%arg2;
        ///* boolean */
    case OP_GT:
        return arg1 > arg2;
    case OP_GTEQ:
        return arg1 >= arg2;
    case OP_NE:
        return arg1 != arg2;
    case OP_NE_STRICT:
        return arg1 !== arg2;
    case OP_EQ:
        return arg1 == arg2;
    case OP_EQ_STRICT:
        return arg1 === arg2;
        
    case OP_LT:
        return arg1 < arg2;
    case OP_LTEQ:
        return arg1 <= arg2;


    case OP_JOIN:
        arg1.push(arg2)
        return arg1;
    case OP_PUSH:
        arg1[getTokenParam(item)]= arg2;
        return arg1;
    }
}

function PropertyValue(base,name){
    this[0] = base;
    this[1] = name;
}
function realValue(arg1){
    if(arg1 instanceof PropertyValue){
        return arg1[0][arg1[1]];
    }
    return arg1;
}

