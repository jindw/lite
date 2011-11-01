
var LiteEngine = require('../scripts/org/xidea/lite/nodejs/lite-engine').LiteEngine
//var LiteEngine = require('../../build/dest/nodelite/lite-engine').LiteEngine;

var root =  __dirname.replace(/(?:[\\\/]WEB\-INF)?[\\\/]?$/i,'/');
console.log(root)
var liteEngine = new LiteEngine(root);
liteEngine.startTestServer("127.0.0.1",1985);
