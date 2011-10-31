/*
require("D:\\workspace\\Lite2\\web\\nodejs\\test.js")
*/

var vm = require('vm');
var fs = require('fs');
var Path = require('path');

var root =Path.join(__dirname,'..');
var TemplateEngine = require('../scripts/org/xidea/lite/nodejs/template-engine.js').TemplateEngine
var templateEngine = new TemplateEngine(root);

var http = require('http');
//** 测试 
//templateEngine.render('/doc/guide/index.xhtml',{}, {write:function(arg){console.log(arg.substring(0,0));}});

//**/
http.createServer(function (req, response) {
	var url = req.url;
	response.writeHead(200, {'Content-Type': 'text/html;charset=utf-8'});
	if(/\.xhtml$/.test(url)){
		templateEngine.render(url,{},response);
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
	   			if(!url || /\/$/.test(url)){
		   			fs.readdir(filepath, function(err, files) {  
		   				for(var i=0;i<files.length;i++){
							response.write("<a href='"+files[i]+"'>"+files[i]+'</a><hr/>','utf8');
						}
						response.end();
		   			});
	   			}else{
	   				console.log("/"+url+'/')
	   				response.writeHead(301, {"Location" : "/"+url+'/'});    
	            	response.end();    
	   			}
	   		}    
	    });   
	}
	
}).listen(1337, "127.0.0.1");
console.log('Server running at http://127.0.0.1:1337/');
// */