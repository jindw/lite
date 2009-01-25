#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys,os
sys.path.append(
os.path.realpath(".")
);
print(
os.path.realpath(".")
);
import StringIO
from lite import Template


json = ["<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><body><h1>",[6,[[0,"name"]]],"</h1><table",[7,[[0,"border"]],"border"],"><tr><th> </th>",[4,["<th>",[6,[[0,"cell"]]],"</th>"],"cell",[[0,"data"]],None],"</tr>",[4,["<tr><th>",[6,[[0,"row"]]],"</th>",[4,["<td>&#x",[6,[[0,"row"]]],[6,[[0,"cell"]]],";</td>"],"cell",[[0,"data"]],None],"</tr>"],"row",[[0,"data"]],None],"</table></body></html>"];

template = Template(json)
context = {
    "data" : list("0123456789ABCDEF"),
    "name": "test2",
    "border": "1px"
}

output = StringIO.StringIO();
template.render(context,output)
#print output.getvalue()

import time
t1 = time.time();
for n in range(1,1000):
    template.render(context,StringIO.StringIO());
    
print(time.time()-t1)

#8.90599989891妙