
var LiteEngine = require('lite').LiteEngine;
var root =__dirname;
var liteEngine = new LiteEngine(root);
.....
	// 传入模板路径(以'/'开头)，模型对象(Object描述的key->value集)，
	// response 对象:用于模板渲染时输出 head(response.writeHead())/内容(response.write())
	// 并结束请求(response.end())
	liteEngine.render(path,data,response);
	//传入模型生成函数，用于提前输出静态内容(如script标签)，优化加载性能。
	liteEngine.render(path,callback,response);
			
/*============== for simple test ===================*/
new (require('lite').LiteEngine)('./').	startTestServer("127.0.0.1",1985);