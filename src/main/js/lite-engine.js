var Template = require('./template').Template
var fs = require('fs');
exports.getTemplateId = getTemplateId;
exports.LiteEngine = LiteEngine;
function LiteEngine(root,options){
	options = options || {}
	var path = require('path');
	var litecache = options.litecache;
	if(litecache && !fs.existsSync(litecache)){
		console.error('litecache not found!!',litecache)
		litecache = null;
	}
	this.litecache = litecache && path.resolve(litecache);
	this.released = options.released;//root && root == options.litecache
	this.root = path.resolve(root || './').replace(/[\\\/]*$/,'/');
	this.templateMap = {};
	this.renderTask = {};
	if(!this.released){
		var engine = this;
		function onAction(result){
			var action = result.action;
			var path = result.path;
			var code = result.code;
			var config = result.config;
			if(action == 'update' || action=='error'){
				onCompiled(engine,path,code,config)
			}else if(action == 'remove'){
				cleanTemplate(engine,path);
			}
		}
		this.compiler = require('./process').build(this, onAction,options.configurator);
	}
}

LiteEngine.prototype.createTemplate  = function(path,code,config){
	config.path = path;
	return new Template(code,config);
}
LiteEngine.prototype.render=function(path,model,request,response){
	try{
	    path = path.replace(/\\/g,'/').replace(/^\/?/,'/');
		if(!response){
			response = request;request=null;
		}
		var engine = this;
	    var debug = tryDebug(engine.root,path,model,request,response);
	    if(debug){
	    	return Promise.resolve(debug)
	    }
		var tpl = engine.templateMap[path];
		if(tpl){
			return doRender(tpl,model,response);
		}else{
			var renderTask = engine.renderTask;
	    	return new Promise(function(resolve,reject){
				(renderTask[path] || (renderTask[path] =[])).push(
					//[path,model,response]
					function(tpl){
						doRender(tpl,model,response).then(resolve,reject);
					}
				);
				requestCompile(engine,path);
			})
			
	    }
	}catch(e){
		return Promise.reject(e)
	}
}
function tryDebug(root,path,model,request,response){
    var cookie = String(request && request.headers &&request.headers.cookie);
    var debug = cookie.replace(/(?:^|&[\s\S]*;\s*)LITE_DEBUG=(\w+)[\s\S]*$/,'$1');
    debug = debug == cookie?false:debug;
	if(debug=='model'){
		var result = JSON.stringify(model);
    	response.end(result);
    	return result.length;
//    }else if(debug=='source'){
//    	var fs = require('fs');
//    	var pathModule = require('path')
//    	var file= pathModule.resolve(root ,path.replace(/^[\\\/]/,''));
//    	fs.readFile(file, "binary", function(err, file) {    
//        	if(err) {
//            	response.writeHead(404, {"Content-Type": "text/plain"});   
//            	response.end(err + "\n");    
//        	}else{
//        		response.writeHead(200, {"Content-Type": 'text/plain;charset=utf8'}); 
//         		response.end(file, "binary"); 
//        	}
//    	});
   	}
}
function doRender(tpl,model,response){
	if(response.headersSent ===  false){
		var config = tpl.config||{};
		//console.log(response.statusCode )
		if(response.statusCode == 200 && config.statusCode){
			response.statusCode  = config.statusCode;
		}
		if(response.getHeader('content-type') == null){
			response.setHeader('content-type', config.contentType||'text/html;charset=utf-8')
		}
	}
	try{
		return tpl.render(model,response);
	}catch(e){
		var rtv = '<pre>'+require('util').inspect(e,true)+'\n\n'+(e.message +e.stack);
		response.end(rtv);
		return Promise.reject(e);
	}
}

function loadTplFromFile(engine,path){
	try{
		var compiledFile = getTemplateModuleFile(engine,path)
		var tplModule = require(compiledFile);
		return engine.createTemplate(path,tplModule.template,tplModule.config);
	}catch(e){
		console.error(e);
		var code = "function(context,out){" +
				"var err = "+JSON.stringify(['\ntemplate:'+path+' load error!!',e.message,e.error,'\n'].join('\n'))
				"out.push('<pre>',err,'</pre>');" +
				"return out.join('')" +
			"}";
		return engine.createTemplate(path,code,{statusCode:500,contentType:'text/html;charset=utf-8'});
	}
}

function cleanTemplate(engine,path){
	var saveFile = getTemplateModuleFile(engine,path)
	//clear cache
	delete engine.templateMap[path];
	if(saveFile){
		delete require.cache[saveFile];//require.resolve(file)]
	}
	//updateLitecache(id) //调试模式下每次都更新
	console.info('clear template cache:' ,path);
}
function onCompiled(engine,path,code,config){
	var tpl = saveAndLoad(engine,path,code,config);
	onTemplateLoaded(engine,path,tpl)
}
function saveAndLoad(engine,path,code,config){
	var litecache = engine.litecache;
	if(litecache){
		saveDeleteLitecacheSync(engine,path,code,config);
		var tpl = loadTplFromFile(engine,path);
	}else{
		var tpl = engine.createTemplate(path,code,config);
	}
	return tpl;
}

function onTemplateLoaded(engine,path,tpl){
	engine.templateMap[path] = tpl; 
	var task = engine.renderTask[path];
	if(task){
		delete engine.renderTask[path];
		for(var i=0;i<task.length;i++){
			var args = task[i];
			if(args instanceof Function){
				args(tpl);
				continue;
			}else{
				args[0] = tpl;
				doRender.apply(null,args)['catch'](String)
			}
		}
	}
}
function requestCompile(engine,path){
	if(engine.released){
		var tpl = loadTplFromFile(engine,path);
		onTemplateLoaded(engine,path,tpl)
	}else{
		engine.compiler.send(path);
	}
}
function saveDeleteLitecacheSync(engine,path,code, config){
	var compiledFile = getTemplateModuleFile(engine,path)
	var dir = compiledFile.replace(/[^\\\/]+$/,'');
	if(fs.existsSync(dir)){
		if(code){
			config.path = path;
			var source = ['exports.template=',code,';\nexports.config = ',JSON.stringify(config)].join('')
			fs.writeFileSync(compiledFile,source);
			return compiledFile;
		}else{
			fs.existsSync(compiledFile) && fs.unlinkSync(compiledFile)
		}
	}else{
		console.error('litecache dir not exists:'+dir)
	}
}
function getTemplateModuleFile(engine,path){
	if(engine.litecache){
		var id = getTemplateId(path);
		return require('path').join(engine.litecache,id+'.js');
	}
}
function getTemplateId(path){
	////path.replace(/[^\w\_]/g,'_')
	return path.slice(1).replace(/[^\w\_]/g,'_');
}

