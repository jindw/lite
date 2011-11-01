if(process.argv[2] == 'debug'){
	console.info('debug model');
	var LiteEngine = require('../scripts/org/xidea/lite/nodejs/lite-engine').LiteEngine
}else{
	var LiteEngine = require('../lite-engine').LiteEngine;
}
var vm = require('vm');
var fs = require('fs');
var Path = require('path');
var root =Path.join(__dirname,'../');
var templateEngine = new LiteEngine(root);
var http = require('http');
function writeNotFound(response){
     response.writeHead(404, {"Content-Type": "text/plain"});    
     response.write("404 Not Found\n");    
     response.end();    
}
function writeFile(filepath,response){
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
        response.writeHead(200, {"Content-Type": contentType+';charset=utf-8'});   
        response.write(file, "binary");    
        response.end();    
    });
}
function writeDir(url,filepath,response){
	if(/\/$/.test(url)){
		fs.readdir(filepath, function(err, files) {  
			for(var i=0;i<files.length;i++){
				response.write("<a href='"+files[i]+"'>"+files[i]+'</a><hr/>','utf8');
			}
			response.end();
		});
	}else{
		response.writeHead(301, {"Location" : url+'/'});    
	            	response.end();    
	}
}
http.createServer(function (req, response) {
	var url = req.url;
	response.writeHead(200, {'Content-Type': 'text/html;charset=utf-8'});
	if(/\.xhtml$/.test(url)){
		templateEngine.render(url,{},response);
	}else{
		var filepath = Path.join(root,url);
    	fs.stat(filepath,function(error,stats){
    		if(stats){
    			if(stats.isDirectory()){
    				writeDir(url,filepath,response);
    			}else if(stats.isFile()){
    				writeFile(filepath,response);
    			}else{
    				writeNotFound(response);   
    			}
    		}else{
    			writeNotFound(response);   
    		}
    	})
	}
	
}).listen(1981, "127.0.0.1");
console.log('Server running at http://127.0.0.1:1981/');
// */