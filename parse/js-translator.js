if(typeof require == 'function'){
var ELSE_TYPE=require('./template-token').ELSE_TYPE;
var TranslateContext=require('./translate-context').TranslateContext;
}/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

/**
 * JS原生代码翻译器实现
 */
function JSTranslator(name,params,defaults){
    this.name = name;
    this.params = params;
    this.defaults = defaults;
    this.liteDefined = false;
}
/**
 */
JSTranslator.prototype = {
	translate:function(list,rtf){
	    //try{
		    var context = new JSTranslateContext(list,this.name,this.params,this.defaults);
		    context.liteImpl = this.liteImpl || "lite_impl";
		    context.parse();
		    var code = context.toSource(rtf);//context.header +  context.body;
		    //console.log('###'+code+'@@@')
		    if(!this.name && !rtf){
		    	new Function('return '+code);
		    }else{
		    	new Function(code);
		    }
	    //}catch(e){
	    //	var error = console.error("生成js代码失败:",e,code);
	     //   code = "return ("+JSON.stringify(error)+');';
	    //}
		var result = [];
    	//if(!this.liteImpl){
    	//	result.push("if('undefined' ==typeof ",context.liteImpl,"){",context.liteImpl,'=',INIT_SCRIPT,"}");
    	//}
	    result.push('\n',code.replace(/<\/script>/g,'<\\/script>'));
	    return result.join("").replace(/^\s*[\r\n]+/,'');
	}
}
/**
 * <code>

function(__context__){
	var __out__ = [];
	var var1 = __context__.var1;
	var var2 = __context__.var2;
	var var3 = __context__.var3;
	
	function __x__(){}
	function __e__(){}
	function __df__(){}
	
	function def(arg1){
		var __out__ = [];
		if(arg1){
			__out__.push('[',arg1,']');
		}
		return __out__.join('');
	}
	function def2(){arg2}{
		return '['+arg2+']';
	}
	....
	return __out__.join('');
}

 */
function JSTranslateContext(code,name,params,defaults){
    TranslateContext.call(this,code,name,params,defaults);
    this.forStack = [];
    this.defaults = defaults;
    
	this.xmlEncoder = 0;
	this.entityEncoder=0;
	this.dateFormat = {hit:0};
}


var GLOBAL_DEF_MAP ={
	"parseInt":1, 	
	"parseFloat":1, 	
	"encodeURIComponent":1, 	
	"decodeURIComponent":1, 	
	"encodeURI":1, 	
	"decodeURI":1, 	
	"isFinite":1, 	
	"isNaN":1
};
var GLOBAL_VAR_MAP ={
	"JSON":1,
	"Math":1
}
copy(GLOBAL_DEF_MAP,GLOBAL_VAR_MAP);

function copy(source,target){
	for(var n in source){
		target[n] = source[n];
	}
}

function optimizeFunction(contents,functionName,params,defaults,vars){
	var result = vars.concat();
	var args = '__context__';
	if(params){
		args = params.join(',');
	}
	if(defaults && defaults.length){
		result.push('\tswitch(arguments.length){\n');
		var begin = params.length - defaults.length
		for(var i =0;i<params.length;i++){
			result.push('\t	case ',i,':\n');
			if(i>=begin){
				result.push('\t	',params[i],'=',JSON.stringify(defaults[i-begin]),';\n');
			}
		}
		result.push('\t}\n');
	}
	var source = contents.join('')
	var SP = /^\s*\__out__\.push\((?:(.*)\)\s*;?)\s*$/g;
	if(SP.test(source)){
		var c  =source.replace(SP,'$1');
		if(c.indexOf(',')>0){
			//安全方式吧.
			source = "\treturn ["+c+"].join('');";
		}else{
			source = "\treturn "+c+';';
		}
	}else{
		source = "\tvar __out__=[]\n"+source+"\n\treturn __out__.join('');\n";
	}
	if(functionName){
    	try{
    		new Function("function "+functionName+"(){}");
    		functionName = "function "+functionName;
    	}catch(e){
    		functionName += "=function";
    	}
    }else{
    	functionName = "function";
    }
    return functionName+"("+args+'){\n'+result.join('')+source.replace(/^[\r\n]+/,'')+'\n}';
}
function buildVars(context,scope,params){
	var result = [];
	var map = {};
	var refMap = scope.externalRefMap;
	var callMap = scope.callMap;
	var varMap = scope.varMap;
	var paramMap = scope.paramMap;
	
	copy(refMap,map);
	copy(callMap,map);
	for(var n in map){
		if(n != '*' && !((n in GLOBAL_VAR_MAP)|| (n in varMap) || (n in paramMap))){
			if(params){//no __context__
				//result.push('\tvar ',n,'=',context.liteImpl,'["',n,'"];\n');
			}else{
				//result.push('\tvar ',n,'=("',n,'" in __context__? __context__:',context.liteImpl,')["',n,'"];\n');
				result.push('\tvar ',n,'=__context__["',n,'"];\n');
			}
			
		}
	}
	return result;
}
function PT(pt){
	for(var n in pt){
		this[n]=pt[n];
	}
}
function OutputItem(args){
	this.data = [args];
}
OutputItem.prototype.toString = function(){
	var data = this.data;
	var buf = ["__out__.push("];
	for(var i=0;i<data.length;i++){
		if(i>0){
			buf.push(',')
		}
		buf.push.apply(buf,data[i]);
	}
	buf.push(");");
	return buf.join('')
}

PT.prototype = TranslateContext.prototype
JSTranslateContext.prototype = new PT({
	getImplementSource : function(){
		var buf = [''];
		var c = this.xmlEncoder + this.entityEncoder;
		if(c){
			buf.push( "function __r__(c,e){return e||'&#'+c.charCodeAt()+';'}\n");
			if(c>3){
				this.optimizedEncoder = true;
				buf.push("function __x__(source,e){return String(source).replace(e,__r__);}\n");
			}
		}
		var df = this.dateFormat;
		if(df.hit){
var dlstart = df.isFixLen?'__dl__(':''	
var dlend = df.isFixLen?',format.length)':''	
if(dlstart)	buf.push(	"function __dl__(date,len){return len > 1? ('000'+date).slice(-len):date;}\n");
if(df.T)		buf.push(	"function __tz__(offset){return offset?(offset>0?'-':offset*=-1,'+')+__dl__(offset/60,2)+':'+__dl__(offset%60,2):'Z'}\n");
if(df)			buf.push(	"function __df__(pattern,date){\n");
if(df)			buf.push(		"date = date?new Date(date):new Date();\n");
if(df)			buf.push(        "return pattern.replace(/",
												df.qute?"'[^']+'|\"[^\"]+\"|":'',
												"([YMDhms])\\1*",
												df['.']?"|\\.s":'',
												df.T?"|TZD$":'',
												"/g,function(format){\n");
if(df)			buf.push(            "switch(format.charAt()){\n");
if(df.Y)			buf.push(            "case 'Y' :return ",dlstart,"date.getFullYear()",dlend,";\n");
if(df.M)			buf.push(            "case 'M' :return ",dlstart,"date.getMonth()+1",dlend,";\n");
if(df.D)			buf.push(            "case 'D' :return ",dlstart,"date.getDate()",dlend,";\n");
if(df.h)			buf.push(            "case 'h' :return ",dlstart,"date.getHours()",dlend,";\n");
if(df.m)			buf.push(            "case 'm' :return ",dlstart,"date.getMinutes()",dlend,";\n");
if(df.s)			buf.push(            "case 's' :return ",dlstart,"date.getSeconds()",dlend,";\n");
if(df['.'])			buf.push(            "case '.':return '.'+",dlstart,"date.getMilliseconds(),3);\n");
if(df.T)			buf.push(            "case 'T':return __tz__(date.getTimezoneOffset());\n");
if(df.qute)			buf.push(            "case '\'':case '\"':return format.slice(1,-1);\n");
if(df)				buf.push(            "default :return format;\n");
if(df)			buf.push(            "}\n");
if(df)			buf.push(        "});\n");
if(df)			buf.push(    "}\n");
		}
		if(this.entityEncoder){
			buf.push( 'var __e__ = /&(?:\w+|#\d+|#x[\da-f]+);|[<&"]/ig;\n');
		}
		return buf.join('');
	},
	createXMLEncoder : function(el,isAttr){
		var thiz= this;
		this.xmlEncoder ++;
		el = thiz.stringifyEL(el);
		return {toString:function(){
			var e = (isAttr?'/[&<\\"]/g':'/[&<]/g');
			if(thiz.optimizedEncoder){
				return '__x__('+el+','+e+')';
			}else{
				return 'String('+el+').replace('+e+',__r__)'
			}
		}}
	},
	createEntityEncoder : function(el){
		var thiz= this;
		el = thiz.stringifyEL(el);
		this.entityEncoder ++;
		return {toString:function(){
			if(thiz.optimizedEncoder){
				return '__x__('+el+',__e__)';
			}else{
				return 'String('+el+').replace(__e__,__r__)'
			}
		}}
	},
	createDateFormat : function(pattern,date){
		var thiz= this;
		var df = this.dateFormat;
		var patternSample=pattern[1];
		var maxLen = 0;
		if(pattern[0] != -1){//非常量,JSEL:VALUE_CONSTANTS
			patternSample='YYMMDDhhmmss.sTZD';
		}
		patternSample.replace(/([YMDhms])\1*|\.s|TZD/g,function(c){
			len = c.length;
			c = c.charAt();
			if(c == '"' || c== '\''){
				df.qute = 1;
			}
			maxLen = Math.max(maxLen,df[c]=Math.max(df[c]||0,len));
		})
		//变量 ，JSEL:VALUE_VAR
		df.isEL = df.isEL || date[0] != -2;
		df.isFixLen = df.isFixLen || maxLen>1;
		df.hit ++;
		pattern = thiz.stringifyEL(pattern);
		date = thiz.stringifyEL(date)
		return {toString:function(){
			return '__df__('+pattern+','+date+')';
		}}
	},
	_output:function(){
		var lastOut = this._lastOut;//不能用闭包var代替
		var lastIndex = this.out.length-1;
		if(lastOut &&  this.out[lastIndex] == lastOut){
			lastOut.data.push(arguments)
		}else{
			this.append(this._lastOut = new OutputItem(arguments));
		}
    },
	stringifyEL:function (el){
		return el?new Expression(el).toString(this):null;
	},
	parse:function(){
		var params = this.params;
		this.depth=0;
		this.out = [];
	    //add function
	    var fs = [];
	    var defs = this.scope.defs;
	    var thiz = this;
	    var defVars = []
	    for(var i=0;i<defs.length;i++){
	        var def = this.scope.defMap[defs[i]];
	        this.depth+=4;
	        this.appendCode(def.code);
	        var vars = buildVars(this,def,def.params);
	        var contents = thiz.reset();
	        defVars.push({
	        	params:def.params,
	        	defaults:def.defaults,
	        	vars:vars,
	        	name:def.name,
	        	toString:function(){
	        		return optimizeFunction(contents,this.name,this.params,this.defaults,this.vars);
	        	}})
	        this.depth-=4;
	        //fs.push(this.liteImpl,".",def.name,"=",content,",\n");
	    }
	   
	    try{
	    	this.depth++;
	        this.appendCode(this.scope.code);
	        this.depth--;
	    }catch(e){
	        //alert(["编译失败：",buf.join(""),this.scope.code])
	        throw e;
	    }
	    
		var s = this.getImplementSource();
		if(s){
			fs.push(s);
		}
	    this.header = '';
	    
	    var vars = buildVars(this,this.scope,this.params);
	    vars.unshift(fs.join(''));
	    this.body = optimizeFunction(this.reset(),this.name,this.params,this.defaults,vars.concat(defVars));
	    //this.append("return __out__.join('');");
	},
	
    appendStatic:function(item){
    	this._output(JSON.stringify(item));
    },
    appendEL:function(item){
    	this._output(this.stringifyEL(item[1]))
    },
    appendXT:function(item){
        this._output(this.createXMLEncoder(item[1]))
    },
    appendXA:function(item){
        //[7,[[0,"value"]],"attribute"]
        var el = item[1];
        var value = this.stringifyEL(el);
        var attributeName = item.length>2 && item[2];
        if(attributeName){
        	var testId = this.allocateId(value);
        	if(testId != value){
        		el = new Expression(testId).token;
            	this.append("var ",testId,"=",value);
        	}
            this.append("if(",testId,"!=null){");
            this.depth++;
            this._output(' ',attributeName,'="',this.createXMLEncoder(el,true),'"');
            this.depth--;
            this.append("}");
            this.freeId(testId);
        }else{
        	this._output(this.createXMLEncoder(el,true))
        }
    },
    appendVar:function(item){
        this.append("var ",item[2],"=",this.stringifyEL(item[1]),";");
    },
    appendCapture:function(item){
        var childCode = item[1];
        var varName = item[2];
        var bufbak = this.allocateId();
        this.append("var ",bufbak,"=__out__;__out__=[];");
        this.appendCode(childCode);
        this.append("var ",varName,"=__out__.join('');__out__=",bufbak,";");
        this.freeId(bufbak);
    },
    appendEncodePlugin:function(item){//&#233;&#0xDDS;
        this._output(this.createEntityEncoder(item[1]));
    },
    appendDatePlugin:function(pattern,date){//&#233;&#0xDDS;
    	this.dateFromat++;
        this._output(this.createDateFormat(pattern[1],date[1]))
    },
    processIf:function(code,i){
        var item = code[i];
        var childCode = item[1];
        var testEL = item[2];
        var test = this.stringifyEL(testEL);
        this.append("if(",test,"){");
        this.depth++;
        this.appendCode(childCode)
        this.depth--;
        this.append("}");
        var nextElse = code[i+1];
        var notEnd = true;
        while(nextElse && nextElse[0] == ELSE_TYPE){
            i++;
            var childCode = nextElse[1];
            var testEL = nextElse[2];
            var test = this.stringifyEL(testEL);
            if(test){
                this.append("else if(",test,"){");
            }else{
                notEnd = false;
                this.append("else{");
            }
            this.depth++;
            this.appendCode(childCode)
            this.depth--;
            this.append("}");
            nextElse = code[i+1];
        }
        return i;
    },
    getForName:function(){
    	var f = this.forStack[0];
    	return f && f[0];
    },
    getForAttribute:function(forName,forAttribute){
    	var f = this.forStack[0];
    	if(f && f[0] == forName){
			if(forAttribute == 'index'){
				return f[1];
			}else if(forAttribute == 'lastIndex'){
				return f[2];
			}
		}
    },
    processFor:function(code,i){
        var item = code[i];
        var indexId = this.allocateId();
        var lastIndexId = this.allocateId();
        var itemsId = this.allocateId();
        var itemsEL = this.stringifyEL(item[2]);
        var varNameId = item[3]; 
        //var statusNameId = item[4]; 
        var childCode = item[1];
        var forInfo = this.findForStatus(item)
        //初始化 items 开始
        this.append("var ",itemsId,'=',itemsEL,';');
        this.append("var ",indexId,"=0;")
        this.append("var ",lastIndexId," = (",
        	itemsId,'=',itemsId,' instanceof Array?',itemsId,':Object.keys(',itemsId,')'
        	,").length-1;");
        
        //初始化 for状态
        var forRef = forInfo.ref ;
        var forAttr = forInfo.index || forInfo.lastIndex;
        if(forRef){
       		var statusId = this.allocateId();
            this.forStack.unshift([statusId,indexId,lastIndexId]);
            this.append("var ",statusId," = {lastIndex:",lastIndexId,"};");
        }else if(forAttr){
            this.forStack.unshift(['for',indexId,lastIndexId]);
        }
        this.append("for(;",indexId,"<=",lastIndexId,";",indexId,"++){");
        this.depth++;
        if(forRef){
            this.append(statusId,".index=",indexId,";");
        }
        this.append("var ",varNameId,"=",itemsId,"[",indexId,"];");
        this.appendCode(childCode);
        this.depth--;
        this.append("}");
        
    	if(forRef){
    		this.freeId(statusId);
    		this.forStack.shift();
    	}else if(forAttr){
    		this.forStack.shift();
    	}
        
        this.freeId(lastIndexId);
        this.freeId(itemsId);;
        var nextElse = code[i+1];
        var notEnd = true;
        var elseIndex = 0;
        while(notEnd && nextElse && nextElse[0] == ELSE_TYPE){
            i++;
            elseIndex++;
            var childCode = nextElse[1];
            var testEL = nextElse[2];
            var test = this.stringifyEL(testEL);
            var ifstart = elseIndex >1 ?'else if' :'if';
            if(test){
                this.append(ifstart,"(!",indexId,"&&",test,"){");
            }else{
                notEnd = false;
                this.append(ifstart,"(!",indexId,"){");
            }
            this.depth++;
            this.appendCode(childCode)
            this.depth--;
            this.append("}");
            nextElse = code[i+1];
        }
        this.freeId(indexId);
        return i;
    },
    toSource:function(rtf){
    	var h = this.header;
    	var b = this.body;
    	if(h){
    		if(rtf){
    			return 'return ('+h+b+')'
    		}else{
    			var m = b.match(/^\s*function(\s+[^\s\(]+)([\s\S]+)$/)
    			if(m && m[1]){
    				return 'var '+m[1]+'= ('+h+'function '+m[2]+')'
    			}else{
    				return h + b;
    			}
    			
    		}
    	}else{
    		if(rtf){
    			return 'return '+b;
    		}else{
    			return b;
    		}
    	}
    }
});
/*
var INIT_SCRIPT = String(function(){
	var __e__ = /&(?:\w+|#\d+|#x[\da-f]+);|[<&"]/ig;
	function __x__(source,e){return String(source).replace(e,__r__);}
	function __r__(c,e){return e||'&#'+c.charCodeAt()+';'}
	
	
	function __dl__(date,format){return format == 1?date : ('000'+date).slice(-format);}
	function __tz__(offset){return offset?(offset>0?'-':offset*=-1,'+')+__dl__(offset/60,2)+':'+__dl__(offset%60,2):'Z'}
	function __df__(pattern,date){
		date = date?new Date(date):new Date();
        return pattern.replace(/([YMDhms])\1*|\.s|TZD/g,function(format){
            switch(format.charAt()){
            case 'Y' :return __dl__(date.getFullYear(),format.length);
            case 'M' :return __dl__(date.getMonth()+1,format.length);
            case 'D' :return __dl__(date.getDate(),format.length);
            case 'h' :return __dl__(date.getHours(),format.length);
            case 'm' :return __dl__(date.getMinutes(),format.length);
            case 's' :return __dl__(date.getSeconds(),format.length);
            case '.':return '.'+__dl__(date.getMilliseconds(),3);
            case 'T':return __tz__(date.getTimezoneOffset());
            }
        })
    }
}).replace(/^\s+|\s+$/g,'')+"()";
*/
if(typeof require == 'function'){
exports.JSTranslator=JSTranslator;
exports.GLOBAL_DEF_MAP=GLOBAL_DEF_MAP;
exports.GLOBAL_VAR_MAP=GLOBAL_VAR_MAP;
var Expression=require('js-el').Expression;
}