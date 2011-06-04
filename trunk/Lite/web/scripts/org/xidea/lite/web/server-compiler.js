/**
 * var tf = liteFunction("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 * var tf = liteTemplate("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 */
function WebCompiler(urlbase){
	this.base = urlbase;
	var config = new ParseConfig(urlbase);
}
WebCompiler.prototype.compile = function(path){
	var context = new ParseContext();
	var t = +new Date();
    context.parse(context.createURI(path));
	var data = context.toList();
	alert(new Date() - t + data)
	var pt = new PHPTranslator();
	
}