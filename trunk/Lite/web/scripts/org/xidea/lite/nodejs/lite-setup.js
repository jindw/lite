//var LiteCompiler = require('./lite-compiler').LiteCompiler;
var LiteCompiler = require('../../../../../../build/dest/nodejs').LiteCompiler;
var root = process.argv[2].replace(/\/?$/,'/');
var templateCompiler= new LiteCompiler(root);

process.on('message', function(config) {
	var path = config.path;
	var result = templateCompiler.compile(path);
    //console.log('child got message:', m.root);
    process.send({path:path,code:result[1]})
});