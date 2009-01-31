function Expression(tokens){
	this.tokens = tokens;
}
Expression.prototype.evaluate = function(context){
     try{
         return evaluate(this.tokens,context);
     }catch(e){
         $log.trace(e);
     }
}
function evaluate(el,context){
     var stack = [];
     _evaluate(stack,el,context)
     return realValue(stack[0]);
}
function _evaluate(stack, tokens,context){
    for(var i=0;i<tokens.length;i++){
        var item = tokens[i]
        var type = item[0];
        var arg1 = null;
        var arg2 = null
        switch(type){
            case VALUE_NEW_LIST:
                stack.push([]);
                break;
            case VALUE_NEW_MAP:
                stack.push({});
                break;
            case VALUE_VAR:
                arg1 = item[1]
                stack.push("this" == arg1?context:(arg1 in context?context:this)[arg1]);
                break;
            case VALUE_LAZY:
                stack.push(new LazyToken(item[1]));
                break;
            case VALUE_CONSTANTS:
                stack.push(item[1]);
                break;
            default:
                switch(type & 3){
                    case 2:
                        arg1 = stack.pop();
                    case 1:
                        arg2=arg1;
                        arg1 = stack.pop();
                }
                arg1 = compute(item, arg1, arg2)
                if(arg1 instanceof LazyToken){
                    _evaluate(stack, arg1.data, context)
                } else{
                    stack.push(arg1)
                }
        }
    }
}
function realValue(arg1){
	if(arg1 instanceof PropertyValue){
    	return arg1.base[arg1.name];
    }
    return arg1;
}
function compute(op,arg1,arg2){
    var type = op[0];
    if(type == OP_INVOKE_METHOD){
    	if(arg1 instanceof Function){
            return arg1.apply(null,arg2);
    	}else if(arg1 instanceof PropertyValue){
            return arg1.base[arg1.name].apply(arg1.base,arg2);
    	}else{
    		throw new Error("not a fn!!"+arg1)
    	}
    }
    arg1 = realValue(arg1);
    arg2 = realValue(arg2);
    switch(type){
    case OP_STATIC_GET_PROP:
        arg2 = op[1]
    case OP_GET_PROP:
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
    case OP_NOTEQ:
        return arg1 != arg2;
    case OP_EQ:
        return arg1 == arg2;
    case OP_LT:
        return arg1 < arg2;
    case OP_LTEQ:
        return arg1 <= arg2;

        ///* and or */
    case OP_AND:
        return arg2 && arg1;
    case OP_OR:
        return arg1 || arg2;
    case OP_QUESTION://// a?b:c -> a?:bc -- >a?b:c
        if(arg1){
            return arg2;
        }else{
            return LazyToken;//use as flag
        }
    case OP_QUESTION_SELECT:
        if(arg1 == LazyToken){//use as flag
            return arg2;
        }else{
            return arg1;
        }
    case OP_PARAM_JOIN:
        arg1.push(arg2)
        return arg1;
    case OP_MAP_PUSH:
        arg1[op[1]]= arg2;
        return arg1;
    }
}
function LazyToken(value){
    this.value = value;
}
function PropertyValue(base,name){
    this.base = base;
    this.name = name;
}

