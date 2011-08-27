function TemplateEngine(root){}
TemplateEngine.prototype.render=function(path,data){
	var context = buildContext();
	var litecode = context.toList();
	var translator = new JSTranslator();//'.','/','-','!','%'
	translator.liteImpl = 'lite__impl_';//avoid inline jslib 
	var jscode = translator.translate(litecode,true);
	var fcode = "function(lite__impl_def,lite__impl_get,lite__impl_encode,lite__impl_list){"+code+"}"
	data =  window.eval("["+(fcode||null)+"][0]");
	var tpl = new Template(data);
	return tpl.render(data);
}
var fs = require('fs');

function buildContext(){
	var context = new ParseContext();
	context.loadXML = loadXML;
	context.parse(context.createURI('/source.xhtml'));
}
function loadXML(uri){
	var text = fs.readFileSync(this.config.root.reserve(uri.path),'utf-8');
	var text = normalizeLiteXML(text,uri);
	var xml = new DOMParser().parseFromString(text);
	xml.documentURI = uri;
	return xml;
}