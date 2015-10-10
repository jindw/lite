# Lite
A cross platform template engine base on xml/html and javascript expression.

Install:
-------
>npm install lite

Example:
====

### client(with jsi(https://github.com/jindw/jsi) debug server)
```javascript
//inline template
var tpl = <div>
             hello ${user.name}
             <c:if test="${user.messages}">
                <c:for var="item" list="${user.messages}">
                        <p>${item}</p>
                </c:for>
             </c:if>
          </div>
//single template file
var tpl = liteXML("../tpl/test.tpl");

//part template of single file(with css3 selector)
var tpl = liteXML('../tpl/test.tpl#header')
```

### nodejs
```javascript
var LiteEngine = require('lite').LiteEngine;
var engine = new LiteEngine('./');
require('http').createServer(function (request, response) {
	if(/\.xhtml$/.test(request.url)){
		var model = {};
    	engine.render(request.url,model,request,response);
	}
}).listen(2012);
```

 * [LiteEngine]:
    * render(url,model,request,response)
