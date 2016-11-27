//jsi export ./compiler.js -f compressed -o .c.js
//require('jsi/lib/exports').exportScript(from,['./compiler.js']

//var parseLite = require('../../index.js').parseLite
var editorMap = editorMap || {};
var ParseContext = require('../../parse/parse-context.js').ParseContext;
var JSTranslator = require('../../parse/js-translator.js').JSTranslator;
var PHPTranslator = require('../../php/php-translator.js').PHPTranslator;
var wrapResponse = require('../../template.js').wrapResponse


var resultEditor = CodeMirror(placeMirror, {
	value: '',
	readOnly:true,
	lineNumbers: true,
	mode: {name:"javascript"}
});
/*
function nodeTest(){
	this.xmlSourceMap = {};
	this.templateEditor = {getValue:function(){
		return "<xml c:if='${test1 && test2 && test3.xxx}'>123{dddd}</xml>"
	}}
	this.showResult = this.updateResultRunner = console.log
	compileToPHP();
}
nodeTest();
*/
function buildContext(){
	var context = new ParseContext();
	var cached = {};
	for(var path in editorMap){
		cached[path] = editorMap[path].getValue();
	}
	cached["/source.xhtml"] = templateEditor.getValue();
	var baseXMLLoader = context.loadXML;
	context.loadXML = function(uri){
		if(uri.path){
			if(uri.path in cached){
				uri = cached[uri.path];
			}else{
				console.warn("未知文件路径",uri.path)
			}
		}
		return baseXMLLoader.call(context,uri);
	}
	context.parse(context.createURI('/source.xhtml'));
	return context;
}
function compileToJS(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var translator = new JSTranslator();//'.','/','-','!','%'
		var jscode = translator.translate(litecode);
	}catch(e){
		console.error("测试失败：模板编译异常：",e);
		throw e;
		return;
	}
	showResult(jscode);
	updateResultRunner('js',litecode,jscode);
}
function compileToNodeJS(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var translator = new JSTranslator({waitPromise:true});
		var jscode = translator.translate(litecode);
	}catch(e){
		console.error("测试失败：模板编译异常：",e);
		return;
	}
	var nodecode = jscode;
	showResult(nodecode);
	updateResultRunner('NodeJS',litecode,nodecode);
}
function compileToPHP(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var pt = new PHPTranslator({
			waitPromise:true,
			path:"/test.xhtml".replace(/[\/\-\$\.!%]/g,'_')
		});//'.','/','-','!','%'
		var phpcode = pt.translate(litecode);
	}catch(e){
		console.error("测试失败：模板编译异常：",e);
		throw e;
		return;
	}
	showResult(phpcode);
	updateResultRunner('php',litecode,phpcode);
}
function compileToLite(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var litecode = JSON.stringify(litecode);
	}catch(e){
		console.error("测试失败：模板编译异常：",e);
		return;
	}
	showResult(litecode);
	updateResultRunner('java',litecode,null);
}

