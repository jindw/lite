var LiteCompiler = require('./compiler').LiteCompiler;

var fs = require('fs');


var isChild = process.argv[2]=='cpc';//child-process-compiler

if(isChild){
	//{path:tplPath,action:'remove'}
	var root = process.argv[3].replace(/\/?$/,'/');
	console.log('ischild:',root);
	var compile = setupCompiler(root,function(cmd){
		process.send(cmd)
	});
	process.on('message', function(path){
		compile(path);
	});
}

/**
 * 
 */
function setupCompiler(root,callback){
	var templateCompiler= new LiteCompiler(root);
	/**
	 * template -> {resource1:true,resource2:true}
	 */
	var templateMap = {
	}
	/**
	 * 允许脏数据，发现脏数据要通过templateMap重新确定
	 * resource -> {template1:true,template2:true}
	 */
	var resourceMap = {
	}
	function addTemplateWatch(path,resources){
		var template = templateMap[path]={};
		for(var i = 0;i<resources.length;i++){
			var res = resources[i];
		 	template[res]=true;
		 	var resource= resourceMap[res];
		 	if(resource == null){
				//console.info('resource file:' ,res);
				resource = resourceMap[res] = {};
		 		addResourceWatch(res);
		 	}
		 	resource[path] = true;
		}
	}
	function addResourceWatch(resourcePath){
		fs.watch(require('path').join(root,resourcePath), function (event, filename) {
			//console.log('event is: ' + event,filename);
			for(var tplPath in resourceMap[resourcePath]){
				var tpl = templateMap[tplPath];
				if(tpl && tpl[resourcePath]){
					delete templateMap[tplPath];
					//console.debug('remove tpl evet:' ,tplPath);
					callback({path:tplPath,action:'remove'})
					//process.send({path:tplPath,action:'remove'})
					
				}
			}
		});
	}
	//process.on('message', function(path) {
	return (function(path){
		try{
			var result = templateCompiler.compile(path);
		    //console.log('child got message:', m.root);
		    var res = result.resources;
			//console.info('resource config:' ,res);
		    addTemplateWatch(path,res);
		    callback({path:path,action:'add',code:result.code,config:result.config,staticPrefix:result[3]})
		    //process.send({path:path,action:'add',code:result.code,config:result.config,staticPrefix:result[3]})
	    }catch(e){
	    	callback({path:path,action:'error',
	    		code:"function(){return '<pre>'+"+JSON.stringify(require('util').inspect(e,true)+
					'\n\n'+(e.message +e.stack))+"}",
	    		config:{'contentType':'text/html',encoding:'utf-8',error:e}
	    	})
	    }
	});
}
exports.setupCompiler = setupCompiler;