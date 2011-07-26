/*
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
 */
function PHPTranslator(id){
    this.id = id;
}

PHPTranslator.prototype = {
	translate:function(list){
	    //var result =  stringifyJSON(context.toList())
		var context = new PHPTranslateContext(list,this.id);
		context.elPrefix = //'';//:
							'@';//*/
		context.encoding = "UTF-8";
	    context.htmlspecialcharsEncoding = context.encoding ;
		context.parse();
		var code = context.toSource();
	    return '<?php'+code ;
		
	}
}
function PHPTranslateContext(code,id){
    TranslateContext.call(this,code,null);
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
			return "',"+stringifyJSON(a);
		}else if(pend){
			return "?','>";
		}else{//'\\
			if(a == '\\'){
				return '\\\\';
			}else if(a == "'"){
				return "\\'";
			}else{
				$log.error("非法输出行!!"+stringifyJSON(line));
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
PHPTranslateContext.prototype = new TCP({
	stringifyEL:function (el){
		return el?stringifyPHPEL(el,this):null;
	},
	parse:function(){
		var code = this.code;
		this.depth = 0;
		this.out = [];
	    //add function
	    var defs = this.scope.defs;
	    for(var i=0;i<defs.length;i++){
	        var def = defs[i];
	        var n = def.name;
	        this.append("if(!function_exists('lite__",n,"')){function lite__",
	        		n,"(",toArgList(def.params),'){')
	        this.depth++;
	        this.append("ob_start();");
	        this.appendCode(def.code);
    		this.append("$rtv= ob_get_contents();ob_end_clean();return $rtv;");
	        this.depth--;
	        this.append("}}");
	    }
	    try{
	        this.append("function lite_template",this.id,'($__context__){')
	        this.depth++;
			this.append("extract($__context__,EXTR_SKIP);");
	        this.appendCode(code);
	        this.depth--;
	        this.append("}");
	    }catch(e){
	        //alert(["编译失败：",buf.join(""),code])
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
    	prefix = prefix!=null? prefix : 'echo '+this.elPrefix
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
            this.append("if(",this.elPrefix,testAutoId,"!=null){");
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
        this.append("$",item[2],"=",this.elPrefix,this.stringifyEL(item[1]),";");
    },
    appendCapture:function(item){
        var childCode = item[1];
        var varName = item[2];
	    this.append("ob_start();");
	    this.appendCode(childCode);
	    this.append("$",varName,"= ob_get_contents();ob_end_clean();");
    },
    appendEncodePlugin:function(item){
    	this._appendEL(item[1],-1,this.stringifyEL(item[1]));
    },
    processIf:function(code,i){
        var item = code[i];
        var childCode = item[1];
        var testEL = item[2];
        var test = this.stringifyEL(testEL);
        this.append("if(",this.elPrefix,php2jsBoolean(testEL,test),"){");
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
                this.append("else if(",this.elPrefix,php2jsBoolean(testEL,test),"){");
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
        var itemsAutoId = this.allocateId();
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
        this.append(itemsAutoId,'=',this.elPrefix,itemsEL,';');
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
        this.append("foreach(",itemsAutoId," as ",keyAutoId,"=>",varName,"){");
        this.depth++;
	    this.append("if(++",indexAutoId," === 0){");
        this.depth++;
	    this.append(isKeyAutoId,"=",indexAutoId," === 0;");
        this.depth--;
	    this.append("}else if(",isKeyAutoId,"){",varName,'=',keyAutoId,";}");
        
        if(needForStatus){
            this.append(FOR_STATUS_KEY,"['index']=",indexAutoId,";");
        }
        this.append(varName,"=",itemsAutoId,"[",indexAutoId,"];");
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
                this.append(ifstart,"(",this.elPrefix,"!",indexAutoId,"&&",php2jsBoolean(testEL,test),"){");
            }else{
                notEnd = false;
                this.append(ifstart,"(",this.elPrefix,"!",indexAutoId,"){");
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