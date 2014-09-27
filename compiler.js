var ParseConfig = require('./parse/config').ParseConfig;
var ParseContext = require('./parse/parse-context').ParseContext;
var JSTranslator = require('./parse/js-translator').JSTranslator;
var loadLiteXML = require('./parse/xml').loadLiteXML;
function LiteCompiler(root,config){
	var path = require('path');
	var root =String(path.resolve(root || './')).replace(/\\/g,'/');
	var config = config || path.resolve(root,'lite.xml');
	if(require('fs').existsSync(config)){
		var dom = loadLiteXML(config);
		//console.log(dom+'')
		this.config = new ParseConfig(root,dom);
	}else{
		var config = path.resolve(root,'WEB-INF/lite.xml');
		if(require('fs').existsSync(config)){
			var dom = loadLiteXML(config);
			//console.log(dom+'')
			this.config = new ParseConfig(root,dom);
		}else{
			this.config = new ParseConfig(root,null);
		}
	}
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
	return {resources:res,code:jscode,config:config,prefix:prefix};
}
exports.LiteCompiler = LiteCompiler;

