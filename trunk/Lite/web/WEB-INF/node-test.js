
if(process.argv[2] == 'debug'){//for debug
	console.info('debug model');
	var LiteEngine = require('../scripts/org/xidea/lite/nodejs/lite-engine').LiteEngine
}else{
	var LiteEngine = require('../../build/dest/nodelite/lite-engine').LiteEngine;
}

var root = /^\//.test(process.argv[2])?process.argv[2] : __dirname.replace(/(?:\/WEB\-INF)?\/?/i,'/');
var liteEngine = new LiteEngine(root);
liteEngine.startTestServer("127.0.0.1",1985);
