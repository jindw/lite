/**
 * var tf = liteFunction("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 * var tf = liteTemplate("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 */
function WebCompiler(urlbase,config){
	if(urlbase.charAt() == '/'){
		urlbase = location.href.replace(/([^\/])\/[^\/].*$/,'$1'+urlbase);
	}
	this.base = urlbase;
	config = config && parseConfig(config) || null;
	this.config = new ParseConfig(urlbase,config);
}
WebCompiler.prototype.compile = function(path){
	var t = +new Date();
	var context = new ParseContext(this.config,path);
	this.litecode = '';
	this.phpcode = '';
	this.path = path;
    context.parse(context.createURI(path));
	this.compileTime = (new Date() - t - (context._loadTime ||0))
	var res = context.getResources();
	var featureMap = context.getFeatureMap();
	var i = res.length;
	while(i--){
		res[i] = res[i].path
	}
	var litecode = context.toList();
	litecode = [res,litecode,featureMap];
	this.litecode = stringifyJSON(litecode)
	var t = +new Date();
	var pt = new PHPTranslator(path,litecode);//'.','/','-','!','%'
	this.phpcode = pt.translate(litecode[1]);
	this.translateTime = (new Date() - t )
}

WebCompiler.prototype.save = function(){
	var post = 
		"compileTime="+this.compileTime+
		"&translateTime="+this.translateTime+
		'&LITE_PATH='+encodeURIComponent(this.path)+
		'&LITE_ACTION=save&LITE_CODE='+base64Encode(this.litecode)+
		'&LITE_PHP='+base64Encode(this.phpcode);
		
	
	var xhr = new XMLHttpRequest();
	xhr.open("POST", this.base, false);
	//contentType:  'application/x-www-form-urlencoded',
	xhr.setRequestHeader("Content-Type",'application/x-www-form-urlencoded');
	xhr.setRequestHeader("Content-Length",''+post.length);
	xhr.send(post);
	try{
		window.eval('('+xhr.responseText+')')
		return true;
	}catch(e){
		$log.info("编译失败:",xhr.responseText);
		return false;
	}
}
