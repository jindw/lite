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
    	var data = source.replace(/^#.*[\r\n]/,'');
    	if(data==source){
    		data = parseContext.createURI(source);
    	}
    }else{
        var data = parseContext.createURI(source);
    }
    parseContext.parse(data);
	var translator = new JSTranslator(config.name,args);
	var code = translator.translate(parseContext);
    data =  window.eval("["+(code||null)+"][0]");
    data.toString=function(){//_$1 encodeXML
        return code;
    }
    return data;
}