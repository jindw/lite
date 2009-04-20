#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys,os
import StringIO

from lite import TemplateEngine
#from TemplateEngine import TemplateEngine
from mod_python import apache

engine = TemplateEngine("D:/workspace/Lite/web/");

def handler(req):
    req.content_type = "text/html";
    #数据模型
    context = {
    	"int1":1,
    	"text1":'1'
    }
    #渲染模板
    engine.render("/example/test.xhtml",context,req);
    return apache.OK