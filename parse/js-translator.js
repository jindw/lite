/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
var ELSE_TYPE=require('./template-token').ELSE_TYPE;
var TranslateContext=require('./translate-context').TranslateContext;
var Expression=require('js-el').Expression;
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
var  ID_EXP = /^[a-z\$\_][\w\$]*$/
copy(GLOBAL_DEF_MAP,GLOBAL_VAR_MAP);
/**
 * @param config {waitPromise:true,liteImpl:'liteImpl'}
 * JS原生代码翻译器实现
 */
function JSTranslator(config){
	this.config = config||{};
}
/**
 * <code>

function(__context__,__out__){
	//gen function __x__(){}
	//gen defs
	function def(p1,pe){
		return [p1,'+',p2].join('')
	}
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


	//gen output1
	//return function(){

		//gen model vars
		var $var1 = __context__.var1;
		var $var2 = __context__.var2;
		var $var3 = __context__.var3;

		__out__.push($var1.x,$var2.y);
		if($var3){
			__out__.push($var3)
		}
		__out__.push($var3)
		
		....
		return __out__.join('');
	//}()



	//gen output2
	//return function(){
		//gen model vars
		var $var1 = __context__.var1;
		var $var2 = __context__.var2;
		var $var3 = __context__.var3;
		
		var it = g();
		if($var1 instanceof Promise){$var1.then(function(v){$var1 = v});}
		if($var2 instanceof Promise){$var1.then(function(v){$var12= v});}
		function next(){
			var next = it.next();
			if(next instanceof Promise){next.then(next);}
		}
		next();

		function* g(){
			yield* __out__.wait(var1,var2);
			__out__.push(var1.x,var2.y);

			yield* __out__.wait(var3);
			if(var3){
				__out__.push(var3)
			}
			__out__.push(var3)

			//bigpiple 输出
			__out__.lazy(function* __lazy__id__(__out__){
				if(var1){
					__out__.push(var1)
				}
				__out__.push(var1)
			});
			__out__.push('<div id="__lazy__id__" style="display:block" class="lazy"></div>');
			
			....
			//return __out__.join('');//__out__.end();
		}

		
	}
}
 */
JSTranslator.prototype = {
	/**
	 * @param list
	 * @param config {name:'functionName',params:['arg1','arg2'],defaults:['arg2value']}
	 */
	translate:function(list,config){
		config = config||{};
		var params = config.params;
		var functionName = config.name||'';
		var ctx = new JSTranslateContext(list,functionName,params,config.defaults);
		
		var translatorConfig = this.config || {};
		var liteImpl = translatorConfig.liteImpl
		ctx.waitPromise = translatorConfig.waitPromise && [[]];
		ctx.hasBuildIn = !!liteImpl;
		ctx.liteImpl = liteImpl && (typeof liteImpl == 'string'?liteImpl:'liteImpl');
		ctx.parse();
		
		var functionPrefix = genFunctionPrefix(functionName)
		var code = genSource(ctx,functionPrefix);//ctx.header +  ctx.body;
		//console.log('###'+code+'@@@')
		
		try{
		    var fn = new Function('return '+code);
		    //if(params ==null|| params.length == 0) {
		    	var scope = ctx.scope;
		    	var refMap = scope.refMap;
		    	var varMap = scope.varMap;
		    	var externalRefs = Object.keys(refMap).filter(function(n){return !(n in varMap)})
		    	if(externalRefs == 0 && params){
		    		return functionPrefix+'(){return '+JSON.stringify(fn()()) + '}'
		    	}
		    //}
		    
		}catch(e){
			var error = ["invalid code:",e,'<code>'+code+'</code>'].join('');
			console.log(error)
			code = functionPrefix+'(){return '+JSON.stringify(error)+';}';
		}
		return code.replace(/<\/script>/g,'<\\/script>').replace(/^\s*[\r\n]+/,'');
	}
}
function genFunctionPrefix(functionName){
	if(functionName.match(ID_EXP)){
		return 'function '+functionName;
	}else if(functionName.match(/[\.\[]/)){
		return functionName+'=function'
	}else{
		return 'function'
	}
	
}
function genSource(ctx,functionPrefix){
	var header = ctx.header;
	var body = ctx.body;
	var params = ctx.params
	var args = params?params.join(','):'__context__,__out__';
	var result = [functionPrefix,"(",args,'){\n',header,'\n']
    if (ctx.waitPromise) {
    	result.push("\t__g__ = __g__();",
			"function __n__(){",
				"var n = __g__.next().value;",
				"if(n instanceof Promise){",
					"n.then(__n__);console.log('is promise',n)",
				//"}else{",
					//"console.log('is not promise',n)",
					//"__n__()",
				"}",
			"};__n__();\n");
		result.push('\tfunction* __g__(){\n',body,'\n\treturn __out__.join("");\n\t}\n}\n');
	}else{
		if(params){
			var m = body.match(/^\s*__out__\.push\((.*?)\);?\s*$/)
			if(m){
				var item = '\treturn ['+m[1]+']'
				try{
					new Function(item)
					if(item.indexOf(',')>0){
						result.push(item,'.join("");\n}')
					}else{
						result.push('\treturn ',m[1],';\n}');
					}
					
					return result.join('');
				}catch(e){}
				
			}
			result.push('\tvar __out__ = [];\n');
		}else{
			result.push('\tvar __out__ = __out__||[];\n');
		}
		result.push(body,'\n\treturn __out__.join("");\n}\n');
	}
	return result.join('');
}


/**
 * 增加默认参数值支持。defaults
 */
function genDecFunction(contents,functionName,params,defaults,modelVarsDec){
	var modelVarsDecAndParams = modelVarsDec.concat();
	//生成参数表
	var args = params?params.join(','):'__context__';
	if(params && defaults && defaults.length){
		//处理默认参数
		modelVarsDecAndParams.push('\tswitch(arguments.length){\n');
		var begin = params.length - defaults.length
		for(var i =0;i<params.length;i++){
			modelVarsDecAndParams.push('\t	case ',i,':\n');
			if(i>=begin){
				modelVarsDecAndParams.push('\t	',params[i],'=',JSON.stringify(defaults[i-begin]),';\n');
			}
		}
		modelVarsDecAndParams.push('\t}\n');
	}

	//优化内容（合并join 串）
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
		source = "\tvar __out__=[]\n"+source.replace(/^\s*\n|\s*\n\s*$/g,'')+"\n\treturn __out__.join('');\n";
	}
	return 'function '+functionName+"("+args+'){\n'+modelVarsDecAndParams.join('')+source.replace(/^[\r\n]+/,'')+'\n}\n'
}

function genModelDecVars(ctx,scope,params){
	var result = [];
	var map = {};
	var refMap = scope.externalRefMap;
	var callMap = scope.callMap;

	var varMap = scope.varMap;
	var paramMap = scope.paramMap;
	//console.log('genModelDecVars::::',params)
	copy(refMap,map);
	//copy(callMap,map);
	var vars = [];
	for(var n in map){
		if(n != '*' && !((n in GLOBAL_VAR_MAP)|| (n in varMap) || (n in paramMap))){
			if(params){//no __context__
				//result.push('\tvar ',n,'=',ctx.liteImpl,'["',n,'"];\n');
			}else{
				//result.push('\tvar ',n,'=("',n,'" in __context__? __context__:',ctx.liteImpl,')["',n,'"];\n');
				result.push('\tvar ',n,'=("',n,'" in __context__?__context__:this)["',n,'"];\n');
				vars.push(n);
			}
			
		}
	}
	if(!params && ctx.waitPromise){
		result.push(vars.join('\n').replace(/.+/mg,'\tif($& instanceof Promise){$&.then(function(v){$& = v});}'),'\n');
	}
	

	/**
	 	
		if($var1 instanceof Promise){$var1.then(function(v){$var1 = v});}
		if($var2 instanceof Promise){$var1.then(function(v){$var12= v});}
		
		
	 */
	return result;
}
/**
 * 构建内容头部
 */
function genBuildInSource(ctx){
	if(ctx.hasBuildIn){return ''}
	var buf = [''];
	var c = ctx.xmlEncoder + ctx.entityEncoder*2;
	if(c){
		if(c>3){
			ctx.optimizedEncoder = true;
			buf.push("	function __x__(source,e){return String(source).replace(e||/&(?!#\\d+;|#x[\\da-f]+;|[a-z]+;)|[<\"]/ig,function(c){return '&#'+c.charCodeAt()+';'});}\n");
		}else{
			buf.push(" 	function __r__(c){return '&#'+c.charCodeAt()+';'}\n");
		}
	}
	
	if(ctx.safeGetter){
		buf.push('	function __get__(o,p,a){try{return a?o[p].apply(o,a):o[p]}catch(e){return e}}\n')
	}
	//if(ctx.entityEncoder){buf.push( 'var __e__ = __x__;\n');}
	if(ctx.forStack.hit){
		//ie7,ie8
		buf.push("	if(!Object.keys)Object.keys=function(o){var r=[];for(var n in o){r.push(n)};return r;};\n")
	}
	
	var df = ctx.dateFormat;
	if(df.hit){
var dlstart = df.isFixLen?'__dl__(':''	
var dlend = df.isFixLen?',format.length)':''	
if(dlstart)	buf.push("	function __dl__(date,len){return len > 1? ('000'+date).slice(-len):date;}\n");
if(df.T)		buf.push("	function __tz__(offset){return offset?(offset>0?'-':offset*=-1,'+')+__dl__(offset/60,2)+':'+__dl__(offset%60,2):'Z'}\n");
if(df)			buf.push("	function __df__(pattern,date){\n");
if(df)			buf.push("		date = date?new Date(date):new Date();\n");
if(df)			buf.push("	        return pattern.replace(/",
												df.qute?"'[^']+'|\"[^\"]+\"|":'',
												"([YMDhms])\\1*",
												df['.']?"|\\.s":'',
												df.T?"|TZD$":'',
												"/g,function(format){\n");
if(df)			buf.push("	            switch(format.charAt()){\n");
if(df.Y)			buf.push("	            case 'Y' :return ",dlstart,"date.getFullYear()",dlend,";\n");
if(df.M)			buf.push("	            case 'M' :return ",dlstart,"date.getMonth()+1",dlend,";\n");
if(df.D)			buf.push("	            case 'D' :return ",dlstart,"date.getDate()",dlend,";\n");
if(df.h)			buf.push("	            case 'h' :return ",dlstart,"date.getHours()",dlend,";\n");
if(df.m)			buf.push("	            case 'm' :return ",dlstart,"date.getMinutes()",dlend,";\n");
if(df.s)			buf.push("	            case 's' :return ",dlstart,"date.getSeconds()",dlend,";\n");
if(df['.'])			buf.push("	            case '.':return '.'+",dlstart,"date.getMilliseconds(),3);\n");
if(df.T)			buf.push("	            case 'T':return __tz__(date.getTimezoneOffset());\n");
if(df.qute)			buf.push("	            case '\'':case '\"':return format.slice(1,-1);\n");
if(df)				buf.push("	            default :return format;\n");
if(df)			buf.push("	            }\n");
if(df)			buf.push("	        });\n");
if(df)			buf.push("	    }\n");
	}
	return buf.join('');
}


function createDateFormat(ctx,pattern,date){
	var df = ctx.dateFormat;
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
	pattern = ctx.stringifyEL(pattern);
	date = ctx.stringifyEL(date)
	return {toString:function(){
		return '__df__('+pattern+','+date+')';
	}}
}

function JSTranslateContext(code,name,params,defaults){
    TranslateContext.call(this,code,name,params,defaults);
    this.forStack = [];
    this.defaults = defaults;
    
	this.xmlEncoder = 0;
	this.entityEncoder=0;
	this.dateFormat = {hit:0};
	this.safeGetter = {hit:0}
}
JSTranslateContext.prototype = new TranslateContext();


JSTranslateContext.prototype.parse=function(){
	var params = this.params;
	this.out = [];
    //add function
    var defs = this.scope.defs;
    var thiz = this;
    var defVars = []
    //生成函数定义
    var waitPromise = this.waitPromise;
    this.waitPromise = null;
    for(var i=0;i<defs.length;i++){
        var def = this.scope.defMap[defs[i]];
        this.outputIndent=1;
        this.appendCode(def.code);
        var vars = genModelDecVars(this,def,def.params);
        var contents = thiz.reset();

        //添加一个函数
        defVars.push({
        	params:def.params,
        	defaults:def.defaults,
        	vars:vars,
        	name:def.name,
        	contents:contents,
        	toString:function(){
        		var fn = genDecFunction(this.contents,this.name,this.params,this.defaults,[]);
        		return String(fn).replace(/^(.)/mg,'\t$1');
        	}});
    }
    this.waitPromise = waitPromise;
    try{
    	this.outputIndent=0;
    	this.outputIndent++;
        this.appendCode(this.scope.code);
        this.outputIndent--;
    }catch(e){
        //alert(["编译失败：",buf.join(""),this.scope.code])
        throw e;
    }
    
    //放在后面，这时 如下信息是正确的！
	// this.xmlEncoder = 0;
	//this.entityEncoder=0;
	//this.dateFormat = {hit:0};
	//this.forStack.hit = true
	var  headers = [];
    var headers = genModelDecVars(this,this.scope,this.params);
	var buildIn = genBuildInSource(this);
	if(buildIn){
		headers.unshift(buildIn);
	}
    this.header = headers.concat(defVars).join('');
    //vars.unshift(fs.join(''));
    this.body = this.reset().join('').replace(/^[\r\n]+/,'')////;optimizeFunction(this.reset(),this.name,this.params,this.defaults,vars.concat(defVars));
}

JSTranslateContext.prototype.appendStatic = function(item){
	appendOutput(this,JSON.stringify(item));
}
JSTranslateContext.prototype.appendEL=function(item){
	appendOutput(this,this.stringifyEL(item[1]))
}
JSTranslateContext.prototype.appendXT=function(item){
    appendOutput(this,createXMLEncoder(this,item[1]))
}
JSTranslateContext.prototype.appendXA=function(item){
    //[7,[[0,"value"]],"attribute"]
    var el = item[1];
    var value = this.stringifyEL(el);
    var attributeName = item.length>2 && item[2];
    if(attributeName){
    	var testId = this.allocateId(value);
    	if(testId != value){
    		el = new Expression(testId).token;
        	this.append("var ",testId,"=",value,';');
    	}
        this.append("if(",testId,"!=null){");
        this.pushBlock();
        appendOutput(this,'" '+attributeName+'=\'"',createXMLEncoder(this,el,true),"\"'\"");
        //appendOutput(this,"' "+attributeName+"=\"'",createXMLEncoder(this,el,true),"'\"'");
        this.popBlock();
        this.append("}");
        this.freeId(testId);
    }else{
    	appendOutput(this,createXMLEncoder(this,el,true))
    }
}
JSTranslateContext.prototype.appendVar=function(item){
    this.append("var ",item[2],"=",this.stringifyEL(item[1]),";");
},
JSTranslateContext.prototype.appendEncodePlugin=function(item){//&#233;&#0xDDS;
    appendOutput(this,createEntityEncoder(this,item[1]));
},
JSTranslateContext.prototype.appendDatePlugin=function(pattern,date){//&#233;&#0xDDS;
    appendOutput(this,createDateFormat(this,pattern[1],date[1]))
}
JSTranslateContext.prototype.processCapture = function(item){
    var childCode = item[1];
    if(childCode.length == 1 && childCode[0].constructor == String){
    	item[1] = JSON.stringify(childCode[0]);
    	this.appendVar(item);
    }else{
    	var varName = item[2];
    	var bufbak = this.allocateId();
    	this.append("var ",bufbak,"=__out__;__out__=[];__out__.wait=",bufbak,'.wait;');
    
    	this.appendCode(childCode);
    	this.append("var ",varName,"=__out__.join('');__out__=",bufbak,";");
    	this.freeId(bufbak);
    }
},
JSTranslateContext.prototype.processIf=function(code,i){
    var item = code[i];
    var childCode = item[1];
    var testEL = item[2];
    var test = this.stringifyEL(testEL);
    //var wel = genWaitEL(this,testEL);visited el intercept function call
    //this.append('if(',wel?'('+wel+')||('+test+')':test,'){');
    this.append('if(',test,'){');
    this.pushBlock();
    this.appendCode(childCode)
    this.popBlock();
    this.append("}");
    var nextElse = code[i+1];
    var notEnd = true;
    this.pushBlock(true);
    while(nextElse && nextElse[0] == ELSE_TYPE){
        i++;
        var childCode = nextElse[1];
        var testEL = nextElse[2];
        var test = this.stringifyEL(testEL);
        
        if(test){
        	var wel = genWaitEL(this,testEL);
            this.append('else if(',wel?'('+wel+')||('+test+')':test,'){');
        }else{
            notEnd = false;
            this.append("else{");
        }
        this.pushBlock();
        this.appendCode(childCode)
        this.popBlock();
        this.append("}");
        nextElse = code[i+1];
    }
    this.popBlock(true);
    return i;
}
JSTranslateContext.prototype.processFor=function(code,i){
	this.forStack.hit = true;
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
    	itemsId,'=',itemsId,' instanceof Array?',itemsId,':typeof ',itemsId,' == "object"? Object.keys(',itemsId,'):Array(',itemsId,')'
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
    this.pushBlock();
    if(forRef){
        this.append(statusId,".index=",indexId,";");
    }
    this.append("var ",varNameId,"=",itemsId,"[",indexId,"];");
    this.appendCode(childCode);
    this.popBlock();
    this.append("}");
    
    var nextElse = code[i+1];
    var notEnd = true;
    var elseIndex = 0;
    this.pushBlock(true);
    while(notEnd && nextElse && nextElse[0] == ELSE_TYPE){
        i++;
        elseIndex++;
        var childCode = nextElse[1];
        var testEL = nextElse[2];
        var test = this.stringifyEL(testEL);
        var ifstart = elseIndex >1 ?'else if' :'if';
        if(test){
        	var wel = genWaitEL(this,testEL);
            this.append(ifstart,
            	'(',wel?'('+wel+')|| !':'!'
            			,indexId,'&&(',test,')){');
        }else{
            notEnd = false;
            this.append(ifstart,"(!",indexId,"){");
        }
        this.pushBlock();
        this.appendCode(childCode)
        this.popBlock();
        this.append("}");
        nextElse = code[i+1];
    }
    this.popBlock(true);
    
	if(forRef){
		this.freeId(statusId);
		this.forStack.shift();
	}else if(forAttr){
		this.forStack.shift();
	}
    this.freeId(lastIndexId);
    this.freeId(itemsId);;
    this.freeId(indexId);
    return i;
}
JSTranslateContext.prototype.pushBlock = function(ignoreIndent){
	if(!ignoreIndent){
		this.outputIndent++
	}
	var waitPromise = this.waitPromise;
	if(waitPromise){
		var topStatus = waitPromise[waitPromise.length-1]
		waitPromise.push(topStatus?topStatus.concat():[])
	}
}
JSTranslateContext.prototype.popBlock = function(ignoreIndent){
	if(!ignoreIndent){
		this.outputIndent--;
	}
	if(this.waitPromise){
		this.waitPromise.pop()
	}
}

JSTranslateContext.prototype.appendModulePlugin = function(child,config){
	if(this.waitPromise){
		this.append('__out__.lazy(function* __lazy_widget_',config.id,'__(__out__){');
		this.pushBlock();//TODO:lazy push, 最后执行的元素可以最后检测waitEL
		this.appendCode(child)
		this.popBlock();
		this.append('})');
	}else{
		this.appendCode(child)
	}
}
JSTranslateContext.prototype.stringifyEL= function (el){
	return el?new Expression(el).toString(this):null;
};

JSTranslateContext.prototype.visitEL= function (el,type){
	el = el && genWaitEL(this,el);
	el && this.append(el);
};

JSTranslateContext.prototype.getVarName = function(name){
	if(name == 'for'){
		console.error('invalue getVarName:')
		//throw new Error()
	}
	return name;
};
JSTranslateContext.prototype.getForName = function(){
	var f = this.forStack[0];
	//console.log('getForName:',f[0])
	return f && f[0];
};
JSTranslateContext.prototype.genGetCode = function(owner,property){

	//safe
	if(this.safeGetter){
		this.safeGetter.hit = true;
		return '__get__('+owner+','+property+')'
	}else{
		//fast
		if(/^"[a-zA-Z_\$][_\$\w]*"$/.test(property)){
			return owner+'.'+property.slice(1,-1);
		}else{
			return owner+'['+property+']';
		}
	}
};

JSTranslateContext.prototype.findForAttribute= function(forName,forAttribute){
	var stack = this.forStack;
	var index = forAttribute == 'index'?1:(forAttribute == 'lastIndex'?2:0);
	for(var i=0;index && i<stack.length;i++){
		var s = stack[i];
		if(s && s[0] == forName){
			return s[index];
		}
	}
}

function genWaitEL(ctx,el){
	if(ctx.waitPromise){
		var topWaitedVars = ctx.waitPromise[ctx.waitPromise.length-1];
		if(topWaitedVars){
		    var vars = Object.keys(new Expression(el).getVarMap());
		    var vars2 = [];
		    for(var i=0;i<vars.length;i++){
		    	var v = vars[i];
		    	if(v != 'for' && topWaitedVars.indexOf(v)<0){
		    		vars2.push(v)
		    		topWaitedVars.push(v)
				}
		
		    }
		    if (vars2.length) {
		    	return 'yield* __out__.wait('+vars2.join(',')+')'
		    };
		}
	}
    
}
function appendOutput(ctx){
	var outList = ctx.out;
	var lastOutGroup = ctx._lastOutGroup;//不能用闭包var代替
	var lastIndex = outList.length-1;
	var args = outList.splice.call(arguments,1);
	if(lastOutGroup &&  outList[lastIndex] === lastOutGroup){
		lastOutGroup.list.push.apply(lastOutGroup.list,args)
	}else{
		ctx.append(ctx._lastOutGroup = new OutputGroup(args));
	}
}

function OutputGroup(args){
	this.list = args;
}
OutputGroup.prototype.toString = function(){
	return '__out__.push(' + this.list.join(',')+');'
}

function createXMLEncoder(thiz,el,isAttr){
	thiz.xmlEncoder ++;
	el = thiz.stringifyEL(el);
	return {toString:function(){
		var e = (isAttr?"/[&<']/g":'/[&<]/g');
		if(thiz.optimizedEncoder||thiz.hasBuildIn){
			return '__x__('+el+','+e+')';
		}else{
			return 'String('+el+').replace('+e+',__r__)'
		}
	}}
}
function createEntityEncoder(thiz,el){
	el = thiz.stringifyEL(el);
	thiz.entityEncoder ++;
	return {
		toString:function(){
		if(thiz.optimizedEncoder || thiz.hasBuildIn){
			return '__x__('+el+')';
		}else{
			return 'String('+el+').replace(/&(?!#\\d+;|#x[\\da-f]+;|[a-z]+;)|[<"]/ig,__r__)'
		}
	}}
}



function copy(source,target){
	for(var n in source){
		target[n] = source[n];
	}
}
exports.JSTranslator=JSTranslator;
exports.GLOBAL_DEF_MAP=GLOBAL_DEF_MAP;
exports.GLOBAL_VAR_MAP=GLOBAL_VAR_MAP;
