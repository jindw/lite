var LiteCompiler = require('./lite-compiler').LiteCompiler;
//var LiteCompiler = require('../../../../../../build/dest/nodejs').LiteCompiler;
var templateCompiler= new LiteCompiler(process.argv[2].replace(/\/?$/,'/'));

process.on('message', function(config) {
	var path = config.path;
	var result = templateCompiler.compile(path);
    //console.log('child got message:', m.root);
    process.send({path:path,code:result[1]})
});