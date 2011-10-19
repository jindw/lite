/**
 */
function liteWrapImpl(data,liteImpl){
	var code = liteWrapCompile(Function.prototype.toString.apply(data,[]),'liteImpl')
	data =  window.eval('(function(liteImpl){return '+code+'})');
	data = data(liteImpl);
    return data;
}
function liteWrapCompile(code,liteImplId){
	var match = code.match(/[\w\s]+\(([\w,\s]*)\)\s*\{([\s\S]+)\}\s*/);
	var args = match[1].replace(/^\s+|\s+$/g,'');
	var source = match[2].replace(/^\s+|;?\s*$/g,'');
	var parseContext = new ParseContext();
	if(!source.match(/^</)){
		//if(source.match(/\bXML\b/)){ string?
		source = (new Function('XML','return '+source))(String);
	}
	var xml =  loadLiteXML(source);
	parseContext.parse(xml);
	var translator = new JSTranslator('',args && args.split(/\s*,\s*/));
	translator.liteImpl = liteImplId || "liteImpl"
	var code = translator.translate(parseContext.toList(),true);
	return code;
}