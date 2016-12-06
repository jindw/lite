/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
//add as default
/**
 * 模板解析上下文对象实现
 */
function ResultContext(){
	this.result = [];
}
function checkVar(v){
	var exp = /^(break|case|catch|const|continue|default|do|else|false|finally|for|function|if|in|instanceof|new|null|return|switch|this|throw|true|try|var|void|while|with)$|^[a-zA-Z_][\w_]*$/;
	var match = v.match(exp);
	if(v == null || !match || match[1]!=null){
		throw new Error("无效变量名：Lite有效变量名为(不包括括弧中的保留字)："+exp+"\n当前变量名为："+v);
	}
	return v;
}
ResultContext.prototype = {
    /**
     * 异常一定要抛出去，让parseText做回退处理
     */
    parseEL : function(el){
	    try{
	        new Function("return ("+el.replace(/\bfor\b/g,"f")+')');
	        return new Expression(el).token;
	    }catch(e){
	        console.info("表达式解析失败[fileName:"+this._context.currentURI+"]",el,e.message)
	        throw new Error();
	    }
    },
    /**
	 * 添加静态文本（不编码）
	 * @param <String>text
	 */
	appendText:function( text){
		for(var len = arguments.length,i=0;i<len;i++){
			this.result.push(String(arguments[i]));
		}
		//this.result.push.apply(this.result,arguments)
	},

	/**
	 * 添加模板指令
	 * 
	 * @param <Object[]> text
	 */
	appendAll:function(ins){
		for(var len = ins.length,i=0;i<len;i++){
			this.result.push(ins[i]);
		}
	},
	/**
	 * @param Object el
	 */
	appendEL:function( el){
		this.result.push([EL_TYPE, requireEL(this,el)]);
	},
	/**
	 * @param String name
	 * @param Object el
	 */
	appendXA:function(attributeName, el){
		this.result.push([XA_TYPE, requireEL(this,el), attributeName ]);
	},
	/**
	 * @param Object el
	 */
	appendXT:function(el){
		this.result.push([XT_TYPE, requireEL(this,el)]);
	},

	/**
	 * @param Object testEL
	 */
	appendIf:function(testEL){
		this.result.push([IF_TYPE, requireEL(this,testEL) ]);
	},

	/**
	 * @param testEL
	 */
	appendElse:function(testEL){
		clearPreviousText(this.result);
		this.result.push([ELSE_TYPE, testEL && requireEL(this,testEL) || null ]);
	},

	appendFor:function(varName, itemsEL, statusName){
		this.result.push([FOR_TYPE,requireEL(this,itemsEL), varName ]);
		if(statusName){
			this.appendVar(checkVar(statusName) , this.parseEL('for'));
		}
	},

	appendEnd:function(){
		this.result.push([])
	},

	appendVar:function(varName, valueEL){
		this.result.push([VAR_TYPE,requireEL(this,valueEL),checkVar(varName)]);
	},

	appendCapture:function(varName){
		this.result.push([CAPTURE_TYPE,checkVar(varName)]);
	},
	appendPlugin:function(clazz, config){
		if(typeof config == 'string'){
			config = JSON.parse(config);
		}
		config['class'] = clazz;
		this.result.push([PLUGIN_TYPE,config]);
	},
	allocateId:function(){
		if(this.inc){
			this.inc++;
		}else{
			this.inc = 1;
		}
		return 'gid_'+this.inc.toString(32);
	},
	mark:function(){
		return this.result.length;
	},
	reset:function(mark){
		return optimizeResult(this.result.splice(mark,this.result.length));
	},
	toList:function(){
		if(!this.optimized){
			var result = optimizeResult(this.result);
			var defMap = {};
	    	var pureCode = buildTreeResult(result,defMap);
	    	this.optimized = doOptimize(defMap,pureCode);
		}
    	return this.optimized;
	}
}
function requireEL(context,el){
	if(typeof el == 'string'){
		el =  context.parseEL(el);
	}
	return el;
}
/**
 * 移除结尾数据直到上一个end为止（不包括该end标记）
 * @public
 */
function clearPreviousText(result){
    var i = result.length;
    while(i--){
    	var item = result[i];
        if(typeof item == 'string'){//end
            result.pop();
        }else{
        	break;
        }
        
    }
}



if(typeof require == 'function'){
exports.ResultContext=ResultContext;
var Expression=require('js-el').Expression;
var PLUGIN_TYPE=require('./template-token').PLUGIN_TYPE;
var buildTreeResult=require('./optimize-util').buildTreeResult;
var optimizeResult=require('./optimize-util').optimizeResult;
var doOptimize=require('./optimize-util').doOptimize;
var VAR_TYPE=require('./template-token').VAR_TYPE;
var XA_TYPE=require('./template-token').XA_TYPE;
var ELSE_TYPE=require('./template-token').ELSE_TYPE;
var PLUGIN_TYPE=require('./template-token').PLUGIN_TYPE;
var CAPTURE_TYPE=require('./template-token').CAPTURE_TYPE;
var IF_TYPE=require('./template-token').IF_TYPE;
var EL_TYPE=require('./template-token').EL_TYPE;
var XT_TYPE=require('./template-token').XT_TYPE;
var FOR_TYPE=require('./template-token').FOR_TYPE;
}