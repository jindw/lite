function LiteEngine(root){
	root = require('path').resolve(root || './')
	root = root.replace(/[\\\/]*$/,'/');
	this.root = root;
	this.templateMap = {};
	this.renderTask = {};
	var thiz = this;
	try{
		this.compiler = require('child_process').fork(__dirname + '/process.js',[root]);
		this.compiler.on('message', function(result){
			thiz.onChange(result.path,result.code,result.config)
		}); 
	}catch(e){
		if(this.compiler == null){
			//TODO:...
		}
	}
}
LiteEngine.prototype.onChange = function(path,code,config) {
	if(code){
		var tpl = new Template(code,config,config.staticPrefix||'');
		this.templateMap[path] = tpl; 
		var task = this.renderTask[path];
		if(task){
			delete this.renderTask[path];
			for(var i=0;i<task.length;i++){
				var args = task[i];
				args[0] = tpl;
				doRender.apply(null,args)
			}
		}
	}else{//clear cache
		delete this.templateMap[path];
		console.info('clear template cache:' ,path);
	}
}
LiteEngine.prototype.render=function(path,model,req,response){
    var cookie = String(req.headers.cookie);
    var debug = cookie.replace(/(?:^|&[\s\S]*;\s*)LITE_DEBUG=(\w+)[\s\S]*$/,'$1');
    debug = debug == cookie?false:debug;
	if(debug=='model'){
    	response.end(JSON.stringify(model));
    }else if(debug=='source'){
    	require('fs').readFile(filepath, "binary", function(err, file) {    
        	if(err) {
            	response.writeHead(404, {"Content-Type": "text/plain"});   
            	response.end(err + "\n");    
        	}else{
        		response.writeHead(200, {"Content-Type": 'text/plain;charset=utf8'}); 
         		response.end(file, "binary"); 
        	}    
    	});
   	}else{
		var tpl = this.templateMap[path];
		if(tpl){
			doRender(tpl,model,response);
		}else{
			(this.renderTask[path] || (this.renderTask[path] =[])).push([path,model,response]);
			this.compiler.send({path:path });
		}
	}
}
function doRender(tpl,model,response){
    response.writeHead(200, {"Content-Type": tpl.contentType});   
	response.write(tpl.staticPrefix,'utf-8');
	if(typeof model == 'function'){
		//TODO,需要引擎级别实现异步,这里知识兼容一下接口
		renderAsync(tpl,callback,response)
	}else{
		var rtv = tpl.render(model);
		response.end(rtv);
	}
}
function renderAsync(tpl,callback,response){
	callback(function(model){
		if(tpl){
			var rtv = tpl.render(model);
			response.end(rtv);
		}
	});
}
function Template(code,config,staticPrefix){
 	//console.log(code)
	try{
    this.impl = eval('['+code+'][0]');
    }catch(e){
    throw e;
    }
    this.config = config;
    this.contentType = config.contentType;
    this.encoding = config.encoding;
    this.staticPrefix = staticPrefix ;
}
Template.prototype.render = function(context){
	return this.impl.call(null,context);
}

exports.LiteEngine = LiteEngine;
exports.Template = Template;