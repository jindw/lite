#!/usr/bin/env python
# -*- coding: utf-8 -*-
#compile time object
BRACKET_BEGIN = 0xFFFE;#([{;
BRACKET_END = 0xFFFF;#)]};
	

#value token（<=0）
VALUE_VAR = -0x00;#n;
#constants token（String,Number,Boolean,Null）
VALUE_CONSTANTS = -0x01;#c;
VALUE_LAZY = -0x02;
VALUE_NEW_LIST = -0x03;#[;
VALUE_NEW_MAP = -0x04;#{;
	
#op token ????? !!

#+1+2
OP_ADD = (1<<2) +2;#+;//6
OP_SUB = (2<<2) +2;#-;
	
OP_MUL = (3<<2) +2;#*;
OP_DIV = (4<<2) +2;#/;
OP_MOD = (5<<2) +2;#%;
OP_QUESTION = (6<<2) +2;#?;
OP_QUESTION_SELECT = (7<<2) +2;#:;

OP_GET_PROP = (8<<2) +2;#.;
OP_STATIC_GET_PROP = (9<<2) +1;#.#;
	
OP_LT = (0xA<<2) +2;#<;
OP_GT = (0xB<<2) +2;#>;
OP_LTEQ = (0xC<<2) +2;#<=;
OP_GTEQ = (0xD<<2) +2;#>=;
OP_EQ = (0xE<<2) +2;#==;
OP_NOTEQ = (0xF<<2) +2;#!=;
OP_AND = (0x10<<2) +2;#&&;
OP_OR = (0x12<<2) +2;#||;
	
	

OP_NOT = (0x13<<2) +1;#!;
OP_POS = (0x14<<2) +1;#+;
OP_NEG = (0x15<<2) +1;#-;

OP_GET_METHOD = (0x16<<2) +2;#.();
OP_INVOKE_METHOD = (0x17<<2) +2;#()


#map_join
OP_PARAM_JOIN = (0x18<<2) +2;#,
#map,push
OP_MAP_PUSH = (0x19<<2) +2;#:,

globalMap = {
    "test":lambda x:x*3
}

#
SKIP_QUESTION = object();


def evaluate(tokens, context):
    stack=[]
    _evaluate(stack, tokens, context)
    return stack[0]
def _evaluate(stack, tokens, context):
    for item in tokens:
        if isinstance(item,list):
	        type = item[0];
	        if type > 3:
	            arg2 = None
	            arg1 = None
	            length = type & 3
	            if length > 1:
	                arg2 = stack.pop()
	                arg1 = stack.pop()
	            else:
	                if (length == 1):
	                    arg1 = stack.pop()
	            if type == OP_PARAM_JOIN:
	                arg1.append(arg2)
	                stack.append(arg1)
	            elif type == OP_MAP_PUSH:
	                arg1[item[1]]= arg2;
	                stack.append(arg1)
	            else:
	                result = compute(item, arg1, arg2)
	                if isinstance(result, (LazyToken)):
	                    _evaluate(stack, result.children, context)
	                else:
	                    stack.append(result)
	        else:
	            stack.append(getTokenValue(context, item))
	    
        else:
            stack.append(item)
def compute(op,arg1,arg2):
    type = op[0];
    if type == OP_NOT:
        return not arg1;
    elif type == OP_POS:
        return +arg1;
    elif type == OP_NEG:
        return -arg1;
        #/* +-*%/ */
    elif type == OP_ADD:
        return arg1+arg2;
    elif type == OP_SUB:
        return arg1-arg2;
    elif type == OP_MUL:
        return arg1*arg2;
    elif type == OP_DIV:
        return arg1/arg2;
    elif type == OP_MOD:
        return arg1%arg2;
        #/* boolean */
    elif type == OP_GT:
        return arg1 > arg2;
    elif type == OP_GTEQ:
        return arg1 >= arg2;
    elif type == OP_NOTEQ:
        return arg1 != arg2;
    elif type == OP_EQ:
        return arg1 == arg2;
    elif type == OP_LT:
        return arg1 < arg2;
    elif type == OP_LTEQ:
        return arg1 <= arg2;

        #/* and or */
    elif type == OP_AND:
        return arg2 and arg1;
    elif type == OP_OR:
        return arg1 or arg2;
    elif type == OP_QUESTION:#// a?b:c -> a?:bc -- >a?b:c
        if arg1:
            return arg2;
        else:
            return SKIP_QUESTION;
    elif type == OP_QUESTION_SELECT:
        if arg1 == SKIP_QUESTION:
            return arg2;
        else:
            return arg1;
    elif type == OP_GET_PROP:
        return arg1[arg2];
    elif type == OP_STATIC_GET_PROP:
        return arg1[op[1]];
    elif type == OP_GET_METHOD:
        #TODO...
        if(isinstance(arg1,dict ) and arg2 in arg1):
            return lambda *args:apply(arg1[arg2],args);
        else:
            return lambda *args:apply(getattr(arg1,arg2),args);
    elif type == OP_INVOKE_METHOD:
        return apply(arg1,arg2);
    elif type == OP_PARAM_JOIN:
        arg1.append(arg2);
        return arg1;
    elif type == OP_MAP_PUSH:
        arg1[op[1]] =arg2;
        return arg1;
def getTokenValue(context, item):
    type = item[0];
    if  type == VALUE_CONSTANTS:
        return item[1];
    elif type == VALUE_VAR:
        value = item[1]
        if "this" == value:
            return context
        else:
            if value in context:
                return context[value]
            elif value in globalMap:
                return globalMap[value]
            return None
    elif type == VALUE_NEW_LIST:
        return []
    elif type == VALUE_NEW_MAP:
        return {}
    elif type == VALUE_LAZY:
        return LazyToken(item[1])
class LazyToken:
    children = 0;
    def __init__(self, el):
        self.children = children

class Test:
    test2=1
    def test(self,x):
        return x*2;
    def __getitem3_(self,x):
        return lambda x:x/2;

print(evaluate([[0,"object"],[-1,"test"],[90],[-3],[-1,123],[98],[94]],{"object":Test()}))

