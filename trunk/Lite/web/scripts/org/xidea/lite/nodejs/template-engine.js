var fs = require('fs');
var Path = require('path');
function TemplateEngine(root){
	var config = Path.resolve(root,'WEB-INF/lite.xml');
	//console.log(Path.existsSync(config))
	if(Path.existsSync(config)){
		config = loadXMLFile(config,config+'');
	}else{
		config = null;
	}
	var root ='file:///'+ String(root).replace(/\\/g,'/');
	//this.config.root.reserve(uri.path)
	this.config = new ParseConfig(root,parseConfig(config));
	//console.dir(this.config._groups);
}
TemplateEngine.prototype.render=function(path,data,response){
	var context = buildContext(this.config,path);
	//console.dir(context.featureMap)
	var litecode = context.toList();
	var translator = new JSTranslator();//'.','/','-','!','%'
	translator.liteImpl = 'liteImpl';//avoid inline jslib 
	var jscode = translator.translate(litecode,true);
	var fcode = jscode
	var i = fcode.indexOf('charAt')
	
	//console.log(fcode.substr(i-100,300));
	data =  window.eval("["+(fcode||null)+"][0]");
	var tpl = new (Template)(data);
	var data = tpl.render(data);
	response.write(data,'utf-8')
}

function buildContext(config,path){
	var context = new ParseContext(config,path);
	context.loadXML = loadXML;
	context.loadText = loadText;
	var uri = context.createURI(path);
	context.parse(uri);
	return context;
}
function loadText(uri){
	var uri2 = this.config.root.resolve(uri.path.replace(/^\//,''));
	var path = uri2.path.replace(/^[\/\\]([A-Z]\:[\/\\])/,'$1');
	var text = fs.readFileSync(path,'utf-8');
	//console.log(text);
	return text;
}
function loadXML(uri){
	this.setCurrentURI(uri);
	var uri2 = this.config.root.resolve(uri.path.replace(/^\//,''));
	var path = uri2.path.replace(/^[\/\\]([A-Z]\:[\/\\])/,'$1');
	return loadXMLFile(path,uri)
}
function loadXMLFile(file,uri){
	var text = fs.readFileSync(file,'utf-8');
	var text = normalizeLiteXML(text,uri);
	var xml = new DOMParser().parseFromString(text);
	xml.documentURI = uri+'' || ''+file;
	return xml;
}