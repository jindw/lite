/**
 */


function liteImpl(source){
	if(config instanceof Array){
		config = {params:config}
	}else if(typeof config == 'string'){
		config = {params:Array.prototype.slice.call(arguments,1)}
	}
	var args = config.params;
	var parseContext = new ParseContext();
	switch(typeof source){
	case 'xml':
		source = source.toXMLString();
	case 'string':
    	if(/^\s*</.test(source)){
    		data =  loadLiteXML(source,parseContext.config.root)
    		//parseContext.loadXML(source);
    	}else{
    		data = source;
    		//data = parseContext.createURI("<c:block>aaa<![CDATA["+source+"]]></c:block>");
    	}
    	break;
    default:
        var data = parseContext.createURI(source);
    }
    parseContext.parse(data);
	var translator = new JSTranslator(config.name,args);
	//不让重复生成lite__impl_
	translator.liteImpl = "lite__impl_"
	var code = translator.translate(parseContext.toList(),true);
	
	data =  window.eval("(function(lite__impl_def,lite__impl_get){"+(code||null)+"})");
	data = data(lite__impl_def,lite__impl_get);
    return data;
}
