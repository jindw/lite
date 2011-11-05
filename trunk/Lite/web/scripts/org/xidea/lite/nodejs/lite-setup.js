var LiteCompiler = require('./lite-compiler').LiteCompiler;
//var LiteCompiler = require('../../../../../../build/dest/nodejs').LiteCompiler;

!function(){
	var FS = require('fs');
	var Path = require('path');
	var root = process.argv[2].replace(/\/?$/,'/');
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
//				console.info('resource file:' ,res);
				resource = resourceMap[res] = {};
		 		addResourceWatch(res);
		 	}
		 	resource[path] = true;
		}
	}
	function addResourceWatch(resourcePath){
		FS.watch(Path.join(root,resourcePath), function (event, filename) {
//			console.log('event is: ' + event,filename);
			for(var tplPath in resourceMap[resourcePath]){
				var tpl = templateMap[tplPath];
				if(tpl && tpl[resourcePath]){
					delete templateMap[tplPath];
//					console.debug('remove tpl evet:' ,tplPath);
					process.send({path:tplPath,action:'remove'})
				}
			}
		});
	}
	process.on('message', function(config) {
		var path = config.path;
		var result = templateCompiler.compile(path);
	    //console.log('child got message:', m.root);
	    var res = result[0];
//		console.info('resource config:' ,res);
	    addTemplateWatch(path,res);
	    process.send({path:path,action:'add',code:result[1],featureMap:result[2],staticPrefix:result[3]})
	});
}();