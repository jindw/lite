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
	translate:function(context){
	    //var result =  stringifyJSON(context.toList())
	    var list = context.toList();
		var context = new PHPTranslateContext(list,this.id);
		context.elPrefix = '';//:'@';
		context.encoding = "UTF-8";
	    context.htmlspecialcharsEncoding = context.encoding ;
		context.parse();
		var code = context.toString();
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
	if(defaults && defaults.length){
		params = params.concat();
		var i = params.length;
		var j = defaults.length;
		while(j--){
			params[--i] += '='+stringifyPHP(defaults[j]);
		}
	}
	return params.join(',')
}
function _stringifyPHPLine(line){//.*[\r\n]*
	var endrn="'";
	line = line.replace(/['\\]|(\?>)|([\r\n]+$)|[\r\n]/,function(a,pend,lend){
		if(lend){
			endrn  = '';
			return "'."+stringifyJSON(a);
		}else if(pend){
			return "?'.'>";
		}else{//'\\
			if(a == '\\'){
				return '\\\\';
			}else if(a == "'"){
				return "\\'";
			}else{
				$log.error("非法输出行!!"+JSON.stringify(line));
			}
			return a == '\\'?'\\\\': "\\'";
		}
	});
	line = "'"+line+endrn;
	if("''." == line.substring(0,3)){
		line = line.substring(3)
	}
	return line;
}
PHPTranslateContext.prototype = new TCP({
	stringifyEL:function (el){
		return el?stringifyPHPEL(el.tree):null;
	},
	parse:function(){
		var code = this.code;
		this.depth = 0;
		this.out = [];
	    //add function
	    for(var i=0;i<this.defs.length;i++){
	        var def = this.defs[i];
	        var n = def.name;
	        this.append("if(function_exists('lite_def_",n,"')){function lite_def_",
	        		n,"(",toArgList(def.params),'){')
	        this.depth++;
	        this.append("ob_start();");
	        this.appendCode(def.code);
    		this.append("$rtv= ob_get_contents();ob_end_clean();return $rtv;");
	        this.depth--;
	        this.append("}}");
	    }
	    try{
	        this.append("function lite_tpl_",n,'($__context__){')
			this.append("extract($__context__,EXTR_SKIP);");
	        this.depth++;
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
		var lines = value.match(/.+[\r\n]*|[\r\n]+/g);
		for(var i=0; i<lines.length; i++) {
			var line = lines[i];
			var start = i==0?'echo ':'\t,'
			var end = i == lines.length-1?';':'';
			line = _stringifyPHPLine(line);
			this.append(start+line+end);
		}
	},
    _appendEL:function(el,model,text,prefix){
    	prefix = prefix!=null? prefix : 'echo '+this.elPrefix
    	//@see http://notownme.javaeye.com/blog/335036
    	var encoding = this.htmlspecialcharsEncoding;
    	var text = text || this.stringifyEL(el);
    	
    	//TODO: check el type
		if(model == -1){
			this.append(prefix,"htmlspecialchars(",text,",ENT_COMPAT",encoding,",false);");
			//this.append(prefix,"strtr(",el,",array('<'=>'&lt;','\"'=>'&#34;'));");
		}else if(model == XA_TYPE){
			this.append(prefix,"htmlspecialchars(",text,",ENT_COMPAT",encoding,");");
		}else if(model == XT_TYPE){
			//ENT_COMPAT 
			this.append(prefix,"htmlspecialchars(",text,",ENT_NOQUOTES",encoding,");");
		}else{
			this.append(prefix,text,";");
		}
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
        var testId = this.allocateId(value);
        if(testId != value){
            this.append(testId,"=",value);
        }
        if(attributeName){
            this.append("if(",testId,"!=null){");
            this.depth++;
            this.append("echo ' "+attributeName+"=\"';");
            this._appendEL(el,XA_TYPE,testId)
            this.append("echo '\"';");
            this.depth--;
            this.append("}");
        }else{
        	this._appendEL(el,XA_TYPE,testId);
        }
        this.freeId(testId);
    },
    appendVar:function(item){
        this.append("$",item[2],"=",this.stringifyEL(item[1]),";");
    },
    appendCaptrue:function(item){
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
        var itemsId = this.allocateId();
        var indexId = this.allocateId();
        var keyId = this.allocateId();
        var isKeyId = this.allocateId();
        var itemsEL = this.stringifyEL(item[2]);
        var varNameId = item[3]; 
        //var statusNameId = item[4]; 
        var childCode = item[1];
        var forInfo = this.findForStatus(item)
        if(forInfo.depth){
            var previousForValueId = this.allocateId();
        }
        //初始化 items 开始
	    this.append("if(",itemsId,"<=PHP_INT_MAX){",itemsId,'=range(1,',itemsId,');}');
        //初始化 for状态
        var needForStatus = forInfo.ref || forInfo.index || forInfo.lastIndex;
        if(needForStatus){
            if(forInfo.depth){
                this.append(previousForValueId ,"=",FOR_STATUS_KEY,";");
            }
            this.append(FOR_STATUS_KEY," = array('lastIndex'=>count(",itemsId,")-1};");
        }
        
        this.append(indexId,"=-1;")
        this.append("foreach(",itemsId," as ",keyId,"=>",varNameId,"){");
        this.depth++;
	    this.append("if(++",indexId," === 0){");
        this.depth++;
	    this.append(isKeyId,"=",indexId," === 0;");
        this.depth--;
	    this.append("}else if(",isKeyId,"){",varNameId,'=',keyId,"}");
        
        if(needForStatus){
            this.append(FOR_STATUS_KEY,"['index']=",indexId,";");
        }
        this.append(varNameId,"=",itemsId,"[",indexId,"];");
        this.appendCode(childCode);
        this.depth--;
        this.append("}");//end for
        
        
        if(needForStatus && forInfo.depth){
           this.append(FOR_STATUS_KEY,"=",previousForValueId);
        }
        this.freeId(isKeyId);
        this.freeId(keyId);
        this.freeId(itemsId);
        if(forInfo.depth){
            this.freeId(previousForValueId);
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
                this.append(ifstart,"(!",indexId,"&&",php2jsBoolean(testEL,test),"){");
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
    toString:function(){
    	return this.out.join('');
    }
});