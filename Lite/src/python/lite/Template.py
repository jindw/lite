#!/usr/bin/env python
# -*- coding: utf-8 -*-

from Expression import evaluate
from StringIO import StringIO

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

FOR_KEY = "for"
IF_KEY = "if"

class Template:
    items = None
    def __init__(self, list):
        self.items = list
        
    def render(self, context, out):
        renderList(context, self.items, out)
def renderList(context, children, out):
    for item in children:
        try:
            if isinstance(item, str):
                out.write(item)
            else:
                if item[0] == EL_TYPE:
                    processExpression(context, item, out, False)
                elif item[0] == XML_TEXT_TYPE:
                    processExpression(context, item, out, True)
                elif item[0] == VAR_TYPE:
                    processVar(context, item)
                elif item[0] == CAPTRUE_TYPE:
                    processCaptrue(context, item)
                elif item[0] == IF_TYPE:
                    processIf(context, item, out)
                elif item[0] == ELSE_TYPE:
                    processElse(context, item, out)
                elif item[0] == FOR_TYPE:
                    processFor(context, item, out)
                elif item[0] == XML_ATTRIBUTE_TYPE:
                    processAttribute(context, item, out)
        except (Exception, ), e:
            #print item[0]
            out.write(str(e))
            pass

def printXMLAttribute(text, out):
    if isinstance(text,bool):
        out.write(text and 'true' or 'false');
    else:
	    for c in str(text):
	        if c == '<':
	            out.write("&lt;")
	            break
	        elif c == '>':
	            out.write("&gt;")
	            break
	        elif c == '&':
	            out.write("&amp;")
	            break
	        elif c == '"':
	            out.write("&#34;")
	            break
	        else:
	            out.write(c)

def printXMLText(text, out):
    if isinstance(text,bool):
        out.write(text and 'true' or 'false');
    else:
	    for c in str(text):
	        if c == '<':
	            out.write("&lt;")
	            break
	        elif c == '>':
	            out.write("&gt;")
	            break
	        elif c == '&':
	            out.write("&amp;")
	            break
	        else:
	            out.write(c)

def toBoolean(test):
    if test is None:
        return False
    else:
        if isinstance(test, list) or isinstance(test, dict):
            return true
        elif test:
            return True
        else:
            return False
    return True

def processExpression(context, data, out, encodeXML):
    value = evaluate(data[1],context)
    if encodeXML and value is not None:
        printXMLText(value, out)
    else:
        out.write(value)

def processIf(context, data, out):
    test = True
    try:
        if toBoolean(evaluate(data[2],context)):
            renderList(context, data[1], out)
        else:
            test = True
    finally:
        context[IF_KEY]=test

def processElse(context, data, out):
    if not toBoolean(context[IF_KEY]):
        try:
            if data[2] is None or toBoolean(evaluate(data[2],context)):
                renderList(context, data[1], out)
                context[IF_KEY] = True
        except (Exception, ), e:
            #if log.isDebugEnabled():
            #    log.debug(e)
            context[IF_KEY] = True

def processFor(context, data, out):
    children = data[1]
    items = evaluate(data[2],context)
    varName = data[3]
    length = len(items)
    preiousStatus = hasattr(context,FOR_KEY) and context[FOR_KEY]
    try:
        forStatus = ForStatus(length)
        context[FOR_KEY]=forStatus
        for item in items:
            forStatus.index += 1
            context[varName]=item
            renderList(context, children, out)
    finally:
        context[FOR_KEY]=preiousStatus
        context[IF_KEY]= length > 0

def processVar(context, data):
    context[data[2]]= evaluate(data[1],context);

def processCaptrue(context, data):
    buf = StringIO();
    renderList(context, data[1], buf)
    context[data[2]]= buf.getvalue();

def processAttribute(context, data, out):
    result = evaluate(data[1],context)
    if data[2] is None:
        printXMLAttribute(result, out)
    elif result is not None:
        out.write(" ")
        out.write(data[2])
        out.write("=\"")
        printXMLAttribute(result, out)
        out.write('"')

class ForStatus(object):
    """ generated source for ForStatus

    """
    index = -1
    lastIndex = 0

    def __init__(self, end):
        self.lastIndex = end - 1
