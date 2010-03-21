#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys,os
import StringIO
sys.path.append(os.path.abspath("../WEB-INF/classes"))

from lite import TemplateEngine

engine = TemplateEngine("D:/workspace/Lite/web/");

#数据模型
context = {
	"int1":1,
	"text1":'1'
}
out = StringIO.StringIO();
#渲染模板
engine.render("/example/test.xhtml",context,out);

print(out.getvalue());
