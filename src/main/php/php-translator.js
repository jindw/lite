if(typeof require == 'function'){
var XA_TYPE=require('../js/parse/template-token').XA_TYPE;
var ELSE_TYPE=require('../js/parse/template-token').ELSE_TYPE;
var EL_TYPE=require('../js/parse/template-token').EL_TYPE;
var XT_TYPE=require('../js/parse/template-token').XT_TYPE;

var TranslateContext=require('../js/parse/translate-context').TranslateContext;
var getELType=require('js-el/src/main/js/expression-token').getELType;
var TYPE_ANY=require('js-el/src/main/js/expression-token').TYPE_ANY;
var TYPE_BOOLEAN=require('js-el/src/main/js/expression-token').TYPE_BOOLEAN;
var TYPE_NULL=require('js-el/src/main/js/expression-token').TYPE_NULL;
var TYPE_NUMBER=require('js-el/src/main/js/expression-token').TYPE_NUMBER;

var GLOBAL_DEF_MAP=require('../js/parse/js-translator').GLOBAL_DEF_MAP;
var GLOBAL_VAR_MAP=require('../js/parse/js-translator').GLOBAL_VAR_MAP;
}/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

/**
 * 将Lite中间代码转化为直接的php代码
 * 
 * function index_xhtml_php($__engine,$__context){
 * 	$encodeURIComponent = 'lite_encodeURIComponent';	
 * 	$decodeURIComponent = 'lite_decodeURIComponent';	
 *  $key = null;
 *  $key2 = null;
 *  $test = 'index_xhtml_php__test';
 *  extract($__context);
 *  
 *   
 * }
 * function index_xhtml_php__test($__engine,$arg1,$arg2){
 *  	
 * }
 */

var FOR_STATUS_KEY = '$__for';
var VAR_LITE_TEMP="$__tmp";

//function checkEL(el){
//    new Function("return "+el)
//}

/**
 * JS原生代码翻译器实现
 * @param config {waitPromise:false,liteImpl:'liteImpl'}
 */
function PHPTranslator(config){
	this.waitPromise = config.waitPromise;
	//TODO: replace 'lite__**' to config.liteImpl**
	this.liteImpl = config.liteImpl||'lite__'
}

PHPTranslator.prototype = {
	/**
	 * 
	 * @param config {name:'functionName',
	 * 			//params:['arg1','arg2'],defaults:['arg2value']
	 * 			encoding:'utf-8',
	 * 			contentType:'text/html',
	 * 			i18n:{},
	 * 			resources:['/path1.xhtml','/path2.xhtml']
	 * 		}
	 */
	translate:function(list,config){
	    //var result =  JSON.stringify(context.toList())
		var context = new PHPTranslateContext(list||this.code,config.name);
		context.waitPromise = this.waitPromise;
		context.htmlspecialcharsEncoding = context.encoding =  config.encoding ||"UTF-8";
		context.contentType =  config.contentType;
		
		//i18n config
		context.i18n = config.i18n
		//for i18n find config
		context.resources = config.resources;
		
		context.parse();
		var code = context.toSource();
		return '<?php'+code ;
	}
}
function PHPTranslateContext(code,id){
    TranslateContext.call(this,code);
    this.id = id;
}
function TCP(pt){
	for(var n in pt){
		this[n] = pt[n];
	}
}
TCP.prototype = TranslateContext.prototype;
function toArgList(params,defaults){
	if(params.length){
		if(defaults && defaults.length){
			params = params.concat();
			var i = params.length;
			var j = defaults.length;
			while(j--){
				params[--i] += '='+stringifyPHP(defaults[j]);
			}
		}
		return '$'+params.join(',$')
	}else{
		return '';
	}
}
function _stringifyPHPLineArgs(line){//.*[\r\n]*
	var endrn="'";
	line = line.replace(/['\\]|(\?>)|([\r\n]+$)|[\r\n]/gm,function(a,pend,lend){
		if(lend){
			endrn  = '';
			return "',"+JSON.stringify(a);
		}else if(pend){
			return "?','>";
		}else{//'\\
			if(a == '\\'){
				return '\\\\';
			}else if(a == "'"){
				return "\\'";
			}else{
				console.error("非法输出行!!"+JSON.stringify(line));
			}
			return a == '\\'?'\\\\': "\\'";
		}
	});
	line = "'"+line+endrn;
	if("''," == line.substring(0,3)){
		line = line.substring(3)
	}
	return line;
}

function _encodeEL(text,model,encoding){
	//TODO: check el type
	if(model == -1){
		var encode = "htmlspecialchars("+text+",ENT_COMPAT,"+encoding+",false)";
		//this.append(prefix,"strtr(",el,",array('<'=>'&lt;','\"'=>'&#34;'));");
	}else if(model == XA_TYPE){
		var encode = "htmlspecialchars("+text+",ENT_COMPAT,"+encoding+')';
	}else if(model == XT_TYPE){
		//ENT_COMPAT 
		var encode = "htmlspecialchars("+text+",ENT_NOQUOTES,"+encoding+')';
	}else{
		var encode = text;
	}
	return encode;
}

function _appendFunctionName(context,scope){
	for(var n in scope.refMap){
		if(!(n in scope.varMap || n in scope.paramMap)){
			if(n in GLOBAL_DEF_MAP){
				context.append('$',n,"='",n,"';");
			}else if(n in GLOBAL_VAR_MAP){
			}else{
				context.append('$',n,"=function_exists('lite__",n,"')?'",n,"':null;");
			}
		}
	}
}
PHPTranslateContext.prototype = new TCP({
	stringifyEL:function (el){
		return el?stringifyPHPEL(el,this):null;
	},
	parse:function(){
		this.depth = 0;
		this.out = [];
	    //add function
	    var defs = this.scope.defs;
	    
	    for(var i=0;i<defs.length;i++){
	        var def = this.scope.defMap[defs[i]];
	        var n = def.name;
	        this.append("if(!function_exists('lite__",n,"')){function lite__",
	        		n,"(",toArgList(def.params,def.defaults),'){')
	        this.depth++;
	        this.append("ob_start();");
	        _appendFunctionName(this,def);
	        this.appendCode(def.code);
    		this.append("$rtv= ob_get_contents();ob_end_clean();return $rtv;");
	        this.depth--;
	        this.append("}}");
	    }
	    try{
	        this.append("function lite_template",this.id,'($__context__){')
	        this.depth++;
			if(this.contentType){
				this.append("if(!headers_sent())header('ContentType:"+this.contentType+"');")
			}
			this.append("mb_internal_encoding('"+this.encoding+"');")
			_appendFunctionName(this,this.scope);
			
			this.append("extract($__context__,EXTR_OVERWRITE);");
			if(this.i18n && this.resources){
				var i18ncode = new Function("return "+this.i18n)();
				var resources = this.resources;
				var resourceMap = {};
				var resourceList = [];
				for(var i=0;i<resources.length;i++){
					resourceMap[i18nHash(resources[i],'_').slice(0,-1)] = resources[i]
				}
				//console.warn(resources,resourceMap)
				for(var n in i18ncode){
					n = n.substring(0,n.indexOf('__')+2);
					if(n in resourceMap){
						resourceList.push(n.slice(0,-2));
						delete resourceMap[n];
					}
				}
				this.append("$I18N = "+stringifyPHP(resourceList).replace(/^array/,'lite_i18n')+";")
				this.append("$I18N = array_merge("+stringifyPHP(i18ncode)+",$I18N);")
			}
	        this.appendCode(this.scope.code);
	        if(this.__lazy_module_){
    			this.append('lite_lazy_block($__lazy_module_);');
    		}
	        this.depth--;
	        this.append("}");
	    }catch(e){
	        //alert(["编译失败：",buf.join(""),code])
	        console.error("PHP编译失败:"+this.id,e);
	        throw e;
	    }
	    //this.append("return _$out.join('');");
	},
	appendStatic:function(value){
		//return this.append("?>"+value+"<?php");
		var lines = value.match(/.+[\r\n]*|[\r\n]+/g);
		for(var i=0; i<lines.length; i++) {
			var line = lines[i];
			var start = i==0?'echo ':'\t,'
			var end = i == lines.length-1?';':'';
			line = _stringifyPHPLineArgs(line);
			this.append(start,line,end);
		}
	},
    _appendEL:function(el,model,text,prefix){
    	var encoding = "'"+this.htmlspecialcharsEncoding+"'";
    	prefix = prefix!=null? prefix : 'echo '
    	//@see http://notownme.javaeye.com/blog/335036
    	var text = text || this.stringifyEL(el);
    	var type = getELType(el);
    	//null,boolean
    	
		if(isSimplePHPEL(text)){//var encode = 
			var initText = text;
			var tmpId = text;
		}else{
			tmpId = VAR_LITE_TEMP;
			initText = '('+tmpId+'='+text+')';
		}
    	if(type != TYPE_ANY){
	    	if(type == TYPE_NULL){
	    		this.append(prefix,"'null';");
	    		return;
	    	}else if(type == TYPE_BOOLEAN){
	    		this.append(prefix,text,"?'true':'false';");
	    		return;
	    	}else if(type == TYPE_NUMBER){
	    		this.append(prefix,text,";");
	    		return;
	    	}
	    	//
			if((TYPE_NULL|TYPE_BOOLEAN)==type){//onlu null boolean
				this.append(prefix,initText,"?'true':(",tmpId,"===null?'null':'false');");
				return;
			}else if(!((TYPE_NULL|TYPE_BOOLEAN) & type)){//number,string,map,array...
	    		this.append(prefix,_encodeEL(text,model,encoding),";");
	    		return;
	    	}
			//v1=== null?'null':(v===true?'true':(v == false ?'false':txt))
			if(!(type & TYPE_NULL)){
				this.append(prefix,
	    			initText," === true?'true':",
	    				"(",tmpId,"===false?'false':",_encodeEL(tmpId,model,encoding),");");
	    		return ;
			}else if(!(type & TYPE_BOOLEAN)){
	    		this.append(prefix,
	    			initText,"===null?'null':",_encodeEL(tmpId,model,encoding),";");
	    		return ;
			}
    	}
//    	this.append(prefix,
//    		initText," ===null?'null':",
//    		"(",tmpId," === true?'true':",
//    			"(",tmpId,"===false?'false':",_encodeEL(tmpId,model,encoding),"));");
		this.append(prefix,'(',initText,'===null||',tmpId,'===false || ',tmpId,'===true)?json_encode(',tmpId,'):',_encodeEL(tmpId,model,encoding),';')
    },
    appendEL:function(item){
    	this._appendEL(item[1],EL_TYPE)
    },
    appendXT:function(item){
    	this._appendEL(item[1],XT_TYPE)
    },
    appendXA:function(item){
        //[7,[[0,"value"]],"attribute"]
        var el = item[1];
        var value = this.stringifyEL(el);
        var attributeName = item.length>2 && item[2];
        var testAutoId = this.allocateId(value);
        if(testAutoId != value){
            this.append(testAutoId,"=",value,';');
        }
        if(attributeName){
            this.append("if(",testAutoId,"!=null){");
            this.depth++;
            this.append("echo ' "+attributeName+"=\"';");
            this._appendEL(el,XA_TYPE,testAutoId)
            this.append("echo '\"';");
            this.depth--;
            this.append("}");
        }else{
        	this._appendEL(el,XA_TYPE,testAutoId);
        }
        this.freeId(testAutoId);
    },
    appendVar:function(item){
        this.append("$",item[2],"=",this.stringifyEL(item[1]),";");
    },
    processCapture:function(item){
        var childCode = item[1];
        var varName = item[2];
	    this.append("ob_start();");
	    this.appendCode(childCode);
	    this.append("$",varName,"= ob_get_contents();ob_end_clean();");
    },
    appendEncodePlugin:function(item){
    	this._appendEL(item[1],-1,this.stringifyEL(item[1]));
    },
    appendDatePlugin:function(pattern,date){//&#233;&#0xDDS;
    	//this.impl_counter.d++;
    	var pattern = this.stringifyEL(pattern[1]);
    	var date = this.stringifyEL(date[1]);
    	if(/^(?:'[^']+'|"[^"]+")$/.test(pattern)){
    		date = date + ',true';
    	}
        this.append('echo lite__2(',pattern,',',date,');')
    },
    appendModulePlugin:function(child,config){
		if(this.waitPromise){
			this.append("ob_start();");
			//this.append('__out__.lazy(function* __lazy_module_',config.id,'__(__out__){');
			this.outputIndent++;
			this.appendCode(child)
			this.outputIndent--;
			//this.append('})');
			if(this.__lazy_module_){
				this.append('array_push($__lazy_module_,'+config.id+');');
				this.append('array_push($__lazy_module_,ob_get_contents());');
			}else{
				this.append('$__lazy_module_=array('+config.id+',ob_get_contents());')
			}
			this.append('ob_end_clean();');
			this.__lazy_module_ = true;
		}else{
			this.appendCode(child)
		}
    },
    processIf:function(code,i){
        var item = code[i];
        var childCode = item[1];
        var testEL = item[2];
        var test = this.stringifyEL(testEL);
        this.append("if(",php2jsBoolean(testEL,test),"){");
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
                this.append("else if(",php2jsBoolean(testEL,test),"){");
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
    processFor:function(code,i){
        var item = code[i];
        var indexAutoId = this.allocateId();
        var keyAutoId = this.allocateId();
        var isKeyAutoId = this.allocateId();
        var itemsEL = this.stringifyEL(item[2]);
        var varName = '$'+item[3]; 
        //var statusNameId = item[4]; 
        var childCode = item[1];
        var forInfo = this.findForStatus(item)
        if(forInfo.depth){
            var preForAutoId = this.allocateId();
        }
        if(/^\$[\w_]+$/.test(itemsEL)){
        	var itemsAutoId = itemsEL;
        }else{
        	var itemsAutoId = this.allocateId();
        	this.append(itemsAutoId,'=',itemsEL,';');
        }
        //初始化 items 开始
	    this.append('if(',itemsAutoId,'<=PHP_INT_MAX){',itemsAutoId,'=',itemsAutoId,'>0?range(1,',itemsAutoId,'):array();}');
        //初始化 for状态
        var needForStatus = forInfo.ref || forInfo.index || forInfo.lastIndex;
        if(needForStatus){
            if(forInfo.depth){
                this.append(preForAutoId ,"=",FOR_STATUS_KEY,";");
            }
            this.append(FOR_STATUS_KEY," = array('lastIndex'=>count(",itemsAutoId,")-1);");
        }
        
        this.append(indexAutoId,"=-1;")
        this.append(isKeyAutoId,'=false;')
        this.append("foreach(",itemsAutoId," as ",keyAutoId,"=>",varName,"){");
        this.depth++;
	    this.append("if(++",indexAutoId," === 0){");
        this.depth++;
	    this.append(isKeyAutoId,"=",keyAutoId," !== 0;");
        this.depth--;
	    this.append("}");
	    this.append("if(",isKeyAutoId,"){",varName,'=',keyAutoId,";}");
        
        if(needForStatus){
            this.append(FOR_STATUS_KEY,"['index']=",indexAutoId,";");
        }
        this.appendCode(childCode);
        this.depth--;
        this.append("}");//end for
        
        
        if(needForStatus && forInfo.depth){
           this.append(FOR_STATUS_KEY,"=",preForAutoId,';');
        }
        this.freeId(isKeyAutoId);
        this.freeId(keyAutoId);
        this.freeId(itemsAutoId);
        if(forInfo.depth){
            this.freeId(preForAutoId);
        }
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
                this.append(ifstart,"(",indexAutoId,"<0&&",php2jsBoolean(testEL,test),"){");
            }else{
                notEnd = false;
                this.append(ifstart,"(",indexAutoId,"<0){");
            }
            this.depth++;
            this.appendCode(childCode)
            this.depth--;
            this.append("}");
            nextElse = code[i+1];
        }
        this.freeId(indexAutoId);
        return i;
    },
    toSource:function(){
    	return this.out.join('');
    }
});
if(typeof require == 'function'){
exports.PHPTranslator=PHPTranslator;
var php2jsBoolean=require('./php-el-translator').php2jsBoolean;
var isSimplePHPEL=require('./php-el-translator').isSimplePHPEL;
var stringifyPHP=require('./php-el-translator').stringifyPHP;
var stringifyPHPEL=require('./php-el-translator').stringifyPHPEL;
var i18nHash=require('../js/parse/syntax-i18n').i18nHash;
}