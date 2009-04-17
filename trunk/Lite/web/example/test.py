#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys,os
import StringIO

from lite import TemplateEngine
from mod_python import apache
engine = new TemplateEngine("../");

def handler(req):
    req.content_type = "text/plain";
    context = {
    	"int1":1,
    	"text1":'1'
    }
    engine.render("/example/test.xhtml",context,req);
    return apache.OK

    #Template(["constants:123=",[EL_TYPE,[[VALUE_CONSTANTS,123]]],"\r\n"]).render({},output)
    #Template(["constants:v1=",[EL_TYPE,[[VALUE_VAR,"v1"]]],"\r\n"]).render({"v1":1},output)
    #test for:<c:for var="item" items="${[1,2,3,4]}" status="status">${item+status.index}</c:for>
    #Template(["test for:",[FOR_TYPE,[[EL_TYPE,  [[VALUE_VAR,"item"],[VALUE_VAR,"status"],[OP_STATIC_GET_PROP,"index"],[OP_ADD]]  ]],"item",[[VALUE_CONSTANTS,[1,2,3,4]]],"status"]]).render({},output);
    #output.close();
