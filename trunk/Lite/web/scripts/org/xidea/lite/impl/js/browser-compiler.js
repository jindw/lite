/**
 */
function liteWrapImpl(data,liteGlobals){
	var match = Function.prototype.toString.apply(data,[]).match(/[\w\s]+\(([\w,\s]*)\)\s*\{([\s\S]+)\}\s*/);
	var args = match[1].replace(/^\s+|\s+$/g,'');
	var source = match[2].replace(/^\s+|;?\s*/g,'');
	var parseContext = new ParseContext();
	if(!source.match(/^</)){
		//if(source.match(/\bXML\b/)){ string?
		source = (new Function('XML',source))(String);
	}
	var xml =  loadLiteXML(source);
	parseContext.parse(xml);
	var translator = new JSTranslator('',args && args.split(/\s*,\s*/));
	translator.liteImpl = "lite__impl_"
	var code = translator.translate(parseContext.toList(),true);
	data =  window.eval("(function(lite__impl_){"+(code||null)+"})");
	data = data(liteGlobals);
    return data;
}
