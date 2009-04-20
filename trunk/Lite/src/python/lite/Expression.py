#!/usr/bin/env python
# -*- coding: utf-8 -*-

#value token（<=0）
VALUE_CONSTANTS = -0x00;#c;
VALUE_VAR = -0x01;#n;
VALUE_LAZY = -0x02;
VALUE_NEW_LIST = -0x03;#[;
VALUE_NEW_MAP = -0x04;#{;
	
#符号标记 ????? !!
#9
OP_GET_PROP = 17;#0 | 16 | 1;
OP_STATIC_GET_PROP = 48;#32 | 16 | 0;
OP_INVOKE_METHOD = 81;#64 | 16 | 1;
#8
OP_NOT = 14;#0 | 14 | 0;
OP_POS = 46;#32 | 14 | 0;
OP_NEG = 78;#64 | 14 | 0;
#7
OP_MUL = 13;#0 | 12 | 1;
OP_DIV = 45;#32 | 12 | 1;
OP_MOD = 77;#64 | 12 | 1;
#6
OP_ADD = 11;#0 | 10 | 1;
#5
OP_SUB = 41;#32 | 8 | 1;
#4
OP_LT = 7;#0 | 6 | 1;
OP_GT = 39;#32 | 6 | 1;
OP_LTEQ = 71;#64 | 6 | 1;
OP_GTEQ = 103;#96 | 6 | 1;
OP_EQ = 135;#128 | 6 | 1;
OP_NOTEQ = 167;#160 | 6 | 1;
#3
OP_AND = 5;#0 | 4 | 1;
OP_OR = 37;#32 | 4 | 1;
#2
OP_QUESTION = 3;#0 | 2 | 1;
OP_QUESTION_SELECT = 35;#32 | 2 | 1;
#1
OP_PARAM_JOIN = 1;#0 | 0 | 1;
OP_MAP_PUSH = 33;#32 | 0 | 1;



import urllib

from json import json_encode,json_decode;
globalMap = {
    "JSON":{
        "stringify":json_encode,#TODO:还是用通用json 库吧
        "parse":json_decode    #TODO:还是用通用json 库吧
    },#TODO:JSON处理
    "encodeURIComponent":lambda t:urllib.quote_plus(t),#TODO:URL编码处理
    "decodeURIComponent":lambda t:urllib.unquote_plus(t),#TODO:URL编码处理
    "test":lambda x:x*3
}

#
SKIP_QUESTION = object();


def evaluate(tokens, context):
    stack=[]
    _evaluate(stack, tokens, context)
    stack = stack[0]
    if(isinstance(stack,PropertyValue )):
        stack = stack.getValue();
    return stack
def _evaluate(stack, tokens, context):
    for item in tokens:
        if isinstance(item,list):
            type = item[0];
            if type > 0:
                arg1 = stack.pop()
                arg2 = arg1
                if type & 1:
                    arg1 = stack.pop()
                result = compute(item, arg1, arg2)
                if isinstance(result, (LazyToken)):
                    _evaluate(stack, result.children, context)
                else:
                    stack.append(result);
            else:
                stack.append(getTokenValue(context, item));
        else:
            stack.append(item)
def compute(op,arg1,arg2):
    type = op[0];
    if type == OP_INVOKE_METHOD:
        if isinstance(arg1,PropertyValue):
            return apply(arg1.getValue(),arg2);
        else:
            return apply(arg1,arg2);
    if isinstance(arg1,PropertyValue):
        arg1 = arg1.getValue();
    if isinstance(arg2,PropertyValue):
        arg2 = arg2.getValue();
        
    if type == OP_STATIC_GET_PROP:
        return PropertyValue(arg1,op[1]);
    elif type == OP_GET_PROP:
        return PropertyValue(arg1,arg2);
    elif type == OP_PARAM_JOIN:
        arg1.append(arg2)
        return arg1;
    elif type == OP_MAP_PUSH:
        arg1[op[1]]= arg2;
        return arg1;
    elif type == OP_NOT:
        return not arg1;
    elif type == OP_POS:
        return +arg1;
    elif type == OP_NEG:
        return -arg1;
        #/* +-*%/ */
    elif type == OP_ADD:
        if isinstance(arg1,str):
            return arg1 + str(arg2);
        elif isinstance(arg2, str):
            return str(arg1) + arg2;
        else:
            return arg1+arg2;
    elif type == OP_SUB:
        return arg1-arg2;
    elif type == OP_MUL:
        return arg1*arg2;
    elif type == OP_DIV:
        if arg1 % arg2:
            return float(arg1)/arg2;
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
    def __init__(self, children):
        self.children = children

class PropertyValue:
    def __init__(self, base,name):
        self.base = base;
        self.name = name;
    def getValue(self):
       if(isinstance(self.base,dict ) and self.name in self.base):
            return self.base[self.name];
       else:
            return getattr(self.base,self.name);

class Expression:
    """ generated source for ExpressionImpl

    """
    source = ""
    expression = []

    def __init__(self, el):
        self.expression = el

    def evaluate(self, context):
        if context is None:
            context = {}
        return evaluate(self.expression, context)