#!/usr/bin/env python
# -*- coding: utf-8 -*-

from Template import Template
from urllib import *
from stat import *
from os.path import getmtime,exists,realpath

class TemplateEngine:
    liteBase = None;
    liteService = None;
    def __init__(self,liteBase,liteService):
        self.liteBase = realpath(liteBase);
        self.liteService = liteService;
        self.liteCached = self.liteBase+"/WEB-INF/litecached/";
    def render(self, path, context, out):
        liteCode = self.load(path,out);
        out.write(path);
        template = Template(liteCode);
        #template.render(context,out);
    def load(self,path,out):
	    liteFile = self.liteCached+quote_plus(path)
	    if exists(liteFile):
	        lite = json_decode(self.readFile(liteFile))
            paths = lite[0]
            liteTime = getmtime(liteFile)
            fileTime = liteTime
            for item in paths:
                fileTime = max(fileTime,getmtime(self.liteBase+item))
            if fileTime<=liteTime:
                return lite[1]
        else:
            out.write('123')
	    lite = self.compile(path)
        self.writeCache(liteFile,json_encode(lite))
        return lite[1]

    def readFile(self,path):
        return open(path).read()
    def writeFile(self,file,context):
        return None
def json_decode(text):
    return eval(text,None,{'true':True,'false':False,'null':None});