/**
 */
function liteWrapImpl(data,liteImpl){
	var code = liteWrapCompile(Function.prototype.toString.apply(data,[]),'liteImpl')
	data =  window.eval('(function(liteImpl){return '+code.replace(/^\s+/,'')+'})');
	data = data(liteImpl);
    return data;
}
function replaceUnicode(a,code){
	return String.fromCharCode(parseInt(code,16))
}
function liteWrapCompile(code,liteImplId){
	var match = code.match(/[\w\s]+\(([\w,\s]*)\)\s*\{([\s\S]+)\}\s*/);
	var args = match[1].replace(/^\s+|\s+$/g,'');
	var source = match[2].replace(/^\s*(?:return\s+)?\s*|;?\s*$/g,'');
	var parseContext = new ParseContext();
	source = source.replace(/\\u([a-fA-F\d]{4})/g,replaceUnicode)
	if(!source.match(/^</)){
		//if(source.match(/\bXML\b/)){ string?
		source = (new Function('XML','return '+source))(String);
	}
	var xml =  loadLiteXML(source);
	parseContext.parse(xml);
	var translator = new JSTranslator('',args && args.split(/\s*,\s*/));
	translator.liteImpl = liteImplId || "liteImpl"
	var code = translator.translate(parseContext.toList());
	return code;
}