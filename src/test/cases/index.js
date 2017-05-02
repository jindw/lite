var fs = require('fs');
var path = require('path');
var xmldom = require('xmldom');
var assert = require('assert')

var ParseConfig=require('lite/src/main/js/parse/config').ParseConfig;
var JSTranslator=require('lite/src/main/js/parse/js-translator').JSTranslator;
var ParseContext=require('lite/src/main/js/parse/parse-context').ParseContext;
var wrapResponse = require('lite/src/main/js/template.js').wrapResponse
var asf = require('asf')

describe('xml cases loader', function (done){
	start();
 })



function start(){
	var fileNames = fs.readdirSync(__dirname);
	for(var i=0;i<fileNames.length;i++){
		var fileName = fileNames[i];
		if(/\.xml$/.test(fileName)){
			describe('test case filename:'+fileName,function(){ 
				var p = path.join(__dirname,fileName);
				var xml = fs.readFileSync(p).toString();
				xml = new xmldom.DOMParser().parseFromString(xml,'text/xml');
				xml.documentURI = p
				testFile(xml,fileName);
			})
		}
	}
}
function testFile(dom,fileName){
	var es = dom.getElementsByTagName('unit');
	//console.info('test file:'+fileName)
	for(var i=0;i<es.length;i++){
		var unit = es[i];
		var title = unit.getAttribute('title');
		var cases = dom.getElementsByTagName('case');
		var sources = dom.getElementsByTagName('source');
		var fileMap = {};
		describe('test unit:['+i+']'+title,function(){ 
			for(var j=0;j<sources.length;j++){
				var s = sources[j];
				var p = s.getAttribute('path');
				if(p){
					fileMap[p] = s.textContent;
				}
			}
			
			//console.info('\tTest Unit:'+title,cases.length)
			for(var j=0;j<cases.length;j++){
				var caseNode = cases[j];
				var title = caseNode.getAttribute('title');
				it('test case:'+title,buildCase(caseNode,fileMap));
			}
		});
	}
}
function buildCase(node,fileMap){

	var title = node.getAttribute('title');
	var source = getSource(node,'source');
	var expect = getSource(node,'expect');
	var model = readJson(getSource(node,'model',true));
	//console.warn('\t\tTest Case:',title||'anonymous');//+';source:'+source);
	var Template = require('lite/src/main/js/template').Template;//Template.prototype.lazyArrived
	var fn = parseLite(source,{fileMap:fileMap});
	//console.log(fn+'')
	var tpl = new Template(fn,{path:'/test.xhtml#'+title})
	//console.log(node.ownerDocument.documentURI,model)
	//var result = fn(model);
	
	return function(done){
		try{
			var out = [];
			var resp = {
				write : function(t){
					out.push(t)
				},
				end:function(last){
					last && out.push(last);
					var result = out.join('');
					var error = assertDomEquals(expect,result,fn)
					done(error)
				}
			}
			//var responseMock = wrapResponse(resp,tpl);
			tpl.render(model,resp)['catch'](String);
		}catch(e){
			var fileName = node.ownerDocument.documentURI.replace(/.*\//,'')
			console.error('\t\t\terror @file:'+fileName,';line:',node.lineNumber)
			done(e)
		}
	}
}
function assertDomEquals(expect,result,code){
	if(result != expect){
		result = formatXML(result.replace(/\r\n?|\n/g,'\n'))
		expect = formatXML(expect.replace(/\r\n?|\n/g,'\n'))
		if(String(result)!= expect){
			//console.error()
			return ['expected:\n',expect,'\nbut(!=)\n', result,code&&('\n'+code)].join('')
		}
	}
}
function readJson(json){
	return new Function("return "+(json||'{}').replace(/^\s+/gm,''))();
}
function formatXML(xml){
	if(!/^\s*</.test(xml)){
		return xml;
	}
	try{
	var doc = new xmldom.DOMParser({errorHandler:function(){}}).parseFromString(xml,'text/xml');
	return doc.toString(false,function(node){
		if(node.nodeType == 3){
			node.data = node.data.replace(/^\s+|\s+$|\s\s+/g,'\n');
		}
		return node;
	}).replace(/^\s+|\s+$/g,'')
	}catch(e){
		console.log(xml)
		throw e;
	}
}
function getSource(node,tagName,findParent){
	var child = node.firstChild;
	while(child){
		if(child.nodeType ==1 && child.tagName == tagName){
			return child.textContent.replace(/\r\n?|\n/g,'\n');
		}
		child = child.nextSibling;
	}
	if(findParent){
		node = node.parentNode;
		return node && getSource(node,tagName,true)
	}
}

function parseLite(data,config){
	var root = config&&config.root;
	var path = config&&config.path;
	var fileMap = config.fileMap || {};
	if(!path){
		if(typeof data == 'string' && /^\//.test(data)){
			path = data;
		}else{
			path = data.documentURI;//dom
		}
	}
	
	root = root || String(path).replace(/[^\/\\]+$/,'');
	var parseContext = new ParseContext(root && new ParseConfig(root));
	path && parseContext.setCurrentURI(path)
	if(typeof data == 'string'){
		//console.log(path,parseContext.currentURI)
		data = parseContext.loadXML(data);
	}
	var loadXML = parseContext.loadXML;
	parseContext.loadXML = function(uri){
		if(uri.path in fileMap){
			uri = fileMap[uri.path];
		}
		return loadXML.apply(this,arguments,{errorHandler:function(){}})
	}
	parseContext.loadText = function(uri){
		
		if(uri.path in fileMap){
			//console.log('css',uri.path,fileMap[uri.path])
			return fileMap[uri.path];
		}
		//console.log("gggg",uri)
		return fileMap[uri]
	}
	parseContext.parse(data);
	try{
		if(config instanceof Array){
			config = {params:config} 
		}
		var translator = new JSTranslator({waitPromise:true,name:'_'});
		//translator.liteImpl = "lite_impl"
		var code = translator.translate(parseContext.toList(),config);
		//console.log(code)
		data =  new Function('return '+code).call();
		//console.log('@@@')
		data.toString=function(){//_$1 encodeXML
			return code;
		}
		return data;
	}catch(e){
		console.error("translate error",e,code)
		throw e;
	}
}
