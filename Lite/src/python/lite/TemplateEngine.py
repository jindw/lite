#!/usr/bin/env python
# -*- coding: utf-8 -*-

from Expression import json_decode
from Template import Template
from urllib import *
from StringIO import StringIO
from stat import *
from os.path import getmtime,exists,realpath
    
class TemplateEngine:
    liteBase = None;
    liteService = "http://litecompiler.appspot.com";
    def __init__(self,liteBase=None,liteService=None):
        self.liteBase = realpath(liteBase);
        if liteService:
            self.liteService = liteService;
        self.liteCached = self.liteBase+"/WEB-INF/litecached/";
    def render(self, path, context, out):
        liteCode = self.load(path,out);
        template = Template(liteCode);
        template.render(context,out);
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
        lite = self.compile(path,liteFile,out)
        return lite[1]
    def compile(self,path,liteFile,out):
        paths = [path];
        sources = [self.readFile(self.liteBase+path)];
        decoratorPath = '/WEB-INF/decorators.xml';
        decoratorXml = self.readFile(self.liteBase+decoratorPath);
        if decoratorXml:
            sources.append(decoratorXml);
            paths.append(decoratorPath);
        #最多尝试6次
        test = 6;
        while test>0 :
            test = test-1;
            code = self.httpLoad(paths,sources,out);
            if not code:
                continue;
            result = json_decode(code);
            if not result:
                continue;
            if not isinstance(result,list):
                missed = result["missed"];
                retry = False;
                for path in missed:
                    if path not in paths:
                        content = self.readFile(self.liteBase+path);
                        sources.append(content);
                        paths.append(path);
                        retry = True;
                if not retry:
                    return [paths,[code]];
            else:
                self.writeCache(liteFile,paths,code)
                return [paths,result];
                
    def httpLoad(self,paths,sources,out):
        params = urlencode(
            {
                'source':sources,
                'path':paths,
                'compress':'true',
                'base':'/'
            },True
        );
        out.write(self.liteService);
        out.write(params);
        request = urlopen(self.liteService, params);
        return request.read() 
    def readFile(self,path):
        f = open(path,'r');
        content = f.read();
        f.close()
        return content;
    def writeCache(self,file,paths,liteCode):
        f = open(file,'w');
        f.write('[[');
        first = True;
        for file in paths:
            if(first):
                first = False;
            else:
                f.write(',');
            f.write('"');
            f.write(file);
            f.write('"');
        f.write('],');
        f.write(liteCode);
        f.write(']');
        f.flush() 
        f.close() 

    
    