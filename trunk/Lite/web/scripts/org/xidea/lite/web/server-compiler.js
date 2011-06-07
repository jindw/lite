/**
 * var tf = liteFunction("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 * var tf = liteTemplate("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 */
function WebCompiler(urlbase){
	if(urlbase.charAt() == '/'){
		urlbase = location.href.replace(/([^\/])\/[^\/].*$/,'$1'+urlbase);
	}
	this.base = urlbase;
	this.config = new ParseConfig(urlbase);
}
WebCompiler.prototype.compile = function(path){
	this.path = path;
	var context = new ParseContext(this.config);
	var t = +new Date();
    context.parse(context.createURI(path));
	var litecode = context.toList();
	var res = context.getResources();
	var featureMap = context.getFeatureMap();
	var i = res.length;
	while(i--){
		res[i] = res[i].path
	}
	this.litecode = stringifyJSON([res,litecode,featureMap])
	this.compileTime = (new Date() - t )
	var t = +new Date();
	var pt = new PHPTranslator(path.replace(/[\/\-\$\.!%]/g,'_'));//'.','/','-','!','%'
	this.phpcode = pt.translate(context);
	this.translateTime = (new Date() - t )
}

WebCompiler.prototype.save = function(){
	var post = 'LITE_PATH='+encodeURIComponent(this.path)+
		'&LITE_ACTION=save&LITE_CODE='+encodeURIComponent(this.litecode)+
		'&LITE_PHP='+encodeURIComponent(this.phpcode);
	
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