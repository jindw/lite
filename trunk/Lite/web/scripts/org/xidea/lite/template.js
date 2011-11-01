/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

/**
 * 生成模板函数
 * @param fn
 * @param 
 */

/**
 * 如果传入的是json 数组 或者是函数对象，直接作为编译结果初始化，否则，作为源代码编译。
 * @param fn 模板源代码或者编译结果
 * <a href="http://code.google.com/p/lite/wiki/Template"> 模版基础指令说明</a>
 * @public
 */
function Template(fn){
    /**
     * 模板数据
     * @private
     * @tyoeof string
     */
    if(typeof fn == 'function'){
    	this.render = fn(this);
    }else{
    	return new ($import('org.xidea.lite.impl:TemplateImpl'))(fn);
    }
}
var liteImpl;
/**
 * micro:(a:a+1)
 * 		(a,b:a+1)
 * 
 * liteWrap(function(a,b){
 * 	<xml>tpl:${a+b+c}</xml>
 * })
 * ==>
 * liteWrap(function(a,b){return '<xml>tpl${a+b+c}<xml>'},impl?);
 * ==>
 * function(impl){return function(a,b){return ['<xml>tpl:',a+b+c,'</xml>'].join('')}}(impl);
 */
var liteWrap = function(replaceMap){
	function replacer(c){return replaceMap[c]||c}
	function dl(date,format){//3
	    format = format.length;
	    return format == 1?date : ("000"+date).slice(-format);
	}
	function tz(offset){
		return offset?(offset>0?'-':offset*=-1||'+')+dl(offset/60,'00')+':'+dl(offset%60,'00'):'Z'
	}
	liteImpl = {
		//xt:0,xa:1,xp:2
		0:function(txt,type){
			return String(txt).replace(
				type==1?/[<&"]/g:
					type?/&(?:\w+|#\d+|#x[\da-f]+);|[<&"]/ig:/[<&]/g
				,replacer);
		},
		1:function(source,result,type) {
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
		},
		2: function(pattern,date){
			//TODO:未考虑国际化偏移
			date = date?new Date(date):new Date();
	        return pattern.replace(/([YMDhms])\1*|\.s|TZD/g,function(format){
	            switch(format.charAt()){
	            case 'Y' :
	                return dl(date.getFullYear(),format);
	            case 'M' :
	                return dl(date.getMonth()+1,format);
	            case 'D' :
	                return dl(date.getDate(),format);
	//	            case 'w' :
	//	                return date.getDay()+1;
	            case 'h' :
	                return dl(date.getHours(),format);
	            case 'm' :
	                return dl(date.getMinutes(),format);
	            case 's' :
	                return dl(date.getSeconds(),format);
	            case '.':
	            	return '.'+dl(date.getMilliseconds(),'000');
	            case 'T'://tzd
	            	//国际化另当别论
	            	return tz(date.getTimezoneOffset());
	            }
	        });
		}
	}
	Template.prototype = liteImpl;
	function wrap(data,global){
		var impl = null;
		return typeof liteWrapImpl == 'function' ? liteWrapImpl(data,global):
			function(){//lazy impl
				if(!impl){
					if(typeof liteWrapImpl == 'function'){
						impl = liteWrapImpl(data,global);
					}else{
						document.cookie = 'LITE_COMPILER='+implUrl
						//alert('liteWrapImpl not load');
						location.reload();
					}
					
				}
				return impl.apply(this,arguments);
			};
	}
	
	//
	function loadImpl(){
		loadImpl = null;
    	if(!implUrl){
    		if(typeof $import == 'function'){
    			return $import('org.xidea.lite.impl.js:liteWrapImpl');
    		}
    	}
    	//
    	document.write('<script src="'+implUrl+'"></script>')
	}
	
	var implUrl =  String(this.document && document.cookie || '').match(/\bLITE_COMPILER=([^;]+)/);
    implUrl = implUrl && implUrl[1];
    if(implUrl){
    	loadImpl();
    }else{
    	implUrl = 'http://www.xidea.org/lite/release/lite-wrap-impl.js'
    }
	return function(fn,global){
	    loadImpl && loadImpl();
	    return wrap(fn,global || liteImpl);
	}
}( {'"':'&#34;','<':'&lt;','&':'&#38;'});
