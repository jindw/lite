# Lite
A cross platform template engine base on xml/html and javascript expression.

Install:
-------
>npm install lite


Innovative Features 
-------
1. Automatic BigPiple+BigRender Support.
2. Automatic encoding based on HTML semantic context.
3. Available contents transmitted immediately  and unavailable content auto wait.


Example:
====



### Simple NodeJS Example

```html
<html>
<head><title>${title}</title></head>
<body>
This is ${user.name}.
I am ${user.age} years old this year
</body>
</html>
```
```javascript
var LiteEngine = require('lite').LiteEngine;
var engine = new LiteEngine('./');
require('http').createServer(function (request, response) {
    var path = '/index.xhtml';
    var model = {
        title:'simple test',
        user:{name:'jindw',age:35}
    };
    engine.render(path,model,request,response);
}).listen(2012);
```


### NodeJS Promise Wait Example
```javascript
var LiteEngine = require('lite').LiteEngine;
var engine = new LiteEngine('./');
require('http').createServer(function (request, response) {
    //template path
    var path = '/index.xhtml';
    var data1Promise = APIProxy.getDataAsPromise('http://api.xxx.com/user?name=zhangshan&...')
    var data2Promise = APIProxy.getDataAsPromise('http://api.xxx.com/user?name=lisi&...')
    //model(available data and unavailable data(pending Promise))
    var model = {
        title:'static first and promise auto wait test'
        data1:data1Promise,//output available contents first and wait until the promise is ready!!
        data2:data2Promise
    };
    engine.render(path,model,request,response);
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


//inline template function(ECMA4XML)
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