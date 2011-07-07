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
	translator.litePrefix = "lite__"
	var code = translator.translate(parseContext.toList());
	data =  window.eval("["+(code||null)+"][0]");
    data.toString=function(){//_$1 encodeXML
        return code;
    }
    return data;
}



/**
 * @public
 */
function lite__def(name,fn){
	lite__g[name] = fn||this[name];
}
/**
 * @public
 */
function lite__init(n,$_context){
	return $_context && n in $_context?$_context[n]:n in lite__g?lite__g[n]:this[n]
}
/**
 * @public
 */
function lite__list(source,result,type) {
	if (result){
		if(type == "number" && source>0){
			while(source--){
				result[source] = source+1;
			}
		}else{
			for(type in source){
				result.push(type);
			}
		}
		return result;
	}
	return source instanceof Array ? source
			: lite__list(source, [],typeof source);
}
/**
 * lite_encode(v1)
 * lite_encode(v1,/<&/g)
 * lite_encode(v1,/[<&"]|(&(?:[a-z]+|#\d+);)/ig)
 * @public
 */
function lite__encode(text,exp){
	return String(text).replace(exp||/[<&"]/g,function (c,a){
			return a || "&#"+c.charCodeAt(0)+";"
		});
}

var lite__g = {};