/*
 *          foo://example.com:8042/over/there?name=ferret#nose
 *          \_/   \______________/\_________/ \_________/ \__/
 *           |           |            |            |        |
 *        scheme     authority       path        query   fragment
 *           |   _____________________|__
 *          / \ /                        \
 *          urn:example:animal:ferret:nose
 */
var uriPattern = /^([a-zA-Z][\w\.]*)\:(?:(\/\/[^\/]*))?(\/?[^?#]*)(\?[^#]*)?(#[\s\S]*)?$/;
var absURIPattern = /^[a-zA-Z][\w\.]*\:/;
var uriChars = /\\|[\x22\x3c\x3e\x5c\x5e\x60\u1680\u180e\u202f\u205f\u3000]|[\x00-\x20]|[\x7b-\x7d]|[\x7f-\xa0]|[\u2000-\u200b]|[\u2028-\u2029]/g;
var allEncodes = /[\x2f\x60]|[\x00-\x29]|[\x2b-\x2c]|[\x3a-\x40]|[\x5b-\x5e]|[\x7b-\uffff]/g;
///[\x22\x25\x3c\x3e\x5c\x5e\x60\u1680\u180e\u202f\u205f\u3000]|[\x00-\x20]|[\x7b-\x7d]|[\x7f-\xa0]|[\u2000-\u200b]|[\u2028-\u2029]/g;


function encodeChar(i){
	return "%"+(0x100+i).toString(16).substring(1)
}
function decodeChar(c){
	var n = c.charCodeAt();
    if (n < 0x80){
        return encodeChar(n);
    }else if (n < 0x800){
    	return encodeChar(0xc0 | (n >>>  6))+encodeChar(0x80 | (n & 0x3f))
    }else{
    	return encodeChar( 0xe0 | ((n >>> 12) & 0x0f))+
    		encodeChar(0x80 | ((n >>>  6) & 0x3f))+
    		encodeChar(0x80 | (n & 0x3f))
    }
}
function uriDecode(source){
	//192,224,240
	for(var result = [], i=1;i<source.length;i+=3){
		var c = parseInt(source.substr(i,2),16);
		if(c>=240){//其实无效，js无法处理超出2字节的字符
			c = (c & 0x07)<<18;
			c += (parseInt(source.substr(i+=3,2),16) &0x3f)<<12;
			c += (parseInt(source.substr(i+=3,2),16) &0x3f)<<6;
			c += (parseInt(source.substr(i+=3,2),16) &0x3f);
		}else if(c>=224){
			c = (c & 0x0f)<<12;
			c += (parseInt(source.substr(i+=3,2),16) &0x3f)<<6;
			c += (parseInt(source.substr(i+=3,2),16) &0x3f);
		}else if(c>=192){
			c = (c & 0x1f)<<6;
			c += (parseInt(source.substr(i+=3,2),16) &0x3f);
		}
		result.push(String.fromCharCode(c))
	}
	return result.join('');
}
function uriReplace(c){
	if(c == '\\'){
		return '/';
	}else{
		return decodeChar(c);
	}
}
function URI(path){
	if(path instanceof URI){
		return path;
	}
	if(/^\s*[<]/i.test(path)){
		path = String(path).replace(uriChars,decodeChar)
		return new URI("data:text/xml,"+path);
    }else{
		path = String(path).replace(uriChars,uriReplace)
    }
    //normalize
	path = path.replace(/\/\.\/|\\\.\\|\\/g,'/');
	if(/^\/|^[a-z]\:\//i.test(path)){
		path = 'file://'+path;
	}
	while(path != (path = path.replace(/[^\/]+\/\.\.\//g,'')));
	var match = path.match(uriPattern);
	if(match){
		setupURI(this,match);
	}else{
		console.error("url must be absolute,"+path)
	}

}

function setupURI(uri,match){
	uri.value = match[0];
	uri.scheme = match[1];
	uri.authority = match[2];
	uri.path = match[3];
	uri.query = match[4];
	uri.fragment = match[5];
	
	 
	if('data' == uri.scheme){
		match = uri.value
		uri.source = decodeURIComponent(match.substring(match.indexOf(',')+1));
		
	}
}
URI.prototype = {
	resolve:function(path){
		path = String(path);
		if( /^\s*[#<]/.test(path) ||absURIPattern.test(path)){
			path = new URI(path.replace(/^\s+/,''));
			return path;
		}
		
		path = path.replace(uriChars,uriReplace)
		if(path.charAt() != '/'){
			var p = this.path;
			path = p.replace(/[^\/]*$/,path);
		}
		return new URI(this.scheme + ':'+(this.authority||'') + path);
	},
	toString:function(){
		return this.value;
	}
}

var btoa = this.btoa || function(bs){
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
var b64codes = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/='.split('');
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
function base64Encode(data){
	data = data && data.replace(/[\u0080-\uFFFF]/g,utf8Replacer)||''
	data = btoa(data) ;
	return encodeURIComponent(data);
}


function buildURIMatcher(pattern){
	var matcher = /\*+|[^\*\\\/]+?|[\\\/]/g;
	var buf = ["^"];
	var m
	matcher.lastIndex = 0;
	while (m = matcher.exec(pattern)) {
		var item = m[0];
		var len = item.length;
		var c = item.charAt(0);
		if (c == '*') {
			if (len > 1) {
				buf.push(".*");
			} else {
				buf.push("[^\\\\/]*");
			}
		} else if(len == 1 && c == '/' || c == '\\') {
			buf.push("[\\\\/]");
		}else{
			buf.push(item.replace(/[^\w]/g,quteReqExp));
		}
	}
	buf.push("$");
	return buf.join('');
}
function quteReqExp(x){
	switch(x){
	case '.':
		return '\\.';
	case '\\':
		return '\\\\';
	default:
		return '\\x'+(0x100 + x.charCodeAt()).toString(16).substring(1);
	}
}
if(typeof require == 'function'){
exports.URI=URI;
exports.buildURIMatcher = buildURIMatcher;
exports.base64Encode=base64Encode;
}