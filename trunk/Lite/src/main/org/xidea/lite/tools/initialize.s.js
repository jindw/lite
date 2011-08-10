include("merge.s.js");
include("xhtml.s.js");
function getSourceLoader(path,text){
	return function(path0){
		if(path0 == path){
			return text;
		}else{
			return loadChainText(path0)
		}
	}
}
function textFilterJS(path,text){
	var fileList = [];
	var map = mergeJS(path,fileList,getSourceLoader(path,text));
	var buf = [];
	for(var i = 0;i<fileList.length;i++){
		var n  = fileList[i];
		addRelation(n);
		buf.push(map[n])
	}
	text = processJS(buf.join('\n'));
	return text;
}
function textFilterCSS(path,text){
	var fileList = [];
	var map = mergeCSS(path,fileList,getSourceLoader(path,text));
	var buf = [];
	for(var i = 0;i<fileList.length;i++){
		var n  = fileList[i];
		addRelation(n);
		buf.push(map[n])
	}
	text = processCSS(buf.join('\n'));
	return text;
}

function processJS(text){
	//replace js:	encodeURI("/module/static/img/a/_/8.png")
	text = text.replace(/\bencodeURI\s*\(\s*(['"])(.*?)\1\s*\)/g,function(a,qute,content){
		content = window.eval(qute+content+qute);
		content = replacePath(content);
		return JSON.stringify(content);
	})
	//TODO:autoEncodeScript
	//${..} == > ${JSON.stringify(..)} ==> /$(tghjk)/
	//compress
	//manager.compressJS(value);
	return text.replace(/(\\(?:\r\n?|\n).)|^\s+/gm,'$1')
}
function processCSS(text){
	//replace css:	url("/module/static/img/a/_/8.png")
	text = text.replace(/\:\s*url\s*\(\s*(['"]|)(.*?)\1\s*\)/g,function(a,qute,content){
		if(qute){
			content = window.eval(qute+content+qute);
		}
		content = replacePath(content);
		return ":url("+JSON.stringify(content)+')';
	})
	//compress 
	//manager.compressCSS(value);
	return text.replace(/(\\(?:\r\n?|\n).)|^\s+/gm,'$1')
}
function processURI(path){
	//replace exact
	path = replacePath(path);
	//TODO:autoEncode
	//${..} == > ${encodeURIComponent(..)} 
	return path;
}
function replacePath(path){
	//exact: /module/static/img/a/_/8.png
	//resourceManager.replacePath(path);
	return path;
}
function addHashData(path,realpath){
	if(path.indexOf('?')>=0){
		return path;//not add hash on dynamic
	}
	if(path.indexOf('#')>=0){
		return path;//not add hash on hash url
	}
	//TODO:use result md5 is better?
	var hash = resourceManager.getContentHash(realpath||path)
	if(hash){
		return path+'?@='+hash
	}else{
		return path;
	}
}
/**
 * 这个工作比较难啊，模板要定位所有资源路径。
 * js/css中通过规范解决（css 资源都用url(...),js 资源地址都用encodeURI(...)），
 */
function addContextPath(path,contextPath){
	if(path.charAt() == '/'){
		return contextPath + path;
	}
	return path;
}

addTextFilter("/**.css",textFilterCSS);
addTextFilter("/**.js",textFilterJS);
addTextFilter("/**.xhtml",textFilterXHTML);
addDocumentFilter("/**.xhtml",domFilterXHTML);
