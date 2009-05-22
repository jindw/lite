/*
 * JavaScript Integration Framework
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/jsi/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

var EL_TYPE = 0;// [0,'el']
var IF_TYPE = 1;// [1,[...],'test']
var BREAK_TYPE = 2;// [2,depth]
var XML_ATTRIBUTE_TYPE = 3;// [3,'value','name']
var XML_TEXT_TYPE = 4;// [4,'el']
var FOR_TYPE = 5;// [5,[...],'items','var']
var ELSE_TYPE = 6;// [6,[...],'test']//test opt?
var ADD_ON_TYPE =7;// [7,[...],'var']
var VAR_TYPE = 8;// [8,'value','name']
var CAPTRUE_TYPE = 9;// [9,[...],'var']
var IF_KEY = "if";
var FOR_KEY = "for";

var TEMPLATE_NS_REG = /^http:\/\/www.xidea.org\/ns\/(?:template|lite)(?:\/core)?\/?$/;


//add as default
function ParseContext(){
    this.parserList = this.parserList.concat([]);
    this.result = [];
}


/**
 * @private
 */
ParseContext.prototype = {
    //nativeJS:false,
    parserList : [],

	parseText:function(source, defaultType) {
		var type = this.textType;
		this.textType = defaultType;
		this.parse(source);
		this.textType = type;
	},

	parse:function(source) {
		if(typeof text == 'object'){
			
		}
		this.topChain.process();
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
		this.result.push([VAR_TYPE,varName , valueEL]);
	},

	appendCaptrue:function(varName){
		this.result.push([CAPTRUE_TYPE,varName]);
	},

	appendAdvice:function(class1, parseEL){
		throw new Error("not support");
	},
    

    /**
     * 给出文件内容或url，解析模版源文件。
     * 如果指定了base，当作url解析，无base，当作纯文本解析
     * @public
     * @abstract
     * @return <Array> result
     */
    parse : function(node){
        throw new Error("未实现")
    },
    /**
     * 解析源文件文档节点。
     * @public 
     */
    parseNode : function(node){
        var parserList = this.parserList;
        var i = parserList.length;
        while(i-- && node!=null){
            node = parserList[i].call(this,node)
        }
    },
    buildResult:function(){
    	var result = joinText(this.result);
        if(this.nativeJS){
            var code = buildNativeJS(buildTreeResult(result));
            try{
                var result =  new Function(code);
                result.toString=function(){//_$1 encodeXML
                    return "function(){"+code+"\n}"
                }
                return result;
            }catch(e){
            	alert("翻译结果错误："+code)
                throw e;
            }
        }else{
            var data = buildTreeResult(result);
            return data;
        }
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
function joinText(source){
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