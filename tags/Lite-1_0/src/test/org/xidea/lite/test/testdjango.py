#!/usr/bin/env python
# -*- coding: utf-8 -*-
from django import template
import time
s = """
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<body>
	<h1>{{name}}</h1>
	<table border="{{border}}">
		<tr>
			<th>&#160;</th>
			{% for cell in data %}
				<th>${cell}</th>
			{% endfor%}
		</tr>
		{% for row in data %}
			<tr>
				<th>{{row}}</th>
				{% for cell in data %}
					<td>
					&#x{{row}}{{cell}};
					</td>
				{% endfor%}
			</tr>
		{% endfor%}
	</table>
</body>
</html>
"""
t = template.Template(s)
context = {
    "data" : list("0123456789ABCDEF"),
    "name": "test2",
    "border": "1px"
}
#context = {'test':True, 'varvalue': 'Hello'}
print t.render(template.Context(context))

t1 = time.time();
for n in range(1,1000):
    t.render(template.Context(context))
    
print(time.time()-t1)
##23.0780000687妙

