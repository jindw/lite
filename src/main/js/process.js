var  configRootKey = '-lite-engine-child-process-root';
var pathModule = require('path');
var fs = require('fs');
exports.build = buildCompiler
tryInitProcess();
function tryInitProcess(){
	//node path -root root -filter path#name
	var argv = process.argv;
	var index = argv.indexOf(configRootKey);
	if(index<0){
		return;
	}
	//var isChild = argv[2]==configRootKey;//child-process-compiler
	//{path:tplPath,action:'remove'}
	var root = argv[index+1].replace(/\/?$/,'/');
	if(argv[index+2] == '-configurator' && argv[index+3]){
		var configurator = argv[index+3];
	}
	//console.log('ischild:',root);
	var compile = setupCompiler(root,function(cmd){
		//console.log('compile:',cmd)
		process.send(cmd)
	},configurator);
	process.on('message', function(path){
		compile(path);
	});
}
/**
 * @param engine
 * @param configurator: modulename#configuratorMethod(compiler)
 */
function buildCompiler(engine,onAction,configurator){
	var root = engine.root;
	try{//try build compiler on new thread!
		if(configurator instanceof Function){
			throw new Error();//function can config can not post to sub process!!
		}
		throw new Error();
		var args = [configRootKey,root]
		if(configurator){
			args.push('-configurator',configurator);
		}
		var compiler = require('child_process').fork(__dirname + '/process.js',args);
		compiler.on('message',onAction); 
		return compiler;
	}catch(e){
		var setupCompiler = require('./process.js').setupCompiler;
		var sender = setupCompiler(root,onAction,configurator);
		return {
			send:sender
		}
	}
}
function setupCompiler(root,callback,configurator){
	var LiteCompiler = require('./compiler').LiteCompiler;
	var templateCompiler= new LiteCompiler(root);
	
	/**
	 * { 
	 * 		templatepath : {
	 * 			resource1:true,
	 * 			resource2:true
	 * 		}
	 * }
	 */
	var templateResourcesMap = {
	}
	/**
	 * 允许脏数据，发现脏数据要通过templateMap重新确定
	 * { resourcePath -> {template1:watcher,template2:watcher}  }
	 */
	var resourceWatcherMap = {}
	
	
	function addTemplateWatch(path,resources){
		var templateInfo = templateResourcesMap[path]={};
		//console.log('resource：',resources)
		for(var i = 0;i<resources.length;i++){
			var resPath = resources[i];
		 	templateInfo[resPath]=true;
		 	addResourceWatch(resPath,path);
		 	//tplWatcherMap[path] = true;
		}
	}
	function addResourceWatch(resourcePath,tplPath){
		if(resourcePath.match(/[\\\/]$/)){//ignore dir
			return;
		}
		var file = pathModule.join(root,resourcePath);
		var options = { persistent: true, recursive: false };
		
		var tplWatcherMap= resourceWatcherMap[resourcePath] || (resourceWatcherMap[resourcePath] = {});
		var oldWatcher = tplWatcherMap[tplPath]
		if(oldWatcher){
			oldWatcher.close();
		}
		
		//console.log('add watcher:',file,tplPath)
		tplWatcherMap[tplPath] = fs.watch(file, options,function (event, filename) {
			//console.log('event is: ' + event,filename,tplPath,resourcePath);
			callback({path:tplPath,action:'remove'})
			/*
			var tplWatcherMap = resourceWatcherMap[resourcePath];
			for(var tplPath in tplWatcherMap){
				var tpl = templateResourcesMap[tplPath];
				if(tpl && tpl[resourcePath]){
					delete templateResourcesMap[tplPath];
					//console.debug('remove tpl evet:' ,tplPath);
					callback({path:tplPath,action:'remove'})
					//process.send({path:tplPath,action:'remove'})
					
				}
			}
			*/
		});
	}
	
	templateCompiler.waitPromise = true
	if(configurator){
		//console.log('filter:',configurator)
		try{
			if('string' == typeof configurator ){
				var args = configurator.split('#');
				var path = args[0];
				var name = args[1];
				var configurator = require(path)[name];
				configurator(templateCompiler)
			}else if(configurator instanceof Function){
				configurator(templateCompiler)
			}
		}catch(e){
			console.error('filter init error:'+e);
		}
	}
	//process.on('message', function(path) {
	return (function(path){
		var tplFile = pathModule.join(root,path);
		try{
			if(fs.existsSync(tplFile)){
				var tplExist = true;
				var result = templateCompiler.compile(path);
				
		    	addTemplateWatch(path,result.resources);
			}else{
		    	addTemplateWatch(path,[tplFile.replace(/[^\\\/]+$/,'')]);
				throw new Error("\nFile Not Found:"+path);
			}
		    var message = {
		    	path:path,
		    	action:'update',
		    	code:result.code,
		    	config:result.config};
		    //process.send({path:path,action:'add',code:result.code,config:result.config,prefix:result[3]})
	    }catch(e){
	    	console.error(e)
			var error = [path+" compile error",e.message,e.stack].join('\n');
			var jscode = 'function(c,out){out.push("<pre>",'+JSON.stringify(error)+',"</pre>");return out.join()}'
	    	var message = {
	    		path:path,
	    		action:'error',
	    		code:jscode,
	    		config:{statusCode:'500',contentType:'text/html;encoding=utf-8'}
	    	}
	    }
		callback(message)
	});
}
exports.setupCompiler = setupCompiler;