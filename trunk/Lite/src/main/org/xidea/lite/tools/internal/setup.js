var Env = require("org/xidea/lite/tools/internal/env");
var Merge = require("./merge");
var XHtml = require("./xhtml");
//通过jsi2 的语法，链接到老的类库
var compressJS = $import('org.xidea.lite.util.compressJS');

function textFilterJS(path,text){
	var fileList = [];
	var map = Merge.mergeJS(path,fileList);
	var buf = [];
	for(var i = 0;i<fileList.length;i++){
		var n  = fileList[i];
		Env.addRelation(n);
		buf.push(map[n])
	}
	text = processJS(buf.join('\n'));
	return text;
}
function textFilterCSS(path,text){
	var fileList = [];
	var map = Merge.mergeCSS(path,fileList,getSourceLoader(path,text));
	var buf = [];
	for(var i = 0;i<fileList.length;i++){
		var n  = fileList[i];
		Env.addRelation(n);
		buf.push(map[n])
	}
	text = processCSS(buf.join('\n'));
	return text;
}
function textFilterXHTML(path,text){
	return XHtml.normalizeXML(text,path);
}

function processJS(text){
	//replace js:	encodeURI("/module/static/img/a/_/8.png")
	text = text.replace(/\bencodeURI\s*\(\s*(['"])([^'"]+)\1\s*\)/g,function(a,qute,content){
		try{
			//野蛮替换,字符串中呢?当能,线上很少见.
			content = window.eval(qute+content+qute);
			content = replacePath(content);
			return JSON.stringify(content);
		}catch(e){
			return a;
		}
	})
	//TODO:autoEncodeScript
	//${..} == > ${JSON.stringify(..)} ==> /$(tghjk)/
	//compress
	//manager.compressJS(value);
//	text = text.replace(/(\\(?:\r\n?|\n).)|^\s+/gm,'$1');
//	text = text.replace(/^(?:\/(?:\*[\s\S]*?\*\/|\/.*)|\s+)+/g,'');//trim left
//	//切尾巴很耗时阿
//	text = text.replace( /(?:\/(?:\*[\s\S]*?\*\/\s*|\/.*)|\s+)$/g,'');//trim right
	return compressJS(text);
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
	var hash = Env.getContentHash(realpath||path)
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

Env.addTextFilter("/**.css",textFilterCSS);
Env.addTextFilter("/**.js",textFilterJS);
Env.addTextFilter("/**.xhtml",textFilterXHTML);
/* 添加文档验证器 */
Env.addDocumentFilter("/**.xhtml",XHtml.checkXHTML);
Env.addDocumentFilter("/**.xhtml",XHtml.filterXHTMLDom);

