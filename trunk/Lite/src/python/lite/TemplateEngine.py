#!/usr/bin/env python
# -*- coding: utf-8 -*-

from Template import Template
from urllib import quote_plus
from stat import *
from os.path import getmtime

class TemplateEngine:
    liteBase = None;
    liteService = None;
    def __init__(self,liteBase,liteService):
        self.liteBase = liteBase;
        self.liteService = liteService;
        self.liteCached = liteBase+"/WEB-INF/litecached/";
    def render(self, path, context, out):
        liteCode = self.load(path);
        template = new Template(liteCode);
        template.render(context,out);
    def load(self,path):
	    liteFile = this.liteCached+urllib.quote_plus(path);
	    if(file_exists(liteFile)){
	    	lite = json_decode(readFile(liteFile));
	    	paths = lite[0];
	    	liteTime = getmtime(liteFile);
	    	fileTime = liteTime;
			for item in paths:
				fileTime = max(fileTime,getmtime(self.liteBase+item));
			}
			if(fileTime<=liteTime){
				return lite[1];
			}
	    }
	    lite = self.compile(path);
	    self.writeCache(liteFile,json_encode(lite));
	    return lite[1];
	def readFile(self,path):
	    open
	def writeFile(self,file,context):
	    
def json_decode(text):
    return eval(text,None,{'true':True,'false':False});