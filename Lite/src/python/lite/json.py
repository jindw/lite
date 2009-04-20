#!/usr/bin/env python
# -*- coding: utf-8 -*-
try:
    from json import loads,dumps
    def json_decode(text):
        return loads(text);
    def json_encode(o):
        return dumps(o);
    print(str(Exception)+"???")
except Exception,e:
    import re
    jsonFinder = re.compile(r'"(?:\\"|[^\"])*"|true|false|null');
    def jsonReplacer( match ):
        text = match.group(0);
        if text == 'true':
            return "True";
        elif text == 'false':
            return "False";
        elif text == 'null':
            return "None";
        else:
            return text.replace('\\/','/');
    def json_decode(text):
        text = jsonFinder.sub(jsonReplacer,text)
        return eval(text,None);
    def json_encode(o):
        return repr(o);