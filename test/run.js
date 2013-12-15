var LiteEngine = require('../lite-engine').LiteEngine;
var engine = new LiteEngine();
var path = require('path');
var fs = require('fs');
var http = require('http');



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
				engine.render(url,new Function('return '+json)(),response);
    		}else{
    			engine.render(url,{},response);
    		}
    	})
    	return true;
	}
	
},engine.root).listen(2012,'127.0.0.1');
console.log('lite test server is started: http://127.0.0.1:2012');
