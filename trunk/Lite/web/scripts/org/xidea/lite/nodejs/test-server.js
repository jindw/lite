var vm = require('vm');
var fs = require('fs');
var Path = require('path');
var http = require('http');
function writeNotFound(filepath,response){
     response.writeHead(404, {"Content-Type": "text/plain"});    
     response.write("404 Not Found \n filepath:"+filepath);    
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
function startTestServer(templateEngine,host,port){
	var root = templateEngine.root;
	var actionList = []
	http.createServer(function (req, response) {
		var url = req.url;
		var i = actionList.length;
		while(i>0){
			var action = actionList[--i];
			var pattern = actionList[--i];
			if(pattern.match(url)){
				action(req,response);
				return;
			}
		}
		if(/\.xhtml$/.test(url)){
			var jsonpath = Path.join(root,url.replace(/\.xhtml$/,'.json'));
	    	fs.stat(jsonpath,function(error,stats){
	    		if(stats && stats.isFile()){
					var json = fs.readFileSync(jsonpath,'utf8');
					templateEngine.render(url,new Function('return '+json)(),response);
	    		}else{
	    			templateEngine.render(url,{},response);
	    		}
	    	})
		}else{
			response.writeHead(200, {'Content-Type': 'text/html;charset=utf-8'});
			var filepath = Path.join(root,url);
	    	fs.stat(filepath,function(error,stats){
	    		if(stats){
	    			if(stats.isDirectory()){
	    				writeDir(url,filepath,response);
	    			}else if(stats.isFile()){
	    				writeFile(filepath,response);
	    			}else{
	    				writeNotFound(filepath,response);   
	    			}
	    		}else{
	    			writeNotFound(filepath,response);   
	    		}
	    	})
		}
		
	}).listen(port||1985,host||'127.0.0.1');
	console.log('lite test server is started: http://'+(host||'127.0.0.1')+':' + (port||1985) );
	return {
		addAction:function(path,action){
			if('string' == typeof path){
				path = new RegExp('^'+path.replace(/[^\w]/,'\\$&')+'$');
			}
			actionList.push(path, action);
		}
	}
}
exports.startTestServer = startTestServer;