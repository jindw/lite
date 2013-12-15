var path = require('path');
var fs = require('fs');
var ParseConfig = require('./parse/config').ParseConfig;
var ParseContext = require('./parse/parse-context').ParseContext;
var JSTranslator = require('./parse/js-translator').JSTranslator;
function LiteCompiler(root){
	var root =String(path.resolve(root || './')).replace(/\\/g,'/');
	var config = path.resolve(root,'WEB-INF/lite.xml');
	if(path.existsSync(config)){
		this.config = new ParseConfig(root,config);
	}else{
		this.config = new ParseConfig(root,null);
	}
	console.info("LiteCompiler root:",root);
	
}
LiteCompiler.prototype.compile=function(path){
	var context = new ParseContext(this.config,path);
	var uri = context.createURI(path);
	context.parse(uri);
	//console.log(context.getConfigMap(path))
	
	var litecode = context.toList();
	var prefix = '';//extractStaticPrefix(litecode);
	if(litecode.length){
		var translator = new JSTranslator();//'.','/','-','!','%'
		//translator.liteImpl = 'liteImpl';//avoid inline jslib 
		var jscode = translator.translate(litecode);
	}else{//纯静态内容
		var jscode = "function(){}";
	}
	
	var res = context.getResources();
	var config = context.getConfigMap();
	var i = res.length;
	while(i--){
		res[i] = res[i].path
	}
	return {resources:res,code:jscode,config:config,statcPrefix:prefix};
}
exports.LiteCompiler = LiteCompiler;

