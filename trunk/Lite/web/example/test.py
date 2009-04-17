#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys
sys.path.append('C:/Users/jindw/workspace/Lite/src/python/org/xidea/');
#print (sys.path)


from lite import Expression
from lite import Template

import StringIO
import sys



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

EL_TYPE = 0;            #// [0,<el>]
IF_TYPE = 1;            #// [1,[...],<test el>]
BREAK_TYPE = 2;         #// [2,depth]
XML_ATTRIBUTE_TYPE = 3; #// [3,<value el>,'name']
XML_TEXT_TYPE = 4;      #// [4,<el>]
FOR_TYPE = 5;           #// [5,[...],<items el>,'varName']
ELSE_TYPE = 6;          #// [6,[...],<test el>] //<test el> 可为null
ADD_ON_TYPE = 7;        #// [7,[...],<add on el>,'<addon-class>']
VAR_TYPE = 8;           #// [8,<value el>,'name']
CAPTRUE_TYPE = 9;       #// [9,[...],'var']




class Test:
    v=3
    def test(self,x):
        return x*self.v;
    def __getitem3_(self,x):
        return lambda x:x/2;
        
print("object.test(123);#123*3=369")
el = [[VALUE_VAR,"object"],[VALUE_CONSTANTS,"test"],[OP_GET_PROP],[VALUE_NEW_LIST],[VALUE_CONSTANTS,123],[OP_PARAM_JOIN],[OP_INVOKE_METHOD]];
print(Expression(el).evaluate({"object":Test()}))


print("v1 = 1;")
el= [[VALUE_VAR,"v1"]]
print(Expression(el).evaluate({"v1":1}));



def test():
    Template(["3*7=",[EL_TYPE,[[VALUE_CONSTANTS,3],[VALUE_CONSTANTS,7],[OP_MUL]]],"\r\n"]).render({},sys.stdout)
    Template(["constants:123=",[EL_TYPE,[[VALUE_CONSTANTS,123]]],"\r\n"]).render({},sys.stdout)
    Template(["constants:v1=",[EL_TYPE,[[VALUE_VAR,"v1"]]],"\r\n"]).render({"v1":1},sys.stdout)
    #test for:<c:for var="item" items="${[1,2,3,4]}" status="status">${item+status.index}</c:for>
    Template(["test for:",[FOR_TYPE,[[EL_TYPE,  [[VALUE_VAR,"item"],[VALUE_VAR,"status"],[OP_STATIC_GET_PROP,"index"],[OP_ADD]]  ]],"item",[[VALUE_CONSTANTS,[1,2,3,4]]],"status"]]).render({},sys.stdout);

test();

