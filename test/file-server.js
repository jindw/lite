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
			response.write("<a href='"+files[i]+"'>"+files[i]+'</a><hr/>','utf8');
		}
		response.end();
	});
	
}
exports.createServer = createServer;




var s1="abab",s2="baba";//分别存储原string与目标string
var dp = [[]];

for (var i = Math.max(s1.length,s2.length)+1;i-->0; ){
	( dp[i]||(dp[i]=[]))[0] = i;
	dp[0][i] = i;
}
for (i=1; i<=s1.length; i++){
	for (j=1; j<=s2.length; j++){
		if (s1.charAt(i-1)==s2.charAt(j-1)){
			dp[i][j]=dp[i-1][j-1];//不需操作
		}else{
			dp[i][j]=Math.min(dp[i][j-1]+1,dp[i-1][j]+1,dp[i-1][j-1]+1);
		}
	}
}
alert(dp[s1.length][s2.length]);
