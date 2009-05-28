/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
//add as default
function ParseContext(){
	this.initialize();
}
function URL(path){
	this.path = path;
}
/**
 * @private
 */
ParseContext.prototype = {
	/**
	 * 初始化上下文
	 * @arguments 链顶插入的解析器列表（第一个元素为初始化后的链顶解析器，以后类推）
	 */
	initialize:function(){
	    var parserList = [];
	    parserList.push.apply(parserList,arguments)
	    this.parserList = parserList.concat(this.parserList);
	    this.result = [];
	    this.topChain = new ParseChain(this);
	},
    createURL:function(path,parentURL) {
    	path = (parentURL||'')+(path||'')
		return new URL(path);
    },
    //nativeJS:false,
    parserList : [],
    loadXML:loadXML,
    selectNodes:selectNodes,
	parseText:function(source, textType) {
		var type = this.textType;
		var mark = this.mark();
		this.textType = textType;
		this.parse(source);
		this.textType = type;
		var result = this.reset(mark);
		return result;
	},
    /**
     * 调用解析链顶解析器解析源码对象
     * @param 文本源代码内容或xml源代码文档对象。
     * @public
     * @abstract
     */
	parse:function(source) {
		if(source instanceof URL){
			source = this.loadXML(source.path);
		}
		this.topChain.process(source);
	},
	
    /**
     * 异常一定要抛出去，让parseText做回退处理
     */
    parseEL : function(el){
        return parseEL(el,this.nativeJS)
    },
	
	mark:function(){
		return this.result.length;
	},
	reset:function(mark){
		return optimizeResult(this.result.splice(mark));
	},
    /**
	 * 添加静态文本（不编码）
	 * @param <String>text
	 * @param <boolean>encode
	 * @param <char>escapeQute
	 */
	append:function( text,  encode,  escapeQute){
		if(encode){
			if(escapeQute == '"'){
				var replaceExp = /[<>&"]/g;
			}else if(escapeQute == '\''){
				var replaceExp = /[<>&']/g;
			}else{
				var replaceExp = /[<>&]/g;
			}
			text = text.replace(replaceExp,xmlReplacer);
		}
		this.result.push(text);
	},

	/**
	 * 添加模板指令
	 * 
	 * @param <Object[]> text
	 */
	appendAll:function(instruction){
		this.result.push.apply(this.result,instruction)
	},
	/**
	 * @param Object el
	 */
	appendEL:function( el){
		this.result.push([EL_TYPE, el]);
	},
	/**
	 * @param String name
	 * @param Object el
	 */
	appendAttribute:function(attributeName, el){
		this.result.push([XML_ATTRIBUTE_TYPE, el, attributeName ]);
	},
	/**
	 * @param Object el
	 */
	appendXmlText:function(el){
		this.result.push([XML_TEXT_TYPE, el]);
	},

	/**
	 * @param Object testEL
	 */
	appendIf:function(testEL){
		this.result.push([IF_TYPE, testEL ]);
	},

	/**
	 * @param testEL
	 */
	appendElse:function(testEL){
		clearPreviousText(this.result);
		this.result.push([ELSE_TYPE, testEL ]);
	},

	appendFor:function(varName, itemsEL, statusName){
		this.result.push([FOR_TYPE,itemsEL, varName ]);
		if(statusName){
			this.appendVar(statusName , this.parseEL('for'));
		}
	},

	appendEnd:function(){
		this.result.push([])
	},

	appendVar:function(varName, valueEL){
		this.result.push([VAR_TYPE,valueEL,varName]);
	},

	appendCaptrue:function(varName){
		this.result.push([CAPTRUE_TYPE,varName]);
	},
	appendAdvice:function(clazz, el){
		this.result.push([ADD_ON_TYPE,el,clazz]);
	},
    buildResult:function(){
    	var result = optimizeResult(this.result);
    	result = buildTreeResult(result);
        if(this.nativeJS){
            var code = buildNativeJS(result);
            try{
                result =  new Function(code);
                result.toString=function(){//_$1 encodeXML
                    return "function(){"+code+"\n}"
                }
            }catch(e){
            	alert("翻译结果错误："+code)
                throw e;
            }
        }
        return result;
    }
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


/**
 * 想当前栈顶添加数据
 * 解析和编译过程中使用
 * @public
 */
function optimizeResult(source){
    var result = [];
    var previousText;
    for(var i=0,j=0;i<source.length;i++){
    	var item = source[i];
		if (item.constructor == String) {
			if(previousText==null){
				j++;
			}else{
				item = previousText + item;
			}
			result[j-1] = previousText = item;
		}else{
			previousText = null;
			result[j++] = item;
		}
    }
    return result;
}
function buildTreeResult(result){
	var stack = [];//new ArrayList<ArrayList<Object>>();
	var current = [];// new ArrayList<Object>();
	stack.push(current);
	for (var i = 0;i<result.length;i++) {
	    var item = result[i];
		if (item.constructor == String) {
			current.push(item);
		} else {
			if (item.length == 0) {
				var children = stack.pop();
				current = stack[stack.length-1];
				current[current.length - 1][1]=children;
			} else {
				var type = item[0];
				var cmd2 =[];
				cmd2.push(item[0]);
				current.push(cmd2);
				switch (type) {
				case CAPTRUE_TYPE:
				case IF_TYPE:
				case ELSE_TYPE:
				case ADD_ON_TYPE:
				case FOR_TYPE:
					cmd2.push(null);
					stack.push(current = []);
				}
				for (var j = 1; j < item.length; j++) {
					cmd2.push(item[j]);
				}

			}
		}
	}
	return current;
}

function xmlReplacer(c){
    switch(c){
        case '<':
          return '&lt;';
        case '>':
          return '&gt;';
        case '&':
          return '&amp;';
        case "'":
          return '&#39;';
        case '"':
          return '&#34;';
    }
}