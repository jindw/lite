/*============== for use ===================*/
var LiteEngine = require('lite').LiteEngine;
var root =__dirname;
var liteEngine = new LiteEngine(root);
.....
			
	liteEngine.render(path,data,response);
	liteEngine.render(path,callback,response);//传入模型的 callback方法，用于实现计算前输出。
		
			
/*============== for simple test ===================*/
var LiteEngine = require('lite').LiteEngine;
var liteEngine = new LiteEngine("./");
//debug server
liteEngine.startTestServer("127.0.0.1",1985);

/*============= for line code test ==============*/ 
new (require('lite').LiteEngine)('./').	startTestServer("127.0.0.1",1985);