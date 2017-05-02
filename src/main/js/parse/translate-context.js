/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */


exports.TranslateContext=TranslateContext;
var OptimizeScope=require('./optimize-scope').OptimizeScope;
var VAR_TYPE=require('./template-token').VAR_TYPE;
var XA_TYPE=require('./template-token').XA_TYPE;
var ELSE_TYPE=require('./template-token').ELSE_TYPE;
var PLUGIN_TYPE=require('./template-token').PLUGIN_TYPE;
var CAPTURE_TYPE=require('./template-token').CAPTURE_TYPE;
var IF_TYPE=require('./template-token').IF_TYPE;
var EL_TYPE=require('./template-token').EL_TYPE;
var XT_TYPE=require('./template-token').XT_TYPE;
var FOR_TYPE=require('./template-token').FOR_TYPE;
var ID_PREFIX = "$_";
var XML_ENCODE_XA = 1;
var XML_ENCODE_XT = 2;
/**
 * @extends LiteContext
 */
function TranslateContext(code,params){
    /**
     * 当前域下的参数表[可以为null,null和空数组表示的意思不同]
     */
    this.params = params;
    /**
     * 当前scope的信息(包括变量,引用,函数调用信息,for状态,函数集...) 
     */
    this.scope = new OptimizeScope(code,params);
    this.allocateIdMap = {};
    this.outputIndent = 0;

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
            if(!this.allocateIdMap[i]){
                this.allocateIdMap[i] = true;
                return ID_PREFIX+i.toString(36);
            }
            i++;
        }
    },
    freeId:function(id){
    	var len = ID_PREFIX.length;
        if(id.substring(0,len) == ID_PREFIX){
        	delete this.allocateIdMap[id.substring(len)];
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
                var type = item && item[0];
    			switch(type){
                case EL_TYPE:
                    this.visitEL(item[1],type)
                    this.appendEL(item);
                    break;
                case XT_TYPE:
                    this.visitEL(item[1],type)
                    this.appendXT(item);
    			    break;
                case XA_TYPE:
                    this.visitEL(item[1],type)
                    this.appendXA(item);
                    break;
                case VAR_TYPE:
                    this.visitEL(item[1],type)
                    this.appendVar(item);
                    break;
                case CAPTURE_TYPE:
                    this.visitEL(null,type)
                    this.processCapture(item);
                    break;
    			case PLUGIN_TYPE://not support
                    this.visitEL(item[2],type)
    				this.processPlugin(item[1],item[2]);
    				break;
                case IF_TYPE:
                    this.visitEL(item[2],type)
                    i = this.processIf(code,i);
                    break;
                case FOR_TYPE:
                    this.visitEL(item[2],type)
                    i = this.processFor(code,i);
                    break;
                case ELSE_TYPE:
                    this.visitEL(item[2],type)
                	i = this.processElse(code,i);
    				break;
                default:
                    throw Error('无效指令：'+i+JSON.stringify(code))
                }
    		}
    	}
    },
    visitEL:function(){},
    //[PLUGIN_TYPE,child,config]
    processPlugin:function(child,config){
    	var pn = config['class'];
    	switch(pn.replace(/^org\.xidea\.lite\.(?:parse\.)?/,'')){
    	case 'EncodePlugin':
    		this.appendEncodePlugin(child[0]);
    		break;
    	case 'DatePlugin':
    		this.appendDatePlugin(child[0],child[1]);
    		break;
    	case 'NativePlugin':
    		this.appendNativePlugin(child,config);
    		break;
        case 'ModulePlugin':
            this.appendModulePlugin(child,config);
            break;
        case 'DefinePlugin':
            //全局自动处理
            break;
    	case 'ClientPlugin':
            //编译期消灭
    	default:
			console.error("程序bug(插件需要预处理):"+pn,config);
    	}
    },
    processElse:function(code,i){
    	throw Error('问题指令(无主else,else 指令必须紧跟if或者for)：'+code,i);
    },
    append:function(){
        var outputIndent = this.outputIndent;
        this.out.push("\n");
        while(outputIndent--){
            this.out.push("\t")
        }
        for(var i=0;i<arguments.length;i++){
            this.out.push(arguments[i]);
        }
    },
    reset:function(){
    	var out = this.out.concat();
    	this.out.length=0;
    	return out;
    }
}

