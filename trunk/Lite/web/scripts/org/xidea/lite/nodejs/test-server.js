var vm = require('vm');
var fs = require('fs');
var Path = require('path');
var http = require('http');
function writeNotFound(filepath,response){
     response.writeHead(404, {"Content-Type": "text/plain"});    
     response.write("404 Not Found \n filepath:"+filepath);    
     response.end();    
}
function writeFile(filepath,response,prepostfix){
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
        	contentType = "text/javascript";
        }
        response.writeHead(200, {"Content-Type": contentType+';charset=utf8'}); 
        if(prepostfix){
         	response.write(prepostfix[0]);
         	response.write(file, "binary"); 
         	response.write(prepostfix[1]); 
        }else{
        	response.write(file, "binary");  
        }
        var wait = filepath.replace(/^.*?(?:w(\d+)\.js)?$/g,'$1');
        if(wait){
        	console.log('wait:',wait)
        	setTimeout(function(){
        		response.end()
        		console.log('wait end!')
        	},1*wait);
        	return;
        }
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
function evalCode(){
	try{
		return eval('('+arguments[0]+')');
	}catch(e){
		console.warn(e,arguments[0]);
	}
}
function startTestServer(templateEngine,host,port){
	var root = templateEngine.root;
	var actionList = []
	var server = {
		addAction:function(path,action){
			if('string' == typeof path){
				path = new RegExp('^'+path.replace(/[^\w]/,'\\$&')+'$');
			}
			actionList.push(path, action);
			return this;
		},
		render:function(path,data,response){
			return templateEngine.render(path,data || {},response);
		}
	}
	var actionList = []
	http.createServer(function (req, response) {
		var url = req.url;
		var p = url.indexOf('?');
		var query = p>0?url.substring(p+1):'';
		var i = actionList.length;
		url = p>0?url.substring(0,p):url;
		while(i>0){
			var action = actionList[--i];
			var pattern = actionList[--i];
			if(pattern.match(url)){
				action.call(server,req,response);
				return;
			}
		}
		if('/runner' == url && query){
			var params = {};
			query.replace(/(\w+)=([^&]+)/g,function(a,k,v){
				params[k] = v.replace(/(?:%[\da-fA-F]{2})+/g ,function(v){
					try{
						return decodeURIComponent(v);
					}catch(e){
						console.error(e,v)
						return v;
					}
				})
			})
			var code = params.code;
			var model = params.model;
			var callback = params.callback;
			var code = evalCode(code);
			var tpl = new (require('./lite-engine').Template)(code[0],code[1],code[2]);
			templateEngine.templateMap['/'] =tpl;
			if(callback){
				var responseMock = {
					data : [],
					writeHead:Function.prototype,
					write:function(){this.data.push(arguments[0])},
					end:Function.prototype
				}
				response.writeHead(200, {'Content-Type': 'text/javascript;charset=utf-8'});
				templateEngine.render('/',evalCode(model),responseMock);
				var result = responseMock.data.join('');
				response.write(callback+'('+JSON.stringify(result)+')')
				response.end();
			}else{
				templateEngine.render('/',evalCode(model),response);
			}
		}else if(/\.xhtml$/.test(url)){
			var jsonpath = Path.join(root,url.replace(/\.xhtml$/,'.json'));
	    	fs.stat(jsonpath,function(error,stats){
	    		if(stats && stats.isFile()){
					var json = fs.readFileSync(jsonpath,'utf8');
					templateEngine.render(url,new Function('return '+json)(),response);
	    		}else{
	    			templateEngine.render(url,{},response);
	    		}
	    	})
		}else if('/exit' == url){
			response.write("server is closing!!");
			response.end();
			process.exit(0);
		}else{
			response.writeHead(200, {'Content-Type': 'text/html;charset=utf-8'});
			var filepath = Path.join(root,url);
	    	fs.stat(filepath,function(error,stats){
	    		if(stats){
	    			if(stats.isDirectory()){
	    				return writeDir(url,filepath,response);
	    			}else if(stats.isFile()){
	    				return writeFile(filepath,response);
	    			}
	    		}
	    		var jsipreload = /__preload__.js$/.test(url);
	    		if(url == '/scripts/boot.js' || jsipreload){//for jsi debug
	    			if(jsipreload){
	    				url = url.replace(/__preload__\.js$/,'.js');
	    				filepath = filepath.replace(/__preload__\.js$/,'.js');
	    			}else{
	    				filepath = Path.join(root,'scripts/org/xidea/lite/nodejs/boot4node.js');
	    			}
	    			fs.stat(filepath,function(error,stats){
	    				if(stats && stats.isFile()){
	    					if(jsipreload){
	    						var pkg = url.substring(url.indexOf('/',2)+1,
	    							url.lastIndexOf('/')
	    						).replace(/[\/]/g,'.');
	    						var file = url.substring(url.lastIndexOf('/')+1);
	    						writeFile(filepath,response,[
	    							"$JSI.preload('"+pkg+"','"+file+"',function(){"+
	    							(file?"eval(this.varText);":'')
	    							,"\n})"]);
	    					}else{
	    						writeFile(filepath,response);
	    					}
	    				}else{
	    					writeNotFound(filepath,response); 
	    				}
	    			});
	    		}else{
	    			writeNotFound(filepath,response);  
	    		}
	    	})
		}
		
	}).listen(port||1985,host||'127.0.0.1');
	console.log('lite test server is started: http://'+(host||'127.0.0.1')+':' + (port||1985) );
	return server;
}
exports.startTestServer = startTestServer;