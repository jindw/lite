function LiteEngine(root){
	root = require('path').resolve(root || './')
	root = root.replace(/[\\\/]*$/,'/');
	this.root = root;
	this.templateMap = {};
	this.renderTask = {};
	var thiz = this;
	try{
		//throw new Error();
		this.compiler = require('child_process').fork(__dirname + '/process.js',['cpc',root]);
		this.compiler.on('message', function(result){
			thiz.onChange(result.path,result.code,result.config,result.prefix)
		}); 
		
	}catch(e){
		if(this.compiler == null){
			var thiz = this;
			var setupCompiler = require('./process.js').setupCompiler;
			var compiler = setupCompiler(root,function(result){
					var action = result.action;
					if(action == 'remove' || action == 'add' || action=='error'){
						thiz.onChange(result.path,result.code,result.config,result.prefix)
					}
				});
			this.compiler = {
				send:compiler
			}
			
		}
	}
}
LiteEngine.prototype.requestCompile = function(path){
	this.compiler.send(path);
}
LiteEngine.prototype.onChange = function(path,code,config,prefix) {
	if(code){
		var tpl = new Template(code,config,prefix||'');
		if(config.error == null){//发生错误的页面每次都需要重建？？
			this.templateMap[path] = tpl; 
		}
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
    	require('fs').readFile(require('path').resolve(this.root ,path.replace(/^[\\\/]/,'')), "binary", function(err, file) {    
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
			this.requestCompile(path);
		}
	}
}
function doRender(tpl,model,response){
    response.writeHead(200, {"Content-Type": tpl.contentType});   
	response.write(tpl.prefix,'utf-8');
	if(typeof model == 'function'){
		//TODO,需要引擎级别实现异步,这里知识兼容一下接口
		renderAsync(tpl,model,response)
	}else{
		try{
			var rtv = tpl.render(model);
		}catch(e){
			rtv = '<pre>'+require('util').inspect(e,true)+'\n\n'+(e.message +e.stack);
			//throw e;
		}
		response.end(rtv);
	}
}
function renderAsync(tpl,modelLoader,response){
	modelLoader(function(model){
		try{
			var rtv = tpl.render(model);
		}catch(e){
			rtv = require('util').inspect(e,true)+'\n\n'+(e.message +e.stack);
		}
		response.end(rtv);
	});
}
function Template(code,config,prefix){
 	//console.log(code)
 	
	try{
    	this.impl = eval('['+code+'][0]');
    }catch(e){
    	console.log(equire('util').inspect(e,true)+'\n\n'+(e.message +e.stack));
    	this.impl = function(){throw e;};
    }
    this.config = config;
    this.contentType = config.contentType;
    this.encoding = config.encoding;
    this.prefix = prefix ;
}
Template.prototype.render = function(context){
	try{
		return this.impl.call(null,context);
	}catch(e){
		console.log(this.impl+'')
		throw e;
	}
}

exports.LiteEngine = LiteEngine;
exports.Template = Template;