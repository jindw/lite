/**
 * var tf = liteFunction("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 * var tf = liteTemplate("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 */
function WebCompiler(urlbase){
	this.base = urlbase;
	var config = new ParseConfig(urlbase);
}
WebCompiler.prototype.compule = function(path){
	var context = new ParseContext();
    context.parse(context.createURI(path));
	alert(JSON.stringify(context.toList()))
}