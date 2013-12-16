# Lite
A cross platform template engine base on xml/html and javascript expression.

Install:
-------
>npm install lite

Example:
====
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
