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
			//console.info('!!'+tp);
			var s = fs.readFileSync(tp,'utf8');
			//console.info('!#!'+s.substring(0,1000));
			return s;
		}
	}
}
var g = {$JSI:$JSI,console:console,require:require};
g.window = g;
var context = vm.createContext(g);
function evalSource(thiz,text,path){
	 var fn = vm.runInContext('(function(){'+text+'\n})',context,path);
	 return fn.call(thiz);
}
var boot = loadSource('boot.js');
vm.runInContext(boot,context,"classpath:///boot.js");
try{
	evalSource(g,"$import('org.xidea.lite:*')",'classpath:///$import');
	evalSource(g,"$import('org.xidea.lite.nodejs:DOMParser')",'classpath:///$import');
	evalSource(g,"$import('org.xidea.lite.nodejs:XPathEvaluator')",'classpath:///$import');
	evalSource(g,"$import('org.xidea.lite.impl.js:*')",'classpath:///$import');
	evalSource(g,"$import('org.xidea.lite.impl:*')",'classpath:///$import');
	evalSource(g,"$import('org.xidea.lite.nodejs:TemplateEngine')",'classpath:///$import');
	var TemplateEngine = evalSource(g,"return TemplateEngine;",'classpath:///$import');
	
}catch(e){
	console.log('error'+e);
	throw e;
}





var http = require('http');
var root = "D:\\workspace\\Lite2\\web\\";
var templateEngine = new TemplateEngine(root);
//** 测试 
templateEngine.render('/doc/guide/index.xhtml',{}, {write:function(arg){console.log(arg.substring(0,0));}});

//**/
http.createServer(function (req, response) {
	var url = req.url;
	response.writeHead(200, {'Content-Type': 'text/html;charset=utf-8'});
	if(/\.xhtml$/.test(url)){
		templateEngine.render(url,{},response);
		response.end();
	}else{
		url = url.substring(1);
		var filepath = Path.resolve(root,url);
		Path.exists(filepath, function(exists) {    
	        if(!exists) {    
	            response.writeHead(404, {"Content-Type": "text/plain"});    
	            response.write("404 Not Found\n");    
	            response.end();    
	            return;    
	        }    
	   		if(fs.statSync(filepath).isFile()){
		        fs.readFile(filepath, "binary", function(err, file) {    
		            if(err) {
		                response.writeHead(500, {"Content-Type": "text/plain"});    
		                response.write(err + "\n");    
		                response.end();    
		                return;    
		            }
		            var contentType = "text/html"
		            if(/.css$/.test(filepath)){
		            	contentType = "text/css";
		            }else  if(/.js$/.test(filepath)){
		            	contentType = "text/js";
		            }
		            response.writeHead(500, {"Content-Type": contentType+';charset=utf-8'});   
		            response.writeHead(200);    
		            response.write(file, "binary");    
		            response.end();    
		        });
	   		}else{
	   			var childs = fs.readdir(filepath, function(err, files) {  
	   				for(var i=0;i<files.length;i++){
						response.write("<a href='"+files[i]+"'>"+files[i]+'</a><hr/>','utf8');
					}
					response.end();
	   			});
		
	   		}    
	    });   
		
		
		
		
	}
	
}).listen(1337, "127.0.0.1");
console.log('Server running at http://127.0.0.1:1337/');
// */