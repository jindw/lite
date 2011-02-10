/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

var ID_PREFIX = "_$";


/**
 * IE 好像容易出问题，可能是线程不安全导致。
 * @internal
 */
var stringRegexp = /["\\\x00-\x1f\x7f-\x9f]/g;
/**
 * 转义替换字符
 * @internal
 */
var charMap = {
    '\b': '\\b',
    '\t': '\\t',
    '\n': '\\n',
    '\f': '\\f',
    '\r': '\\r',
    '"' : '\\"',
    '\\': '\\\\'
};
/**
 * 转义替换函数
 * @internal
 */
function charReplacer(item) {
    var c = charMap[item];
    if (c) {
        return c;
    }
    c = item.charCodeAt().toString(16);
    return '\\u00' + (c.length>1?c:'0'+c);
}
function encodeString(value){
	stringRegexp.lastIndex = 0;
    return '"' + (stringRegexp.test(value) ?
                    value.replace(stringRegexp,charReplacer) :
                    value)
               + '"';
}
function checkEL(el){
    new Function("return "+el)
}

/**
 * JS原生代码翻译器实现
 */
function Translator(id,params){
    this.id = id;
    this.params = params;
}

Translator.prototype = {
	translate:function(context){
	    try{
	    	//var result =  stringifyJSON(context.toList())
	        var list = context.toList();
		    var context = new JSContext(list,this.params);
		    context.parse();
		    var code = context.toString();
		    new Function("function x(){"+code+"\n}");
	    }catch(e){
	    	var buf = [];
	    	for(var n in e){
	    		buf.push(n+':'+e[n]);
	    	}
	    	$log.error(code,e);
	        code = "return ('生成js代码失败：'+"+encodeString(buf.join("\n"))+');';
	    }
	    var body = "("+(this.params?this.params.join(','):'')+"){"+code+"\n}";
	    if(this.id){
	    	try{
	    		new Function("function "+this.id+"(){}");
	    		return "function "+this.id+body;
	    	}catch(e){
	    		return this.id+"=function"+body;
	    	}
	    	
	    }else{
	    	return "function"+body;
	    }
		
	}
}
/**
 * <code>
function(context){
    function _$toList(source,objectType){
        if(objectType){
            var result = [];
            for(objectType in source){
                result.push({key:objectType,value:source[objectType]})
            }
            return result;
        }
        objectType = typeof source;
        return objectType == "number"? new Array(source):
            objectType == "string"?source.split(""):
            source instanceof Array?source:_$toList(source,1);
    }

	function replacer(k){return k in context?context[k]:this[k];}
	var var1 = replacer("var1")
	var var2 = replacer("var2")
	replace = function(c){return "&#"+c.charCodeAt()+";";}</code>
 */
function JSContext(code,params){
    var vs = this.vs = new VarStatus(code);
    this.code = code;
    this.params = params;
    this.hasFor = vs.forInfos.length;
    this.needReplacer = vs.needReplacer;
    this.defs = vs.defs;
    this.refMap = vs.refMap;
    this.idMap = {};
    this.depth = 1;
    this.index = 0
    //print([vs.defs,vs.refMap])
}
JSContext.prototype = {
	getEL:function (el){
		if(el instanceof ELTranslator){
			return el;
		}
		return new ELTranslator(el)
	},
	parse:function(){
		var code = this.code;
		var params = this.params;
		this.out = [];
		var firstAppend = true;
		if(!params){
		    for(var n in this.refMap){
		    	if(n!= 'for'){
		    		if(firstAppend){
		    			firstAppend = false;
		    			this.append('function _$replacer(k){return k in _$context?_$context[k]:this[k];}')
		    			this.append('var _$context = arguments[0];');
		    		}
		    		this.append('var ',n,'=_$replacer("',n,'");')
		    		
		    	}
		    }
		}
		this.append("var _$out=[];")//小心空格，需要做文本搜索的
		
	    if(this.needReplacer){
	    	this.append((params?'function _$replacer':'_$replacer = function')+'(c){return "&#"+c.charCodeAt()+";";}')
	    	this.append('function _$replace(text){return String(text).replace(/[<&"]/g,_$replacer)}')
	    }
	    /**
function _$toList(source,result,type) {
  if (result){
    if(type == "number" && source>0){
      while(source--){
        result[source] = source+1;
      }
    }else{
      for (type in source) {
        result.push(type);
      }
    }
    return result;
  }
  return source instanceof Array ? source
            : _$toList(source, [],typeof source);
}
	     */
	    if(this.hasFor){
	        this.append('var ',FOR_STATUS_KEY);
	        this.append('function _$toList(source,result,type) {');
	        this.append('  if (result){');
	        this.append('    if(type == "number" && source>0){');
	        this.append('      while(source--){');
	        this.append('        result[source] = source+1;');
	        this.append('      }');
	        this.append('    }else{');
	        this.append('      for(type in source){');
	        this.append('        result.push(type);');
 	        this.append('      }');
	        this.append('    }');
	        this.append('    return result;');
	        this.append('  }');
	        this.append('  return source instanceof Array ? source');
	        this.append('            : _$toList(source, [],typeof source);');
	        this.append('}');
	    }
	    
	    
	    //add function
	    for(var i=0;i<this.defs.length;i++){
	        var def = this.defs[i];
	        var n = def.name;
	        this.append("function ",n,"(",def.params.join(','),'){')
	        this.depth++;
	        this.append('var _$out=[];');
	        this.appendCode(def.code);
	        this.append("return _$out.join('');");
	        this.depth--;
	        this.append("}");
	     	this.append('if("',n,'" in _$context){',n,'=_$context["',n,'"];}')
	    }
	    try{
	        this.appendCode(code);
	    }catch(e){
	        //alert(["编译失败：",buf.join(""),code])
	        throw e;
	    }
	    //this.append("return _$out.join('');");
	},
    getForStatus:function(forCode){
        return this.vs.getForStatus(forCode);
    },
    getVarId:function(){
        var i = this.index;
        while(true){
            if(!this.idMap[i]){
                this.idMap[i] = true;
                return ID_PREFIX+i.toString(36);
            }
            i++;
        }
    },
    freeVarId:function(id){
        var i = id.substring(ID_PREFIX.length);
        delete this.idMap[i];
    },
    appendOut:function(){
    	var len = arguments.length;
    	var last = this.out[this.out.length-1];
    	var data = Array.prototype.join.call(arguments,'');
    	if(last == this.lastOut){
    		data = last.substring(0,last.length-2)+","+data+");";
    		this.out[this.out.length-1] = data;
    	}else{
    		data = "_$out.push("+data+");";
    		this.append(data);
    	}
    	this.lastOut = data
    },
    append:function(){
        var depth = this.depth;
        this.out.push("\n");
        while(depth--){
            this.out.push("\t")
        }
        for(var i=0;i<arguments.length;i++){
            this.out.push(arguments[i]);
        }
    },
    /**
     */
    appendCode:function(code){
    	for(var i=0;i<code.length;i++){
    		var item = code[i];
    		if(typeof item == 'string'){
    			this.appendOut(encodeString(item))
    		}else{
    			switch(item[0]){
                case EL_TYPE:
                    this.processEL(item);
                    break;
                case XT_TYPE:
                    this.processXMLText(item);
    			    break;
                case XA_TYPE:
                    this.processXMLAttribute(item);
                    break;
                case VAR_TYPE:
                    this.processVar(item);
                    break;
                case CAPTRUE_TYPE:
                    this.processCaptrue(item);
                    break;
                case IF_TYPE:
                    i = this.processIf(code,i);
                    break;
                case FOR_TYPE:
                    i = this.processFor(code,i);
                    break;
    			case PLUGIN_TYPE://not support
    				break;
                //case ELSE_TYPE:
                default:
                    throw Error('无效指令：'+item)
                }
    		}
    	}
    },
    processEL:function(item){
    	this.appendOut(this.getEL(item[1]))
    },
    processXMLText:function(item){
        this.appendOut("_$replace(",this.getEL(item[1]),")")
    },
    processXMLAttribute:function(item){
        //[7,[[0,"value"]],"attribute"]
        var value = this.getEL(item[1]);
        try{
        	var attributeName = item.length>2?item[2]:null;
        }catch(e){
        	$log.info("@@@@@属性异常："+item.get(2),e)
        }
        if(attributeName){
            var testId = this.getVarId();
            this.append("var ",testId,"=",value);
            this.append("if(",testId,"!=null){");
            this.depth++;
            this.appendOut("' ",attributeName,"=\"',_$replace("+testId+"),'\"'");
            this.depth--;
            this.append("}");
            this.freeVarId(testId);
        }else{
        	this.appendOut("_$replace(",value,")")
        }
    },
    processVar:function(item){
        this.append("var ",item[2],"=",this.getEL(item[1]),";");
    },
    processCaptrue:function(item){
        var childCode = item[1];
        var varName = item[2];
        var bufbak = this.getVarId();
        this.append("var ",bufbak,"=_$out;_$out=[];");
        this.appendCode(childCode);
        this.append("var ",varName,"=_$out.join('');_$out=",bufbak,";");
        this.freeVarId(bufbak);
    },
    processIf:function(code,i){
        var item = code[i];
        var childCode = item[1];
        var test = this.getEL(item[2]);
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
            var test = this.getEL(nextElse[2]);
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
    processFor:function(code,i){
        var item = code[i];
        var indexId = this.getVarId();
        var itemsId = this.getVarId();
        var itemsEL = this.getEL(item[2]);
        var varNameId = item[3]; 
        //var statusNameId = item[4]; 
        var childCode = item[1];
        var forInfo = this.getForStatus(item)
        if(forInfo.depth){
            var previousForValueId = this.getVarId();
        }
        //初始化 items 开始
        this.append("var ",itemsId,"=",itemsEL,";");
        this.append("var ",indexId,"=0;")
        this.append(itemsId,"=_$toList(",itemsId,")");
        
        //初始化 for状态
        var needForStatus = forInfo.ref || forInfo.index || forInfo.lastIndex;
        if(needForStatus){
            if(forInfo.depth){
                this.append("var ",previousForValueId ,"=",FOR_STATUS_KEY,";");
            }
            this.append(FOR_STATUS_KEY," = {lastIndex:",itemsId,".length-1};");
        }
        this.append("for(;",indexId,"<",itemsId,".length;",indexId,"++){");
        this.depth++;
        if(needForStatus){
            this.append(FOR_STATUS_KEY,".index=",indexId,";");
        }
        this.append("var ",varNameId,"=",itemsId,"[",indexId,"];");
        this.appendCode(childCode);
        this.depth--;
        this.append("}");
        
        if(needForStatus && forInfo.depth){
           this.append(FOR_STATUS_KEY,"=",previousForValueId);
        }
        this.freeVarId(itemsId);;
        if(forInfo.depth){
            this.freeVarId(previousForValueId);
        }
        var nextElse = code[i+1];
        var notEnd = true;
        while(notEnd && nextElse && nextElse[0] == ELSE_TYPE){
            i++;
            var childCode = nextElse[1];
            var test = this.getEL(nextElse[2]);
            if(test){
                this.append("if(!",indexId,"&&",test,"){");
            }else{
                notEnd = false;
                this.append("if(!",indexId,"){");
            }
            this.depth++;
            this.appendCode(childCode)
            this.depth--;
            this.append("}");
            nextElse = code[i+1];
        }
        this.freeVarId(indexId);
        return i;
    },
    toString:function(){
    	var s = this.out.join('');
    	var p = /\b_\$out.push\((?:(.*)\);)?/g;
    	p.lastIndex=0;
    	p.exec(s);
    	if(!p.exec(s)){
    		s = s.replace(/^\s+var _\$out=\[\];/,'');
    		s = s.replace(p,"return [$1].join('')")
        	return s;
    	}else{
    		return s+ "\n\treturn _$out.join('');\n";
    	}
    }
}