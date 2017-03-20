var LiteEngine = require('lite').LiteEngine;
var path = require('path');
var fs = require('fs');
var http = require('http');
var root = path.resolve(__dirname,'../../../');
var litecache = path.join(root,'.litecache');
console.log(litecache)
var engine = new LiteEngine(root,{litecache:litecache,released:false});

function toPromiseModel(model){
	var model2= {};
	for(var n in model){
		model2 [n] = bindPromise(model [n])
	}
	return model2;
}
function bindPromise(value){
	return new Promise(function(accept,reject){
			if(Math.random()>.5){
				setTimeout(reject(new Error()),Math.random()*1000);
			}else{
				setTimeout(accept(value),Math.random()*1000);
			}
		});
}
require('./file-server').createServer(function (req, response,root) {
	var url = req.url;
	var param = {};
	var p = url.indexOf('?');
	if(p>0){
		var query = url.substring(p+1);
		query.replace(/(\w+)=([^&]+)/g,function(a,k,v){
			param[decodeURIComponent(k)] = decodeURIComponent(v);
		})
		url = url.substring(0,p);
	}
	
	if(/\.xhtml$/.test(url)){
		var jsonpath = path.join(root,url.replace(/\.xhtml$/,'.json'));
		fs.stat(jsonpath,function(error,stats){
			if(stats && stats.isFile()){
				var json = fs.readFileSync(jsonpath,'utf8');
				var model = new Function('return '+json)();
				model = toPromiseModel(model)
				engine.render(url,model,req,response);
			}else{
				engine.render(url,{},req,response);
			}
		})
		return true;
	}
	
},root).listen(process.env.APP_PORT || 2012);
console.log('lite test server is started: http://127.0.0.1:'+(process.env.APP_PORT || 2012));
