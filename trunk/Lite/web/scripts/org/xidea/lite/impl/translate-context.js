/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */


var ID_PREFIX = "$_";
var XML_ENCODE_XA = 1;
var XML_ENCODE_XT = 2;
/**
 * @extends LiteContext
 */
function TranslateContext(code,name,params){
    /**
     * 函数名称 可以是null
     */
    this.name = name;
    /**
     * 当前域下的参数表[可以为null,null和空数组表示的意思不同]
     */
    this.params = params;
    /**
     * 当前scope的信息(包括变量,引用,函数调用信息,for状态,函数集...) 
     */
    this.scope = new OptimizeScope(code,params)
    this.code = code;
    this.idMap = {};
    this.depth = 0;

}


TranslateContext.prototype = {
    findForStatus:function(code){
	    var fis = this.scope.fors;
	    var i = fis.length;
	    while(i--){
	        var fi = fis[i];
	        if(fi.code == code){
	            return fi;
	        }
	    }
        //return this.vs.getForStatus(forCode);
    },
    allocateId:function(id){
    	if(id && /^([\w\$_]+|[\d\.]+)$/.test(id)){
    		return id;
    	}
        var i = 0;
        while(true){
            if(!this.idMap[i]){
                this.idMap[i] = true;
                return ID_PREFIX+i.toString(36);
            }
            i++;
        }
    },
    freeId:function(id){
    	var len = ID_PREFIX.length;
        if(id.substring(0,len) == ID_PREFIX){
        	delete this.idMap[id.substring(len)];
        }
    },
    /**
     */
    appendCode:function(code){
    	for(var i=0;i<code.length;i++){
    		var item = code[i];
    		if(typeof item == 'string'){
    			this.appendStatic(item)
    		}else{
    			switch(item && item[0]){
                case EL_TYPE:
                    this.appendEL(item);
                    break;
                case XT_TYPE:
                    this.appendXT(item);
    			    break;
                case XA_TYPE:
                    this.appendXA(item);
                    break;
                case VAR_TYPE:
                    this.appendVar(item);
                    break;
                case CAPTURE_TYPE:
                    this.appendCapture(item);
                    break;
    			case PLUGIN_TYPE://not support
    				var pn = item[2]['class']
    				if(/^(?:baidu\.)?org\.xidea\.lite\.EncodePlugin$/.test(pn)){
    					this.appendEncodePlugin(item[1][0]);
    				}else if(/^(?:baidu\.)?org\.xidea\.lite\.DefinePlugin$/.test(pn)){
    					//continue;
    				}else if(/^(?:baidu\.)?org\.xidea\.lite\.parse\.ClientPlugin$/.test(pn)){
    					$log.error("程序bug(插件需要预处理):"+pn,item[2]);
    					//continue;
    				}else{
    					$log.error("程序bug(插件类型尚未支持):"+pn,item[2]);
    				}
    				break;
                case IF_TYPE:
                    i = this.processIf(code,i);
                    break;
                case FOR_TYPE:
                    i = this.processFor(code,i);
                    break;
                case ELSE_TYPE:
                	i = this.processElse(code,i);
    				break;
                default:
                    throw Error('无效指令：'+i+stringifyJSON(code))
                }
    		}
    	}
    },
    processElse:function(code,i){
    	throw Error('问题指令(无主else,else 指令必须紧跟if或者for)：'+code,i);
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
    reset:function(){
    	var text = this.out.join('');
    	this.out.length=0;
    	return text;
    }
}

