/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
//lite__def/lite__g/
//lite__list
//lite__encode
//lite__init(0,name,fn);//set
//lite__init(1,name,context);//get
//lite__init(2,obj);//encode
//lite__init(3,list);//tolist
var FOR_STATUS_KEY = '$__for';
var INIT_SCRIPT = String(function(){
	var lite__impl_get;
	var lite__impl_def = function(g,p){
    	lite__impl_get = function(n,c){
			return (c && n in c ? c:n in g?g:this)[n];
		};
		g[1] = function(source,result,type) {
			if(source instanceof Array){
				return source;
			}
			var result = [];
			if(typeof source == 'number'){
				source = parseInt(source);
				while(source >0){
					result[--source] = source+1;
				}
			}else{
				for(source in source){
					result.push(source);
 				}
			}
			return result;
    	};
		var map = {'"':'&#34;','<':'&lt;','&':'&#38;'};
		function replacer(c){return map[c]||c}
		//xt:0,xa:1,xp:2
		g[0] = function(txt,attr){
//			if(type){
//				type = type == 1?/[<&]/g:/(&(?:[a-z]+|#\d+|#0x[\da-f]+);)|[<&"]/ig;
//			}else{
//				type = /[<&"]/g;
//			}
//			type = ;
			return String(txt).replace(
				attr?/&(?:\w+|#\d+|#x[\da-f]+);|[<&"]/ig:/&(?:\w+|#\d+|#0x[\da-f]+);|[<&]/ig
				,replacer);
		};
		return function(n,fn){
    		g[n]=fn;
    	};
	}({});
	with({}){
		alert(lite__impl_def,lite__impl_get);
	}

}).replace(/^[^{]+\{\s*|\bwith\b[\s\S]*$/g,'');//.replace(/\s*([^\w_$\s]+)\s*/g,'$1')
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
	    try{
	    	//var result =  stringifyJSON(context.toList())
	        //var list = context.toList();
		    var context = new JSTranslateContext(list,this.name,this.params,this.defaults);
		    context.liteImpl = this.liteImpl || "lite__impl_";
		    context.parse();
		    if(rtf){
		    	var code = context.header + "\nreturn "+ context.body;
		    }else{
		    	var code = context.toSource();//context.header +  context.body;
		    }
		    if(!this.name && !rtf){
		    	new Function('return '+code);
		    }else{
		    	new Function(code);
		    }
	    }catch(e){
	    	var error = $log.error("生成js代码失败:",e,code);
	        code = "return ("+stringifyJSON(error)+');';
	    }
		var result = [];
    	if(!this.liteImpl){
    		result.push("if(!window.",context.liteImpl,"def){",INIT_SCRIPT,"}");
    	}
	    result.push('\n',code.replace(/<\/script>/g,'<\\/script>'));
	    return result.join("");
	}
}
/**
 * <code>
if(!window.lite__def){
	${INIT_SCRIPT}
}
lite__def('add',function add(a,b){retur a+b});
function($_context){
	var var1 = lite__init('var1',$_context);
	var var2 = lite__init('var2',$_context);
	var var3 = lite__init('var3',$_context);
	....
}
 */
function JSTranslateContext(code,name,params,defaults){
    TranslateContext.call(this,code,name,params,defaults);
    this.forStack = [];
    this.defaults = defaults;
    this.impl_counter = {list:0,x:0};
}
var GLOBAL_VAR_MAP ={
	"JSON":1,
	"Math":1
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

function copy(source,target){
	for(var n in source){
		target[n] = source[n];
	}
}

function optimizeFunction(context,functionName,params,defaults,result){
	var text = context.reset();
	var args = '$_context';
	if(params){
		args = params.join(',');
	}
	//(a,b,c=3,d=4)
	//switch(arguments.length){
	//  case 0:
	//	case 1:
	//	case 2
	//		c=3;
	//	case 3
	//		d=4
	//	case 4
	//	default
	//}
	if(defaults && defaults.length){
		result.push('\tswitch(arguments.length){\n');
		var begin = params.length - defaults.length
		for(var i =0;i<params.length;i++){
			result.push('\t	case ',i,':\n');
			if(i>=begin){
				result.push('\t	',params[i],'=',stringifyJSON(defaults[i-begin]),';\n');
			}
		}
		result.push('\t}\n');
	}
	var SP = /^\s*\$_out\.push\((?:(.*)\)\s*;?)\s*$/g;
	if(SP.test(text)){
		var c  =text.replace(SP,'$1');
		if(c.indexOf(',')>0){
			//安全方式吧.
			text = "\treturn ["+c+"].join('');";
		}else{
			text = "\treturn "+c+';';
		}
	}else{
		text = "\tvar $_out=[]\n"+text+"\n\treturn $_out.join('');\n";
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
    return functionName+"("+args+'){\n'+result.join('')+text.replace(/^[\r\n]+/,'')+'\n}';
}
function buildVars(context,refMap,callMap,params){
	var result = [];
	var map = {};
	copy(refMap,map);
	copy(callMap,map);
	for(var n in map){
		if(!((n in GLOBAL_VAR_MAP) ||( n in GLOBAL_DEF_MAP))){
			result.push('\tvar ',n,'=',context.liteImpl,'get("',n,'"',(params?'':',$_context'),');\n');
		}
	}
	if(context.impl_counter.list){
		result.push('\tvar ',context.liteImpl,'list=',context.liteImpl,'get(1);\n');
	}
	if(context.impl_counter.x){
		result.push('\tvar ',context.liteImpl,'x=',context.liteImpl,'get(0);\n');
	}
	return result;
}
function PT(pt){
	for(var n in pt){
		this[n]=pt[n];
	}
}


PT.prototype = TranslateContext.prototype
JSTranslateContext.prototype = new PT({
	_appendOutput:function(){
    	var len = arguments.length;
    	var data = Array.prototype.join.call(arguments,'');
    	var lastOut = this._lastOut;//不能用闭包var代替
    	var lastIndex = this.out.length-1;
    	if(lastOut &&  this.out[lastIndex] == lastOut){
    		data = lastOut.substring(0,lastOut.length-2)+","+data+");";
    		this.out[lastIndex] = data;
    	}else{
    		data = "$_out.push("+data+");";
    		this.append(data);
    	}
    	this._lastOut = data
    },
	stringifyEL:function (el){
		return el?stringifyJSEL(el,this):null;
	},
	parse:function(){
		var code = this.code;
		var params = this.params;
		this.depth=0;
		this.out = [];
	    //add function
	    var fs = [];
	    var defs = this.scope.defs;
	    for(var i=0;i<defs.length;i++){
	        var def = defs[i];
	        var n = def.name;
	        var refMap = def.externalRefMap;
	        var callMap = def.callMap;
	        this.depth++;
	        this.appendCode(def.code);
	        var vars = buildVars(this,refMap,callMap,def.params);
	        var content = optimizeFunction(this,'',def.params,def.defaults,vars);
	        this.depth--;
	        fs.push(this.liteImpl,"def('",n,"',",content,");\n");
	        this.impl_counter = {list:0,x:0};
	    }
	    this.header = fs.join('');
	    
	    try{
	    	this.depth++;
	        this.appendCode(code);
	        this.depth--;
	    }catch(e){
	        //alert(["编译失败：",buf.join(""),code])
	        throw e;
	    }
	    var refMap = this.scope.externalRefMap;
	    var callMap =this.scope.callMap;
	    
	    var vars = buildVars(this,refMap,callMap,this.params);
	    this.body = optimizeFunction(this,this.name,this.params,this.defaults,vars);
	    //this.append("return $_out.join('');");
	},
	
    appendStatic:function(item){
    	this._appendOutput(stringifyJSON(item));
    },
    appendEL:function(item){
    	this._appendOutput(this.stringifyEL(item[1]))
    },
    appendXT:function(item){
    	this.impl_counter.x++;
        this._appendOutput(this.liteImpl,"x(",this.stringifyEL(item[1]),")")
    },
    appendXA:function(item){
    	this.impl_counter.x++;
        //[7,[[0,"value"]],"attribute"]
        var el = item[1];
        var value = this.stringifyEL(el);
        var attributeName = item.length>2 && item[2];
        if(attributeName){
        	var testId = this.allocateId(value);
        	if(testId != value){
            	this.append("var ",testId,"=",value);
        	}
            this.append("if(",testId,"!=null){");
            this.depth++;
            this._appendOutput("' ",attributeName,"=\"',",this.liteImpl,"x("+testId+",1),'\"'");
            this.depth--;
            this.append("}");
            this.freeId(testId);
        }else{
        	this._appendOutput(this.liteImpl,"x(",value,",1)")
        }
    },
    appendVar:function(item){
        this.append("var ",item[2],"=",this.stringifyEL(item[1]),";");
    },
    appendCapture:function(item){
        var childCode = item[1];
        var varName = item[2];
        var bufbak = this.allocateId();
        this.append("var ",bufbak,"=$_out;$_out=[];");
        this.appendCode(childCode);
        this.append("var ",varName,"=$_out.join('');$_out=",bufbak,";");
        this.freeId(bufbak);
    },
    appendEncodePlugin:function(item){//&#233;&#0xDDS;
    	this.impl_counter.x++;
        this._appendOutput(this.liteImpl,'x(',this.stringifyEL(item[1]),',2)')
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
    	this.impl_counter.list++;
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
        this.append("var ",itemsId,'=',this.liteImpl,"list(",itemsEL,")");
        this.append("var ",indexId,"=0;")
        this.append("var ",lastIndexId," = ",itemsId,".length-1;");
        
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
    toSource:function(){
    	return this.header+this.body
    }
});
