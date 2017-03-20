var fs = require('fs');
var path = require('path');
var xmldom = require('xmldom');
var ns = fs.readdirSync(__dirname);

var ParseConfig=require('lite/src/main/js/parse/config').ParseConfig;
var JSTranslator=require('lite/src/main/js/parse/js-translator').JSTranslator;
var ParseContext=require('lite/src/main/js/parse/parse-context').ParseContext;
var wrapResponse = require('lite/src/main/js/template.js').wrapResponse
var g=start();
resume()

function resume(value){
	var v = g.next(value);
	if(!v.done){
		value = v.value;
		if(value instanceof Promise){
			value.then(resumeSuccess,resumeError)
		}else{
			resumeSuccess(value)
		}
	}
}
function resumeSuccess(value){
	resume(value)
}
function resumeError(value){
	g.throw(value);
	resumeSuccess();
}
function *start(){
	for(var i=0;i<ns.length;i++){
		var n = ns[i];
		if(/\.xml$/.test(n)){
			var p = path.join(__dirname,n);
			var xml = fs.readFileSync(p).toString();
			xml = new xmldom.DOMParser().parseFromString(xml,'text/xml');
			xml.documentURI = p
			yield* testFile(xml,n);
		}
	}
}
function *testFile(dom,fileName){
	var es = dom.getElementsByTagName('unit');
	console.info('test file:'+fileName)
	for(var i=0;i<es.length;i++){
		var unit = es[i];
		var title = unit.getAttribute('title');
		var cases = dom.getElementsByTagName('case');
		var sources = dom.getElementsByTagName('source');
		var fileMap = {};
		
		for(var j=0;j<sources.length;j++){
			var s = sources[j];
			var p = s.getAttribute('path');
			if(p){
				fileMap[p] = s.textContent;
			}
		}
		
		console.info('\tTest Unit:'+title)
		for(var j=0;j<cases.length;j++){
			yield *testCase(cases[j],fileMap)
		}
	}
}
function * testCase(node,fileMap){
	var title = node.getAttribute('title');
	var source = getSource(node,'source');
	var expect = getSource(node,'expect');
	var model = readJson(getSource(node,'model',true));
	console.warn('\t\tTest Case:',title||'anonymous');//+';source:'+source);
	var fn = parseLite(source,{fileMap:fileMap});
	//console.log(node.ownerDocument.documentURI,model)
	//var result = fn(model);
	var out = [];
	//console.log('@@@@@',nodejsMock.wait)
	try{
		var promise = new Promise(function(resolve,reject){
				var resp = {
					write : function(t){
						out.push(t)
					},
					end:function(last){
						last && out.push(last);
						var result = out.join('');
						var error = assertDomEquals(expect,result)
						if(error){
							reject(error);
						}else{
							resolve(result)
						}
					}
				}
				var nodejsMock = wrapResponse(resp,fn);
				fn(model,nodejsMock);
		});
		yield promise;
	}catch(e){
		var fileName = node.ownerDocument.documentURI.replace(/.*\//,'')
		console.error('\t\t\terror @file:'+fileName,';line:',node.lineNumber)
		console.error('\t\t\t'+e)
		//console.error(e.stack)
		console.error(fn+'')
	}
}
function assertDomEquals(expect,result){
	if(result != expect){
		result = formatXML(result.replace(/\r\n?|\n/g,'\n'))
		expect = formatXML(expect.replace(/\r\n?|\n/g,'\n'))
		if(String(result)!= expect){
			//console.error()
			return [expect,'!=', result].join('')
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
