var JSTransform = require('./jstransform').JSTransform;
//通过jsi2 的语法，链接到老的类库
var liteWrapCompile =  Function.prototype;//$import('org.xidea.lite.impl.js.liteWrapCompile');
var processURI = require('./uri').processURI;

function jsCodeFilter(path,text){
	// ^ 表达式开始
	// $ 表达式结束
	// :S 匹配表达式代码
	// :ID 匹配JavaScript ID
	// :/exp/i 匹配自定义正则表达式
	// :[] 可选代码
	text = JSTransform(text).replace("^liteWrap(:S)",function(a,tpl){
		//编译LiteWrap模板
		return liteWrapCompile(tpl)
	}).replace("^encodeURI(:S)",function(a,value){
		//处理js中的url资源
		return JSON.stringify(processURI(window.eval(value)));
	}).compress();
	return text ;
}

function cssCodeFilter(path,text){
	//replace css:	url("/module/static/img/a/_/8.png")
	text = text.replace(/\:\s*url\s*\(\s*(['"]|)(.*?)\1\s*\)/g,function(a,qute,content){
		if(qute){
			content = window.eval(qute+content+qute);
		}
		content = processURI(content);
		return ":url("+JSON.stringify(content)+')';
	})
	//compress 
	//manager.compressCSS(value);
	return text.replace(/(\\(?:\r\n?|\n).)|^\s+/gm,'$1')
}
function jsiDefine(path,text){
	var source = new Function ('(function(){'+text+'})')+''
	var result = ["$JSI.define('",path,"',["];
	var sep = '';
	source.replace(/\brequire\((\"[^"]+\")\)/,function(a,dep){
		result.push(sep,dep);
		sep = ','
	})
	
	result.push('],function(){require,exports}{',text,'\n});');
	return result.join('');
}
exports.jsiDefine = jsiDefine;
exports.jsCodeFilter = jsCodeFilter;
exports.cssCodeFilter = cssCodeFilter