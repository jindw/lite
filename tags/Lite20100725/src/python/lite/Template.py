#!/usr/bin/env python
# -*- coding: utf-8 -*-

from Expression import evaluate
from cgi import escape
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
        render_list(context, self.items, out)
def render_list(context, children, out):
    for item in children:
        try:
            if isinstance(item, str):
                out.write(item)
            else:
                if item[0] == EL_TYPE:
                    process_el(context, item, out, False)
                elif item[0] == XML_TEXT_TYPE:
                    process_el(context, item, out, True)
                elif item[0] == VAR_TYPE:
                    process_var(context, item)
                elif item[0] == CAPTRUE_TYPE:
                    process_captrue(context, item)
                elif item[0] == IF_TYPE:
                    process_if(context, item, out)
                elif item[0] == ELSE_TYPE:
                    process_else(context, item, out)
                elif item[0] == FOR_TYPE:
                    process_for(context, item, out)
                elif item[0] == XML_ATTRIBUTE_TYPE:
                    process_attribute(context, item, out)
        except Exception,e:
            out.write(str(item))
            out.write(str(e))
            pass

def print_xml_attribute(text, out):
    if isinstance(text,bool):
        out.write(text and 'true' or 'false');
    else:
        out.write(escape(str(text),True));

def print_xml_text(text, out):
    if isinstance(text,bool):
        out.write(text and 'true' or 'false');
    else:
        out.write(escape(str(text)));

def to_bool(test):
    if isinstance(test, list) or isinstance(test, dict):
        return True
    else:
        return bool(test);

def process_el(context, data, out, encodeXML):
    value = evaluate(data[1],context)
    if encodeXML and value is not None:
        print_xml_text(value, out)
    else:
        out.write(value)

def process_if(context, data, out):
    test = True
    try:
        if to_bool(evaluate(data[2],context)):
            render_list(context, data[1], out)
        else:
            test = False
    finally:
        context[IF_KEY]=test

def process_else(context, data, out):
    if not to_bool(context[IF_KEY]):
        try:
            if data[2] is None or to_bool(evaluate(data[2],context)):
                render_list(context, data[1], out)
                context[IF_KEY] = True
        except Exception,e:
            #out.write(e)
            context[IF_KEY] = True

def process_for(context, data, out):
    items = evaluate(data[2],context)
    if items == None :
        context[IF_KEY]=False;
        return;
    try:
        children = data[1]
        var_name = data[3]
        length = len(items)
        preious_status = hasattr(context,FOR_KEY) and context[FOR_KEY]
     
        for_status = ForStatus(length)
        context[FOR_KEY]=for_status
        for item in items:
            for_status.index += 1
            context[var_name]=item
            render_list(context, children, out)
    finally:
        context[FOR_KEY]=preious_status
        context[IF_KEY]= length > 0

def process_var(context, data):
    context[data[2]]= evaluate(data[1],context);

def process_captrue(context, data):
    buf = StringIO();
    render_list(context, data[1], buf);
    context[data[2]]= buf.getvalue();
    buf.close();

def process_attribute(context, data, out):
    result = evaluate(data[1],context)
    if data[2] is None:
        print_xml_attribute(result, out)
    elif result is not None:
        out.write(" ")
        out.write(data[2])
        out.write("=\"")
        print_xml_attribute(result, out)
        out.write('"')

class ForStatus(object):
    index = -1
    lastIndex = 0
    def __init__(self, end):
        self.lastIndex = end - 1
