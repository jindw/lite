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
	}finally{
		showResult(jscode);
		updateResultRunner('JavaScript',litecode,jscode);
	}
}
function compileToNodeJS(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var translator = new JSTranslator({waitPromise:Math.random()>.5});
		var jscode = translator.translate(litecode);
		
		var nodecode = jscode;
		showResult(nodecode);
		updateResultRunner('NodeJS',litecode,nodecode);
	}catch(e){
		try{
			new Function('return function*(){yield 1;}')
		}catch(e){
			showResult('/**\n' +
					' * 生成nodejs 代码失败，请在支持es6 yield 语法的浏览器（chrome）上测试）\n' +
					'*/');
			updateResultRunner('NodeJS',litecode,nodecode);
		}
	}
}
function compileToPHP(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var id = "/test.xhtml".replace(/[\/\-\$\.!%]/g,'_');
		var pt = new PHPTranslator({
			waitPromise:true
		});//'.','/','-','!','%'
		
		var phpcode = pt.translate(litecode,{name:id});
	}finally{
		showResult(phpcode);
		updateResultRunner('PHP',litecode,phpcode);
	}
}
function compileToLite(){
	try{
		var context = buildContext();
		var litecode = context.toList();
		var litecode = JSON.stringify(litecode);
	}finally{
		showResult(litecode);
		updateResultRunner('Java',litecode,null);
	}
}

