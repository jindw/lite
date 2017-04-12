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


### NodeJS Example（Automatic Data Loading Optimization）
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


### NodeJS Example（Automatic Bigpiple && BigRender Optimization）

```xml
<html>
<head>...</head>
<body>
<div c:widget="widget/header.xhtml"/>
<div c:lazy-widget="widget/module1.xhtml">
    <!-- module1 source will build as a inner generator function, 
        and flush as __widget_arrived callback(...) when data prepared  -->
    loading module1...
</div>
<div c:lazy-widget="widget/module2.xhtml">
    loading module2...
</div>
<div c:lazy-widget="widget/module3.xhtml">loading module3...</div>
<div c:widget="widget/footer.xhtml"/>

</body>
</html>
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
