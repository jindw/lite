/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

//parse



function parseText(text,context,parseChain){
    if(text!=null && text.constructor == String){
        switch(context.textType){        case XML_ATTRIBUTE_TYPE :
            var escapeQute = '"';
        case XML_TEXT_TYPE :
            var encode = true;  
            var textType = context.textType;
            break;
        default:
            var textType = EL_TYPE;
        }
        var pattern = /(\\*)\$([a-zA-Z!]{0,5}\{)/  //允许$for{} $if{} $end ...  see CT????
        //var pattern = /(\\*)\$\{/g
        var match ;
        //seach:
        while(match = pattern && pattern.exec(text)){
            var begin = match.index;
            var expressionBegin = begin + match[0].length;
            var expressionEnd = findELEnd(text,expressionBegin-1);
            var fn = match[2];
            
            begin && context.append(text.substr(0,begin),encode,escapeQute);
            
            if(match[1].length & 1){//转义后，打印转义结果，跳过
                context.append(match[1].substr(0,parseInt(match[1].length / 2)) + '$',encode,escapeQute)
                text = text.substr(expressionBegin+1);
            }else{
                fn = fn.substr(0,fn.length-1);
                //expression:
                try{
                    var expression = text.substring(expressionBegin ,expressionEnd );
                    expression = context.parseEL(expression);
                    if(textType == XML_TEXT_TYPE){
                    	context.appendXmlText(expression);
                    }else if(textType == XML_ATTRIBUTE_TYPE){
                    	context.appendAttribute(null,expression);
                    }else{
                    	context.appendEL(expression);
                    }
                    
                    text = text.substr(expressionEnd+1);
                    //以前为了一些正则bug,不知道是否还需要:(
                    //pattern = text && /(\\*)\$([a-zA-Z!]{0,5}\{)/;
                    //continue seach;
                }catch(e){
                	$log.debug("尝试表达式解析失败",expression,text,expressionBegin ,expressionEnd,e);
                	context.append(match[0],encode,escapeQute);
                	text = text.substr(expressionBegin);
                }
            }
        }
        text && context.append(text,encode,escapeQute);
    }else{
        parseChain.process(text);
    }
}

function parseFN(fn,expression){
    if(fn){
        switch(fn){
            case 'for':
            //parseFor();
        }
        throw new Error("不支持指令："+fn);
    }
}
function parseEL(expression){
    try{
        checkEL(expression.replace(/\bfor\b/g,"f"));
        return new ExpressionTokenizer(expression).getResult();
    }catch(e){
        $log.debug("表达式解析失败",expression,e)
        throw e;
    }
}

function parseFor(el){
    //与CT相差太远
    try{
        checkEL(el);
        el = '{'+el+'}'
    }catch(e){
        checkEL(el = '['+el+']');
    }
}
function checkEL(el){
    new Function("return "+el)
}