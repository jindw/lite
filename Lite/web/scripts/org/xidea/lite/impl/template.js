/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var EL_TYPE = 0;// [0,'el']
var IF_TYPE = 1;// [1,[...],'test']
var BREAK_TYPE = 2;// [2,depth]
var XML_ATTRIBUTE_TYPE = 3;// [3,'value','name']
var XML_TEXT_TYPE = 4;// [4,'el']
var FOR_TYPE = 5;// [5,[...],'items','var']
var ELSE_TYPE = 6;// [6,[...],'test']//test opt?
var PLUGIN_TYPE =7;// [7,[...],'el','clazz']
var VAR_TYPE = 8;// [8,'value','name']
var CAPTRUE_TYPE = 9;// [9,[...],'var']

var IF_KEY = "if";
var FOR_KEY = "for";
var PLUGIN_DEFINE = "org.xidea.lite.DefinePlugin";
/**
 * 如果传入的是json 数组 或者是函数对象，直接作为编译结果初始化，否则，作为源代码编译。
 * @param data 模板源代码或者编译结果
 * @param parser 解析器对象，或者类名（通过jsi导入），可选
 * <a href="http://code.google.com/p/lite/wiki/Template"> 模版基础指令说明</a>
 * @public
 */
function TemplateImpl(data,parseContext,runAsLiteCode){
    if(!(data instanceof Array || data instanceof Function)){
        if(parseContext == null|| parseContext == "xml"){
        	parseContext = new ParseContext();
        }else if(typeof parseContext == "string"){
            parseContext = new $import(parseContext)();
        }
        if(typeof data == 'string'){
        	data = parseContext.createURI(data);
        }
        parseContext.parse(data);
        if(!runAsLiteCode){
	        try{
		    	var translator = new Translator("");
		    	var code = translator.translate(parseContext);
	            data =  window.eval("["+(code||null)+"][0]");
	            data.toString=function(){//_$1 encodeXML
	                return code;
	            }
	        }catch(e){
	        	$log.error("翻译结果错误：",e,code)
	            throw e;
	        }
	        this.compileData = data;
        }
    }
    //alert(data.join("\n"));;
    /**
     * 模板数据
     * @private
     * @tyoeof string
     */
    this.data = data;
    //alert(this.data)
}
/**
 * 渲染模板
 * @public
 */
TemplateImpl.prototype.render = function(context){
	try{
	    var data = this.data;
	    if(data instanceof Function){
	        return this.data(context);
	    }else{
	        var i=data.length;
	        var context2 = {};
	        while(i--){//本来是编译期处理的,偷懒,性能优化在toNative中处理吧:(
	            var item = data[i];
	            if(item instanceof Array && item[0] == PLUGIN_TYPE){
	                if(item[3] == PLUGIN_DEFINE){
	                    processDef(context2, item);
	                }
	            }
	        }
	        for(var i in context){
	            context2[i] = context[i];
	        }
	        var buf = [];
	        renderList(context2,data,buf);
	        return buf.join("");
	    }
	}catch(e){
		$log.warn("模板渲染异常：",e,this.data+'');
	}
}


function processDef(context, item){
    var fn = evaluate(item[2],context);
    var args = fn.params;
    var fn = fn.name;
    context[fn] = function(){
        var context = {};
        var buf = [];
        for(var n in this){
            context[n] = this[n];
        }
        n = args.length;
        while(n--){
            context[args[n]] = arguments[n]
        }
        renderList(context,item[1],buf);
        return buf.join('');
    }
}
/**
 * 模版渲染函数
 * @internal
 */
function renderList(context,data,out){
    for(var i=0;i<data.length;i++){
        var item = data[i];
        if(typeof item == 'string'){
            out.push(item)
        }else{
        	try{
	            switch(item[0]){
	            case EL_TYPE:
	                processExpression(context, item, out, false);
	                break;
	            case XML_TEXT_TYPE:
	                processExpression(context, item, out, true);
	                break;
	            case VAR_TYPE:
	                processVar(context, item);
	                break;
	            case CAPTRUE_TYPE:
	                processCaptrue(context, item);
	                break;
	            case IF_TYPE:
	                processIf(context, item, out);
	                break;
	            case ELSE_TYPE:
	                processElse(context, item, out);
	                break;
	            case FOR_TYPE:
	                processFor(context, item, out);
	                break;
	            case XML_ATTRIBUTE_TYPE:
	                processAttribute(context, item, out);
	                break;
	            }
        	}catch(e){
        		$log.debug("render error",item,e)
        	}
        }
    }
}
/**
 * 构建表达式
 * el             [EL_TYPE,expression]
 * @internal
 */
function processExpression(context, data, out, encode){
    var value = String(evaluate(data[1],context));
    if(encode ){
        value = value.replace(/[<&"]/g,xmlReplacer)
    }
    out.push(value);
}



function processIf(context, data, out) {
	var test = true;
	try {
		if (evaluate(data[2],context)) {
			renderList(context,  data[1], out);
		}else{
			test = false;
		}
	} finally {
		context[IF_KEY]= test;
	}
	
}

function processElse(context, data, out) {
	if (!context[IF_KEY]) {
		var test =true;
		try {
			if (data[2] == null
					|| evaluate(data[2],context)) {// if
				renderList(context, data[1], out);
			}else{
				test = false;
			}
		}  finally {
			context[IF_KEY]= test;
		}
	}
}
/**
 * 构建申明处理
 * var            [VAR_TYPE,expression,name]             //设置某个变量（el||string）
 * @internal
 */
function processVar(context,data){
    context[data[2]]= evaluate(data[1],context);
}
	
/**
 * 构建申明处理
 * var            [CAPTRUE_TYPE,[...],name]             //设置某个变量（el||string）
 * @internal
 */	
function processCaptrue(context, data) {
	var buf = [];
	renderList(context, data[1], buf);
	context[data[2]]= buf.join('');
}
function processAttribute(context, data, out){
	var result = evaluate(data[1],context);
	if(!data[2]){
		out.push(String(result).replace(/[<&"]/g,xmlReplacer));
	}else if (result != null) {
		out.push(' ');
		out.push(data[2]);// prefix
		out.push('="');
		out.push(String(result).replace(/[<&"]/g,xmlReplacer));
		out.push('"');
	}

}

//function processAttributeValue(context, data, out)
//		throws IOException {
//	out.push(value.replace(/[<>&]/g,xmlReplacer));
//}
function processFor(context, data, out) {
	var children = data[1];
	var items = evaluate(data[2],context);
	var varName = data[3];
	//var statusName = data[4];
	var preiousStatus = context[FOR_KEY];
	try {
	    if(!(items instanceof Array)){
            //hack $for as buf
            forStatus = [];
            //hack len as key
            for(len in data){
                forStatus.push({key:len,value:data[len]});
            }
            items = forStatus;
        }
        var len = items.length;
		var forStatus = {lastIndex:len-1,depth:preiousStatus?preiousStatus.depth+1:0};
		context[FOR_KEY]= forStatus;
		//if (statusName != null) {
		//	context[statusName]=forStatus;
		//}
		for (var i=0;i<items.length;i++) {
			forStatus.index = i;
			context[varName]= items[i];
			renderList(context, children, out);
		}
	} finally {
		// context.put("for", preiousStatus);
		context[FOR_KEY]= preiousStatus;// for key
		context[IF_KEY]= len > 0;// if key
	}
}
function xmlReplacer(c){
    return "&#"+c.charCodeAt()+';';
}
