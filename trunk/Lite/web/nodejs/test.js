/*
require('vm').runInNewContext(
 	require('fs').readFileSync("D:\\workspace\\Lite2\\web\\nodejs\\test.js",'utf-8'),
 	{require:require},'test.js');
*/

var vm = require('vm');
var fs = require('fs');
var roots = ["D:\\workspace\\JSI2\\web\\scripts\\","D:\\workspace\\Lite2\\web\\scripts\\"];
var Path = require('path');
function loadSource(path){
	path = path.replace(/^classpath\:\/+/,'')
	for(var i=0;i<roots.length;i++){
		var tp = roots[i]+path;
		if(Path.existsSync(tp)){
			return fs.readFileSync(tp,'utf-8');
		}
	}
}

function evalSource(text,path){
	vm.runInNewContext(text,{},path);
}
var boot = loadSource('boot.js');
evalSource(boot);
