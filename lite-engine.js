var Template = require('./template').Template
exports.getTemplateId = getTemplateId;
exports.LiteEngine = LiteEngine;
function LiteEngine(root,options){
	options = options || {}
	var path = require('path');
	var litecache = options.litecache;
	this.litecache = litecache && path.resolve(litecache);
	this.released = options.released;//root && root == options.litecache
	this.root = path.resolve(root || './').replace(/[\\\/]*$/,'/');
	this.templateMap = {};
	this.renderTask = {};
	if(!this.released){
		this.compiler = initCompiler(this, options.configurator);
	}
}
/**
 * @param engine
 * @param configurator: modulename#configuratorMethod(compiler)
 */
function initCompiler(engine,configurator){
	var root = engine.root;
	try{
		if(configurator instanceof Function){
			throw new Error();//function can config can not post to sub process!!
		}
		//throw new Error();
		var configRootKey = require('./process').configRootKey
		var args = [configRootKey,root]
		if(configurator){
			args.push('-configurator',configurator);
		}
		throw new Error();
		var compiler = require('child_process').fork(__dirname + '/process.js',args);
		compiler.on('message', function(result){
			//console.log(Object.keys(result))
			engine.onChange(result.path,result.code,result.config)
		}); 
		return compiler;
	}catch(e){
		var setupCompiler = require('./process.js').setupCompiler;
		var sender = setupCompiler(root,function(result){
				var action = result.action;
				if(action == 'remove' || action == 'add' || action=='error'){
					engine.onChange(result.path,result.code,result.config)
				}
			},configurator);
		return {
			send:sender
		}
		
	}
}
LiteEngine.prototype.requestCompile = function(path){
	if(this.released){
		var id = getTemplateId(path);
		var file = require('path').join(this.litecache,id+'.js');
		this.onChange(path,null,{liteFile:file})
	}else{
		this.compiler.send(path);
	}
}
LiteEngine.prototype.createTemplate  = function(code,config){
	return new Template(code,config);
}
LiteEngine.prototype.onChange = function(path,code,config) {
	//console.log(path,(code).length,config)
	if(config&&config.liteFile){
		try{
			var tpl = require(config.liteFile);
			tpl = this.createTemplate(tpl.template,tpl.config);
		}catch(e){
			console.error(e)
			code = "function(context,out){var err = 'template:"+config.liteFile+" not found!!';console.error(err);out.push(err);return out.join('')}";
			var tpl = this.createTemplate(code,config);
		}
		
	}else{
		var litecache = this.litecache;
		if(litecache){
			var id = getTemplateId(path);
			var file = require('path').join(this.litecache,id+'.js');
		}
		if(code && config.error == null){//发生错误的页面每次都需要重建？？
			if(file && this.updateLitecache(file,code,config)){
				try{
					var tpl = require(file);
					tpl = this.createTemplate(tpl.template || code,tpl.config);
				}catch(e){
					console.error(e);
					var tpl = this.createTemplate(code,config);
				}
			}else{
				var tpl = this.createTemplate(code,config);
			}
			this.templateMap[path] = tpl; 
		}else{//clear cache
			delete this.templateMap[path];
			if(file){
				delete require.cache[file];//require.resolve(file)]
			}
			//this.updateLitecache(id) //调试模式下每次都更新
			console.info('clear template cache:' ,path);
			return;
		}
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
}
LiteEngine.prototype.updateLitecache = function(file,code,config){
	var fs = require('fs');
	var dir = file.replace(/[^\\\/]+$/,'');
	if(fs.existsSync(dir)){
		if(code){
			var source = ['exports.template=',code,';\nexports.config = ',JSON.stringify(config)].join('')
			fs.writeFileSync(file,source);
			return file;
		}else{
			fs.existsSync(file) && fs.unlinkSync(file)
		}
	}else{
		console.error('litecache dir not exists:'+dir)
	}
}
LiteEngine.prototype.render=function(path,model,req,response){
	if(arguments.length == 3){
		response = req;req=null;
	}
    path = path.replace(/\\/g,'/').replace(/^\/?/,'/');
    var cookie = String(req && req.headers.cookie);
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
	if(!response.headersSent){
		//var statusCode = response.statusCode || 200;
		//var contentType = response.getHeader('content-type') || tpl.contentType
		//response.writeHead(statusCode, {"Content-Type": contentType});
		if(!response.statusCode ){
			response.statusCode  = 200;
		}
		if(response.getHeader('content-type') == null){
			response.setHeader('content-type', tpl.contentType||'text/html;charset=utf-8')
		}
	}
	//console.log(response.getHeader('content-type'),response.headersSent)
	try{
		tpl.render(model,response);
	}catch(e){
		var rtv = '<pre>'+require('util').inspect(e,true)+'\n\n'+(e.message +e.stack);
		response.end(rtv);
		throw e;
	}
}

function getTemplateId(path){
	////path.replace(/[^\w\_]/g,'_')
	return path.slice(1).replace(/[^\w\_]/g,'_');
}

