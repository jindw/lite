# Lite
A cross platform template engine base on xml/html and javascript expression.

Install:
-------
>npm install lite

Example:
====



### NodeJS Example

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


### Web Browser (with jsi(https://github.com/jindw/jsi) debug server)
```javascript

//single template file
var tpl = require("../tpl/test.tpl");
var model = {user:...}
var html = tpl(model);


//inline template function
var tplFunction = function(user){
    return <div>
             hello ${user.name}
             <c:if test="${user.messages}">
                <c:for var="item" list="${user.messages}">
                        <p>${item}</p>
                </c:for>
             </c:if>
          </div>
}
//inline template string
var user = ...
var tplString = <div>
             hello ${user.name}
             <c:if test="${user.messages}">
                <c:for var="item" list="${user.messages}">
                        <p>${item}</p>
                </c:for>
             </c:if>
          </div>;

```



### Other Server Side Impl

```php
<?php
require_once("LiteEngine.php");
$root = realpath(__DIR__.'/../').'/';
$engine = new LiteEngine($root);


$path =  '/test/index.xhtml';
$context = array("int1"=>1,"text1"=>'1');
$engine->render($path,$context);
```