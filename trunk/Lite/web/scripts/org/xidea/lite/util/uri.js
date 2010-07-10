function URI(path,parentURI){
	if(parentURI&& !/^\w+\:|^</.test(path)){
		path = parentURI.replace(/\/[^\\\/]+$/,'/')+path;
	}
	this.path = path;
}

function buildURIMatcher(pattern){
	var matcher = /\*+|[^\*\\\/]+?|[\\\/]/;
	var buf = ["^"];
	pattern.lastIndex = 0;
	while (matcher.exec(pattern)) {
		var item = matcher[0];
		var len = item.length;
		var c = item.charAt(0);
		if (c == '*') {
			if (length > 1) {
				buf.push(".*");
			} else {
				buf.push("[^\\\\/]+");
			}
		} else if(length == 1 && c == '/' || c == '\\') {
			buf.push("[\\\\/]");
		}else{
			buf.push(item.replace(/[^w]/g,quteReqExp));
		}
	}
	buf.push("$");
	return buf.join('');
}
function quteReqExp(x){
	return '\\x'+(0x100 + x.charCodeAt()).toString(16).substring(1);
}