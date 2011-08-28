/*
require("D:\\workspace\\Lite2\\web\\nodejs\\test.js")
*/

var vm = require('vm');
var fs = require('fs');
var roots = ["D:\\workspace\\JSI2\\web\\scripts\\","D:\\workspace\\Lite2\\web\\scripts\\"];
var Path = require('path');
var $JSI = {
	impl:{
		loadText : loadSource,
		eval : evalSource,
		log:function(title,level,msg){console.info(msg)}
	}
}
function loadSource(path){
	path = path.replace(/^classpath\:\/+/,'')
	for(var i=0;i<roots.length;i++){
		var tp = roots[i]+path;
		if(Path.existsSync(tp)){
			console.info('!!'+tp);
			var s = fs.readFileSync(tp,'utf8');
			console.info('!#!'+s);
			return s;
		}
	}
}

function evalSource(text,path){
	vm.runInNewContext(text,{$JSI:$JSI},path);
}
var boot = loadSource('boot2.js');
//evalSource(boot);
