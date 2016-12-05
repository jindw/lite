var Template = require('./template').Template

function LiteEngine(root,options){
	root = require('path').resolve(root || './')
	root = root.replace(/[\\\/]*$/,'/');
	this.root = root;
	this.templateMap = {};
	this.renderTask = {};
	var thiz = this;
	/**
	 * 
	 * configurator: modulename#configuratorMethod(compiler)
	 */
	var configurator = options&&options.configurator;
	try{
		if(configurator instanceof Function){
			throw new Error();//function can config can not post to sub process!!
		}
		//throw new Error();
		var configRoot = require('./process').configRoot
		var args = [configRoot,root]
		if(configurator){
			args.push('-configurator',configurator);
		}
		this.compiler = require('child_process').fork(__dirname + '/process.js',args);
		this.compiler.on('message', function(result){
			//console.log(Object.keys(result))
			thiz.onChange(result.path,result.code,result.config)
		}); 
		
	}catch(e){
		if(this.compiler == null){
			var thiz = this;
			var setupCompiler = require('./process.js').setupCompiler;
			var compiler = setupCompiler(root,function(result){
					var action = result.action;
					if(action == 'remove' || action == 'add' || action=='error'){
						thiz.onChange(result.path,result.code,result.config)
					}
				},configurator);
			this.compiler = {
				send:compiler
			}
			
		}
	}
}
LiteEngine.prototype.requestCompile = function(path){
	this.compiler.send(path);
}
LiteEngine.prototype.onChange = function(path,code,config) {
	//console.log(path,(code).length,config)
	if(code){
		var tpl = new Template(code,config);
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
	try{
		tpl.render(model,response);
	}catch(e){
		var rtv = '<pre>'+require('util').inspect(e,true)+'\n\n'+(e.message +e.stack);
		response.end(rtv);
		throw e;
	}
}

exports.LiteEngine = LiteEngine;
