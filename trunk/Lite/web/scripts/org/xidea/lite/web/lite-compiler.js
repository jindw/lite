/**
 * var tf = liteFunction("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 * var tf = liteTemplate("<c:if test='${test}'></c:if>",{type:'xml',extension:'/scripts/lite-extends.js'})
 */
function liteTemplate(source,config){
	var jsTemplate = new TemplateImpl(source);
	return jsTemplate;
}
function liteFunction(source,config){
	if(config instanceof Array){
		config = {params:config}
	}else if(typeof config == 'string'){
		config = {params:Array.prototype.slice.call(arguments,1)}
	}
	var args = config.params;
	var parseContext = new ParseContext();
	if(typeof source == 'string'){
    	if(/^\s*</.test(source)){
    		data =  loadXML(source,parseContext._config._root)
    		//parseContext.loadXML(source);
    	}else{
    		data = source;
    		//data = parseContext.createURI("<c:block>aaa<![CDATA["+source+"]]></c:block>");
    	}
    }else{
        var data = parseContext.createURI(source);
    }
    parseContext.parse(data);
	var translator = new JSTranslator(config.name,args);
	translator.liteImpl = "lite__impl"
	var code = translator.translate(parseContext.toList());
	data =  window.eval("["+(code||null)+"][0]");
    data.toString=function(){//_$1 encodeXML
        return code;
    }
    return data;
}
