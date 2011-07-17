include("merge.js");
include("xhtml.js");

function textFilterJS(path,text){
	text = processJS(text);
	return "//!test\n"+text;
}
function textFilterCSS(path,text){
	text = processCSS(text);
	return text;
}

function processJS(text){
	//TODO:merge...
	//replace js:	encodeURI("/module/static/img/a/_/8.png")
	text = replacePath(text);
	//TODO:autoEncodeScript
	//${..} == > ${JSON.stringify(..)} ==> /$(tghjk)/
	//compress
	//manager.compressJS(value);
	return text.replace(/(\\(?:\r\n?|\n).)|^\s+/gm,'$1')
}
function processCSS(text){
	//TODO:merge,absolute uri
	//replace css:	url("/module/static/img/a/_/8.png")
	text = replacePath(text);
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
function addHashData(path){
	if(path.indexOf('?')>=0){
		return path;//not add hash on dynamic
	}
	if(path.indexOf('#')>=0){
		return path;//not add hash on hash url
	}
	//TODO:use result md5 is better?
	//resourceManager.getContentHash(path)
	return path+'?@='+(+new Date()).toString(32)
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
