#!/usr/bin/env python
# -*- coding: utf-8 -*-

from Expression import evaluate

EL_TYPE = 0
VAR_TYPE = 1
IF_TYPE = 2
ELSE_TYPE = 3
FOR_TYPE = 4
BREAK_TYPE = 5
EL_TYPE_XML_TEXT = 6
ATTRIBUTE_TYPE = 7
ATTRIBUTE_VALUE_TYPE = 8
CAPTRUE_TYPE = 9
FOR_KEY = "for"
IF_KEY = "if"

class ForStatus(object):
    """ generated source for ForStatus

    """
    index = -1
    lastIndex = 0

    def __init__(self, end):
        self.lastIndex = end - 1
def renderList(context, children, out):
    for item in children:
        try:
            if isinstance(item, str):
                out.write(item)
            else:
                if item[0] == EL_TYPE:
                    processExpression(context, item, out, False)
                elif item[0] == EL_TYPE_XML_TEXT:
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
                elif item[0] == ATTRIBUTE_VALUE_TYPE:
                    processAttributeValue(context, item, out)
                elif item[0] == ATTRIBUTE_TYPE:
                    processAttribute(context, item, out)
        except (Exception, ), e:
            print "aaaaa" 
            print item[0]
            print e
            pass

def printXMLAttribute(text, context, out, escapeSingleChar):
    ## for-while
    for c in text:
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
        elif c == '\'':
            if escapeSingleChar:
                out.write("&#39;")
            break
        else:
            out.write(c)

def printXMLText(text, out):
    ## for-while
    for c in text:
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
        printXMLText(str(value), out)
    else:
        out.write(str(value))

def processIf(context, data, out):
    test = True
    try:
        if toBoolean(evaluate(data[2],context)):
            renderList(context, data[1], out)
        else:
            test = True
    finally:
        test = True
    context[IF_KEY]=test

def processElse(context, data, out):
    if not toBoolean(context[IF_KEY]):
        try:
            if data[2] is None or toBoolean(evaluate(data[2],context)):
                renderList(context, data[1], out)
                context[IF_KEY] = Boolean.TRUE
        except (Exception, ), e:
            if log.isDebugEnabled():
                log.debug(e)
            context[IF_KEY] = Boolean.TRUE

def processFor(context, data, out):
    children = data[1]
    varName = data[2]
    statusName = data[4]
    items = evaluate(data[3],context)
    length = len(items)
    preiousStatus = hasattr(context,FOR_KEY) and context[FOR_KEY]
    try:
        
        forStatus = ForStatus(length)
        context[FOR_KEY]=forStatus
        if statusName is not None:
            context[statusName]=forStatus
        for item in items:
            forStatus.index += 1
            context[varName]=item
            renderList(context, children, out)
        if statusName is not None:
            context[statusName]=preiousStatus
    finally:
        context[FOR_KEY]=preiousStatus
        context[IF_KEY]= length > 0

def processVar(context, data):
    context.put(data[2], evaluate(data[1],context))

def processCaptrue(context, data):
    buf = StringWriter()
    renderList(context, data[1], buf)
    context.put(data[2], str(buf))

def processAttribute(context, data, out):
    result = evaluate(data[1],context)
    if result is not None:
        out.write(" ")
        out.write(data[2])
        out.write("=\"")
        printXMLAttribute(str(result), context, out, False)
        out.write('"')

def processAttributeValue(context, data, out):
    printXMLAttribute(str(evaluate(data[1],context)), context, out, True)