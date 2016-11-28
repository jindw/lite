var ParseConfig = require('./parse/config').ParseConfig;
var ParseContext = require('./parse/parse-context').ParseContext;
var JSTranslator = require('./parse/js-translator').JSTranslator;
var loadLiteXML = require('./parse/xml').loadLiteXML;
function LiteCompiler(root,options){
	options = options || {};
	var path = require('path');
	var root =String(path.resolve(root || './')).replace(/\\/g,'/');
	var compileDir  = options.compileDir;
	var configPath = options.configPath || path.resolve(root,'lite.xml');
	
	if(require('fs').existsSync(configPath)){
		var dom = loadLiteXML(configPath);
		//console.log(dom+'')
		this.config = new ParseConfig(root,dom);
	}else if(!options.configPath){
		configPath = path.resolve(root,'WEB-INF/lite.xml');
		if(require('fs').existsSync(configPath)){
			var dom = loadLiteXML(configPath);
			//console.log(dom+'')
			this.config = new ParseConfig(root,dom);
		}
	}
	this.config = this.config || new ParseConfig(root,null);
	
	this.translator = new JSTranslator({
		//liteImpl:liteImpl,
		waitPromise:true
	});
	console.info("LiteCompiler root:",root);
	
}
LiteCompiler.prototype.createParseContext = function(path){
	return new ParseContext(this.config,path);
}
LiteCompiler.prototype.compile=function(path){
	var context = this.createParseContext(path);
	var uri = context.createURI(path);
	context.parse(uri);
	//console.log("&&&",path)
	//console.log(context.getConfigMap(path))
	
	var litecode = context.toList();
	if(litecode.length){
		//translator.liteImpl = 'liteImpl';//avoid inline jslib 
		var functionName = path.replace(/[^\w\_]/g,'_')
		var jscode = this.translator.translate(litecode,{name:functionName});//,params:null,defaults:null
	}else{//纯静态内容
		var jscode = "function(){}";
	}
	
	var res = context.getResources();
	var config = context.getConfigMap();
	var i = res.length;
	while(i--){
		res[i] = res[i].path
	}
	return {resources:res,litecode:litecode,jscode:jscode,config:config};
}
exports.LiteCompiler = LiteCompiler;

