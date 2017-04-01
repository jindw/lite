var fs = require('fs');
var path = require('path');
var http = require('http');

function createServer(callback,root){
	root = require('path').resolve(root || './')
	return http.createServer(function (req, response) {
		if(!callback(req,response,root)){
			var url = req.url.replace(/[?#].*$/,'');
			writeFile(root,url,response)
		}
	});
}
function writeFile(root,url,response){
	var filepath = path.join(root,url);
	fs.stat(filepath,function(error,stats){
		if(stats){
	    	if(stats.isDirectory()){
	    		if(/[\\\/]$/.test(url)){
	    			writeIndex(filepath,response);
	    		}else{
	    			response.writeHead(301, {"Location" : url+'/'});    
	            	response.end();    
	    		}
	    	}else{
	    		return writeContent(filepath,response);
	    	}
	    }else{
	    	response.writeHead(404, {"Content-Type": "text/plain"});    
     		response.end("404 Not Found \n filepath:"+filepath);    
	    }
	});
}
function writeContent(filepath,response,prefix,postfix){
	fs.readFile(filepath, "binary", function(err, file) {    
        if(err) {
            response.writeHead(500, {"Content-Type": "text/plain"});   
            response.end(err + "\n");    
            return;    
        }
        var contentType = "text/html"
        if(/.css$/.test(filepath)){
        	contentType = "text/css";
        }else  if(/.js$/.test(filepath)){
        	contentType = "text/javascript";
        }
        response.writeHead(200, {"Content-Type": contentType+';charset=utf8'}); 
        if(prefix||postfix){
         	prefix && response.write(prefix);
         	response.write(file, "binary"); 
         	postfix && response.write(postfix); 
        	response.end();
        }else{
        	response.end(file, "binary");  
        }    
    });
}
function writeIndex(filepath,response){
	fs.readdir(filepath, function(err, files) { 
		files.sort(); 
		for(var i=0;i<files.length;i++){
			if(files[i].charAt() != '.'){
				response.write("<a href='"+files[i]+"'>"+files[i]+'</a><hr/>','utf8');
			}
		}
		response.end();
	});
	
}
module.exports = createServer;