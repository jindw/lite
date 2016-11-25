# Lite
A cross platform template engine base on xml/html and javascript expression.

Install:
-------
>npm install lite


Innovative features 
====
1. Available content parallel download and unavailable content auto wait
2. Automatic Bigpiple+Bigrender Support

Example:
====



### NodeJS Example

```javascript
var LiteEngine = require('lite').LiteEngine;
var engine = new LiteEngine('./');
require('http').createServer(function (request, response) {
    //
    var model = {
        title:'static first and promise auto wait test'
        data1:model1Promise,//output available contents first and wait until the promise is ready!!
        data2:model2Promise
    };
    engine.render(request.url,model,request,response);
}).listen(2012);
```

 * [LiteEngine]:
    * render(url,model,request,response)


### Web Browser
```javascript
//autocompile via jsi(https://github.com/jindw/jsi) 
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