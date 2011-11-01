//for debug
if(process.argv[2] == 'debug'){
	console.info('debug model');
	var LiteEngine = require('../scripts/org/xidea/lite/nodejs/lite-engine').LiteEngine
}else{
	var LiteEngine = require('./lite-engine').LiteEngine;
}
exports.LiteEngine = require('../../build/dest/nodejs/lite-engine').LiteEngine