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
				render.apply(null,args)
			}
		}
	}else{//clear cache
		delete this.templateMap[path];
		console.info('clear template cache:' ,path);
	}
}
LiteEngine.prototype.render=function(path,data,response){
	var tpl = this.templateMap[path];
	if(tpl){
		render(tpl,data,response);
	}else{
		(this.renderTask[path] || (this.renderTask[path] =[])).push([path,data,response]);
		this.compiler.send({path:path });
	}
}
function render(tpl,data,response){
    response.writeHead(200, {"Content-Type": tpl.contentType});   
	response.write(tpl.staticPrefix,'utf-8');
	if(typeof data == 'function'){
		//TODO,需要引擎级别实现异步,这里知识兼容一下接口
		renderSync(tpl,callback,response)
	}else{
		var rtv = tpl.render(data);
		response.write(rtv,'utf-8');
		response.end();
	}
}
function renderSync(tpl,callback,response){
	var rtv = data(function(data){
		if(tpl){
			var rtv = tpl.render(data);
			response.write(rtv,'utf-8');
			response.end();
		}
	});
	if(rtv){
		var rtv = tpl.render(rtv);
		response.write(rtv,'utf-8');
		response.end();
		tpl = null;
	}
}
function Template(code,config,staticPrefix){
 console.log(code)
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