from Template import *
from Expression import *
from Template import renderList
import StringIO
import sys

class Test:
    v=3
    def test(self,x):
        return x*self.v;
    def __getitem3_(self,x):
        return lambda x:x/2;
        
print("object.test(123);#123*3=369")
el = [[VALUE_VAR,"object"],[VALUE_CONSTANTS,"test"],[OP_GET_PROP],[VALUE_NEW_LIST],[VALUE_CONSTANTS,123],[OP_PARAM_JOIN],[OP_INVOKE_METHOD]];
print(evaluate(el,{"object":Test()}))

print("v1 = 1;")
el= [[VALUE_VAR,"v1"]]
print(evaluate(el,{"v1":1}));



def test():
    Template(["3*7=",[EL_TYPE,[[VALUE_CONSTANTS,3],[VALUE_CONSTANTS,7],[OP_MUL]]],"\r\n"]).render({},sys.stdout)
    Template(["constants:123=",[EL_TYPE,[[VALUE_CONSTANTS,123]]],"\r\n"]).render({},sys.stdout)
    Template(["constants:v1=",[EL_TYPE,[[VALUE_VAR,"v1"]]],"\r\n"]).render({"v1":1},sys.stdout)
    #test for:<c:for var="item" items="${[1,2,3,4]}" status="status">${item+status.index}</c:for>
    Template(["test for:",[FOR_TYPE,[[EL_TYPE,  [[VALUE_VAR,"item"],[VALUE_VAR,"status"],[OP_STATIC_GET_PROP,"index"],[OP_ADD]]  ]],"item",[[VALUE_CONSTANTS,[1,2,3,4]]],"status"]]).render({},sys.stdout);
test();
