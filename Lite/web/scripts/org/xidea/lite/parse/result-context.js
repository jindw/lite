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
	this.attributeMap = [[],[]]
}

ResultContext.prototype = {
	setAttribute:function(key,value){
		setValueByKey(this.attributeMap,key,value)
	},
	getAttribute:function(key){
		return getValueByKey(this.attributeMap,key)
	},
    /**
     * 异常一定要抛出去，让parseText做回退处理
     */
    parseEL : function(el){
	    try{
	        new Function("return "+expression.replace(/\bfor\b/g,"f"));
	        return new ExpressionTokenizer(expression).getResult();
	    }catch(e){
	        $log.warn("表达式解析失败[fileName:"+this.currentURI+"]",el,e)
	        throw e;
	    }
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
				var replaceExp = /[<&"]/g;
			}else if(escapeQute == '\''){
				var replaceExp = /[<&']/g;
			}else{
				var replaceExp = /[<&]/g;
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
		this.result.push([EL_TYPE, requireEL(this,el)]);
	},
	/**
	 * @param String name
	 * @param Object el
	 */
	appendAttribute:function(attributeName, el){
		this.result.push([XML_ATTRIBUTE_TYPE, requireEL(this,el), attributeName ]);
	},
	/**
	 * @param Object el
	 */
	appendXmlText:function(el){
		this.result.push([XML_TEXT_TYPE, requireEL(this,el)]);
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
		this.result.push([ELSE_TYPE, testEL && requireEL(this,testEL)  ]);
	},

	appendFor:function(varName, itemsEL, statusName){
		this.result.push([FOR_TYPE,requireEL(this,itemsEL), varName ]);
		if(statusName){
			this.appendVar(statusName , this.parseEL('for'));
		}
	},

	appendEnd:function(){
		this.result.push([])
	},

	appendVar:function(varName, valueEL){
		this.result.push([VAR_TYPE,requireEL(this,valueEL),varName]);
	},

	appendCaptrue:function(varName){
		this.result.push([CAPTRUE_TYPE,varName]);
	},
	appendPlugin:function(clazz, el){
		this.result.push([PLUGIN_TYPE,requireEL(this,el),clazz]);
	},
	mark:function(){
		return this.result.length;
	},
	reset:function(mark){
		return optimizeResult(this.result.splice(mark,this.result.length));
	},
	toList:function(){
		var result = optimizeResult(this.result);
    	return buildTreeResult(result);
	},
    toCode:function(){
        return stringifyJSON(this.toList())
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
				case PLUGIN_TYPE:
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