//jsi  export ./web-compiler.js -o .wc.js
var PHPTranslator=require('lite/php/php-translator').PHPTranslator;
var ParseContext=require('lite/parse/parse-context').ParseContext;
var ParseConfig=require('lite/parse/config').ParseConfig;
var parseConfig=require('lite/parse/config-parser').parseConfig;
var URI=require('lite/parse/resource').URI;
var base64Encode=require('lite/parse/resource').base64Encode;
require('fs').readFileSync = function(url){
	var xhr = new XMLHttpRequest();
	//var url = this.base+'?LITE_ACTION=load'
	xhr.open("GET", url, false);
	//contentType:  'application/x-www-form-urlencoded',
	xhr.setRequestHeader("Content-Type",'application/x-www-form-urlencoded');
	///xhr.setRequestHeader("Content-Length",''+post.length);
	xhr.send();
	return xhr.responseText;
}
/**
 * var tf = liteFunction("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 * var tf = liteTemplate("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 */
function WebCompiler(urlbase,config){
	if(urlbase.charAt() == '/'){
		urlbase = location.href.replace(/([^\/])\/[^\/].*$/,'$1'+urlbase);
	}
	this.base = urlbase;
	//config = config && parseConfig(config) || null;
	this.config = new ParseConfig(urlbase,config);
}
WebCompiler.prototype.compile = function(path){
	try{
		var t = +new Date();
		var context = new ParseContext(this.config,path);
		this.litecode = '';
		this.phpcode = '';
		this.path = path;
	    context.parse(context.createURI(path));
		this.compileTime = (new Date() - t - (context._loadTime ||0))
		var res = context.getResources();
		var configMap = context.getConfigMap();
		var i = res.length;
		while(i--){
			res[i] = res[i].path
		}
		var code = context.toList();
		this.litecode = JSON.stringify([res,code,configMap])
		var t = +new Date();
		var pt = new PHPTranslator({waitPromise:true});//'.','/','-','!','%'
		this.phpcode = pt.translate(code,{
			name:path.replace(/[\.\/\-!%]/g,'_')
		});
		//console.error(this.litecode)
		//console.error(this.phpcode)
		this.translateTime = (new Date() - t );
	}finally{
		if(!this.phpcode){
			if(!this.litecode){
				prompt('build litecode failed!!'+this.path);
			}else{
				prompt('build phpcode failed!!'+this.path);
			}
		}
	}
}

WebCompiler.prototype.save = function(){
	var post = 
		"compileTime="+this.compileTime+"&translateTime="+this.translateTime+
		'&LITE_ACTION=save' +
		'&LITE_PATH='+encodeURIComponent(this.path)+
		'&LITE_CODE='+base64Encode(this.litecode)+
		'&LITE_PHP='+base64Encode(this.phpcode);
		
	
	var xhr = new XMLHttpRequest();
	xhr.open("POST", this.base, false);
	//contentType:  'application/x-www-form-urlencoded',
	xhr.setRequestHeader("Content-Type",'application/x-www-form-urlencoded');
	//try{xhr.setRequestHeader("Content-Length",''+post.length);}catch(e){}
	xhr.send(post);
	try{
		window.eval('('+xhr.responseText+')')
		return true;
	}catch(e){
		console.info("编译失败:",xhr.responseText);
		return false;
	}
}

//exports.WebCompiler=WebCompiler;
