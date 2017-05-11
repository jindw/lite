var  configRootKey = '-lite-engine-child-process-root';
exports.configRootKey = configRootKey
//node path -root root -filter path#name
var argv = process.argv;
var isChild = argv[2]==configRootKey;//child-process-compiler
if(isChild){
	//{path:tplPath,action:'remove'}
	var root = argv[3].replace(/\/?$/,'/');
	if(argv[4] == '-configurator' && argv[5]){
		var configurator = argv[5];
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

function setupCompiler(root,callback,configurator){
	var fs = require('fs');
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
		var file = require('path').join(root,resourcePath);
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
				var args = configurators.split('#');
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
		
		
		try{
			if(fs.existsSync(root+path)){
				var result = templateCompiler.compile(path);
			}else{
				result = {resources:['./'],
					litecode:[],
					jscode:'function(c,out){out.push("File Not Found:'+path+'");return out.join()}',
					config:{}
				};
			}
			
		    //console.log('child got message:', m.root);
		    var res = result.resources;
			//console.info('resource config:' ,res,result);
		    addTemplateWatch(path,res);
		    callback({path:path,action:'add',code:result.jscode,config:result.config})
		    //process.send({path:path,action:'add',code:result.code,config:result.config,prefix:result[3]})
	    }catch(e){
	    	console.log(e)
	    	//throw e;
	    	callback({path:path,action:'error',
	    		code:"function(){return '<pre>'+"+JSON.stringify(require('util').inspect(e,true)+
					'\n\n'+(e.message +e.stack))+"}",
	    		config:{contentType:'text/html',encoding:'utf-8',error:e}
	    	})
	    }
	});
}
exports.setupCompiler = setupCompiler;