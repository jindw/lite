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
	this.litecode = '';
	this.phpcode = '';
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
	this.phpcode = pt.translate(context.toList());
	this.translateTime = (new Date() - t )
}

WebCompiler.prototype.save = function(){
	var post = 'LITE_PATH='+encodeURIComponent(this.path)+
		'&LITE_ACTION=save&LITE_CODE='+encode(this.litecode)+
		'&LITE_PHP='+encode(this.phpcode);
	
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
var b64codes = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/='.split('');
var btoa = window.btoa || function(bs){
	var b64 = [];
    var bi = 0;
    var len = bs.length;
    while (bi <len) {
        var b0 = bs.charCodeAt(bi++);
        var b1 = bs.charCodeAt(bi++);
        var b2 = bs.charCodeAt(bi++);
        var data = (b0 << 16) + (b1 << 8) + (b2||0);
        b64.push(
        	b64codes[(data >> 18) & 0x3F ],
        	b64codes[(data >> 12) & 0x3F],
        	b64codes[isNaN(b1) ? 64 : (data >> 6) & 0x3F],
        	b64codes[isNaN(b2) ? 64 : data & 0x3F]) ;
    }
    return b64.join('');

}
function utf8Replacer(c){
	var n = c.charCodeAt();
	if (n < 0x800){
        return String.fromCharCode(
            (0xc0 | (n >>>  6)),
            (0x80|(n & 0x3f)));
    }else{
        return String.fromCharCode(
            (0xe0 | ((n >>> 12) & 0x0f)),
            (0x80 | ((n >>>  6) & 0x3f)),
            (0x80 |  (n         & 0x3f)));
    }
}
function encode(data){
	data = data.replace(/[\u0080-\uFFFF]/g,utf8Replacer)
	data = window.btoa(data) ;
	return encodeURIComponent(data);
}
