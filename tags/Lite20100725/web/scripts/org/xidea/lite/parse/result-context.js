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
	textType:0,
	setTextType:function(textType){
		this.textType = textType;
	},
	getTextType:function(){
		return this.textType;
	},
	setAttribute:function(key,value){
		setByKey(this.attributeMap,key,value)
	},
	getAttribute:function(key){
		return getByKey(this.attributeMap,key)
	},
    /**
     * 异常一定要抛出去，让parseText做回退处理
     */
    parseEL : function(el){
	    try{
	        new Function("return "+el.replace(/\bfor\b/g,"f"));
	        return new ExpressionTokenizer(el).getResult();
	    }catch(e){
	        $log.debug("表达式解析失败2[fileName:"+this.currentURI+"]",el,e.message)
	        throw new Error();
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
		this.result.push([XML_ATTRIBUTE_TYPE, requireEL(this,el), attributeName ]);
	},
	/**
	 * @param Object el
	 */
	appendXT:function(el){
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
		this.result.push([ELSE_TYPE, testEL && requireEL(this,testEL) || null ]);
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
		if ('string' == typeof item) {
			if(previousText==null){
				previousText = item;
			}else{
				previousText += item;
			}
		}else{
			if(previousText){
				result[j++] = previousText;
			}
			previousText = null;
			result[j++] = item;
		}
    }
    if(previousText){
    	result[j++] = previousText;
    }
    return result;
}
function buildTreeResult(result){
	var stack = [];//new ArrayList<ArrayList<Object>>();
	var defs = [];
	var current = [];// new ArrayList<Object>();
	stack.push(current);
	for (var i = 0;i<result.length;i++) {
	    var item = result[i];
		if ('string' == typeof item) {
			current.push(item);
		} else {
			if (item.length == 0) {//end
				var children = stack.pop();
				current = stack[stack.length-1];//向上一级列表
				var parentNode = current.pop();//最后一个是当前结束的标签
				parentNode[1]=children;
				if(parentNode[0] == PLUGIN_TYPE && parentNode[3]== 'org.xidea.lite.DefinePlugin'){
					defs.push(parentNode);
				}else{
					current.push(parentNode);
				}
				
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
	return defs.concat(current);
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