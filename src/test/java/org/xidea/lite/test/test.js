var testRoot;
var LiteCompiler = require('lite/src/main/js/compiler').LiteCompiler;
var PHPTranslator=require('lite/src/main/php/php-translator').PHPTranslator;
var xmldom = require('xmldom');
var DOMParser = xmldom.DOMParser;
var testCompiler;
var testResourceMap={}
var compileResult;
function initTest(root){
	testRoot = root
	testCompiler = new LiteCompiler(testRoot,{waitPromise:false});
	//liteCompiler.translator = null;
}
var createParseContext = LiteCompiler.prototype.createParseContext;
LiteCompiler.prototype.createParseContext = function(path){
	var ctx = createParseContext.apply(this,arguments);
	var loadXML = ctx.loadXML;
	
	//console.log(path+'!');
	ctx.loadXML = function(path){
		//console.log(path+'!2'+path.path);
		var p = path.path || path.replace(/^lite\:\/\/[^\/]+/,'');
		//console.log(path+'!'+p);
		//console.log(path,p, testResourceMap[p])
		if(testResourceMap && testResourceMap[p]){
			path =  testResourceMap[p]
		}
		var doc =  loadXML.call(this,path)
		//console.log(doc+'');
		return doc;
		//console.log(path)
	}
	return ctx;
}
function buildTemplate(resourceMap,path){
	testResourceMap = resourceMap || {};
	//engine.eval("var lite = require('lite')");
	/*
	String result = engine.eval("JSON.stringify(liteCompiler.compile("+JSONEncoder.encode(path)+"))").toString();
	Map<String,Object> json = JSONDecoder.decode(result);
	Map<String,Object> config = (Map<String,Object>)json.get("config");
	List<String> resources = (List<String>)json.get("resources");
	List<Object> litecode = (List<Object>)json.get("litecode");
	return JSONEncoder.encode(new Object[]{resources,litecode,config});
	* 
	var config = result.config;
	var resources = result.resources;
	var litecode = result.litecode;
	* */
	compileResult = testCompiler.compile(path);

	compileResult.path = path;
	return JSON.stringify(compileResult);
}
function getJsCode(compileResult){
	return compileResult.code;
}
function getPhpCode(compileResult){
	//this.litecode = JSON.stringify([compileResult.resources,compileResult.litecode,compileResult.config])
	var pt = new PHPTranslator({waitPromise:true});//'.','/','-','!','%'
	return pt.translate(compileResult.litecode,{
		name:compileResult.path.replace(/[\.\/\-!%]/g,'_')
	});
}
function formatXML(source){
	var dom = new DOMParser().parseFromString(source,'text/html');
	var result = dom.toString(true,function filter(node){
		//console.log(JSON.stringify(arguments));
		node.attributes && [].sort.call(node.attributes,function(attr1,attr2){
			return attr1.name>attr2.name?1:attr1.name==attr2.name?0:-1;
		});
		return node;
	});
	//console.log(result);
	return result;
	
}