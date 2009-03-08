/*
 * JavaScript Integration Framework
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/jsi/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

/**
 * 模板类
 * 起初base 的设计是表示，他是一个url还是一段文本。
 * 感觉没有必要，感觉需要简化。
 * 设计为接受两个参数？
 * <a href="http://code.google.com/p/jsiside/wiki/Template"> 模版基础指令说明</a>
 * @public
 */
function Template(data,parser){
    if("org.xidea.lite:compile"){
        if(!(data instanceof Array || data instanceof Function)){
            var inlineClass = {
                'xml':XMLParser,
                'text':TextParser
            }
            parser = parser || "xml";
            if(typeof parser == "string"){
                parser = new (inlineClass[parser] || $import(parser))(true);
            }
            parser.parse(data);
            data = parser.buildResult();
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
Template.prototype.render = function(context){
    var buf = [];
    var context2 = {};
    var data = this.data;
    for(var n in context){
        context2[n] = context[n];
    }
    if(data instanceof Function){
        data.apply(context2,[buf,xmlReplacer]);
    }else{
        renderList(context2,data,buf)
    }
    
    return buf.join("");
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
        value = value.replace(/[<>&]/g,xmlReplacer)
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
		out.push(String(result).replace(/[<>&'"]/g,xmlReplacer));
	}else if (result != null) {
		out.push(' ');
		out.push(data[2]);// prefix
		out.push('="');
		out.push(String(result).replace(/[<>&'"]/g,xmlReplacer));
		out.push('"');
	}

}

//function processAttributeValue(context, data, out)
//		throws IOException {
//	out.push(value.replace(/[<>&]/g,xmlReplacer));
//}
function processFor(context, data, out) {
	var children = data[1];
	var varName = data[2];
	var statusName = data[4];
	var items = evaluate(data[3],context);
	var preiousStatus = context[FOR_KEY];
	try {
	    if(!(items instanceof Array)){
            //hack $for as buf
            forStatus = [];
            //hack len as key
            for(len in data){
                forStatus.push(len);
            }
            items = forStatus;
        }
        var len = items.length;
		var forStatus = {lastIndex:len-1,depth:preiousStatus?preiousStatus.depth+1:0};
		context[FOR_KEY]= forStatus;
		if (statusName != null) {
			context[statusName]=forStatus;
		}
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
    switch(c){
        case '<':
          return '&lt;';
        case '>':
          return '&gt;';
        case '&':
          return '&amp;';
        case "'":
          return '&#39;';
        case '"':
          return '&#34;';
    }
}
