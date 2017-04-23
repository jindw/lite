var ParseConfig = require('./parse/config').ParseConfig;
var ParseContext = require('./parse/parse-context').ParseContext;
var JSTranslator = require('./parse/js-translator').JSTranslator;
var loadLiteXML = require('./parse/xml').loadLiteXML;
var buildURIMatcher = require('./parse/resource').buildURIMatcher
exports.getTemplateId = getTemplateId
exports.LiteCompiler = LiteCompiler;
function getTemplateId(path){
	////path.replace(/[^\w\_]/g,'_')
	return path.slice(1).replace(/[^\w\_]/g,'_');
}

exports.execute = function(args){
	var options = {};
	var key = '';
	for(var i=2;i<args.length;i++){
		var arg = args[i];
		if(arg.charAt() == '-'){
			key = arg.substr(1)
			options[key] = [];
		}else{
			options[key].push(arg)
		}
	}
	//console.log(options)
	var root = options.root && options.root[0];
	var output = options.output && options.output[0];
	compile(root,output,options.translator,
		options.includes,options.excludes)
}

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
	var waitPromise = 'waitPromise' in options?options.waitPromise:true;
	this.translator = new JSTranslator({
		//liteImpl:liteImpl,
		waitPromise:waitPromise
	});
	console.info("LiteCompiler root:",root);
	
}
LiteCompiler.prototype.createParseContext = function(path){
	return new ParseContext(this.config,path);
}
LiteCompiler.prototype.compile=function(path){
	var root = this.config.root;
	var context = this.createParseContext(path);
	var uri = context.createURI(path);
	
	context.parse(uri);
	//console.log("&&&",path)
	//console.log(context.getConfigMap(path))
	
	var litecode = context.toList();
	if(litecode.length){
		//translator.liteImpl = 'liteImpl';//avoid inline jslib 
		var functionName = getTemplateId(path);
		var jscode = this.translator && this.translator.translate(litecode,{name:functionName});//,params:null,defaults:null
	}else{//纯静态内容
		var jscode = "function(){}";
	}
	
	var res = context.getResources();
	var config = context.getConfigMap();
	var i = res.length;
	while(i--){
		res[i] = res[i].path
	}
	return {resources:res,litecode:litecode,code:jscode,config:config,path:path};
};

//exports.compile = compile;
function compile(root,output,translator,includes,excludes){
	var fs = require('fs');
	var path = require('path');
	root = fs.realpathSync(root || './');
	output = output || path.join(root,'.litecode');
	if(!fs.existsSync(output))fs.mkdirSync(output);
	//console.log('compile lite @'+root,{})
	var compiler = new LiteCompiler(root);
	includes = includes && includes.length && new RegExp(includes.map(buildURIMatcher).join('|'));
	excludes = excludes && excludes.length && new RegExp(excludes.map(buildURIMatcher).join('|'));
	function loadFile(dir){
		var files = fs.readdirSync(dir);
		for(var i=0;i<files.length;i++){
			var n = files[i];
			var file = dir+'/'+n;
			//console.warn(file)
			var stat = fs.statSync(file);
			if(stat.isFile()){
				var p = path.relative(root,file).replace(/^[\/\\]?|\\/g,'/');
				if(excludes && excludes.test(p)){
					continue;
				}
				if(includes ? includes.test(p):/\.xhtml$/.test(p)){
					console.log('compile:',path.join(output,p))
					var result = compiler.compile(p);
					var source = ['exports.template=',result.jscode,';\nexports.config = ',JSON.stringify(result.config)].join('');
					var id = getTemplateId(p);
					fs.writeFileSync(path.join(output,id)+'.js',source);
					//dest.writeFile(path.join(dest,p))
				}
				//console.log(p)
			}else if(n.charAt() != '.' &&stat.isDirectory() ){
				loadFile(file)
			}
		}
	}
	loadFile(root);
}

