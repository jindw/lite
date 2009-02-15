/*
 * JavaScript Integration Framework
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/jsi/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
//parse

//add as default
function TextParser(nativeJS){
    this.nativeJS = nativeJS;
    this.parserList = this.parserList.concat([]);
    this.result = [];
}





/**
 * 解析函数集
 * @private
 */
TextParser.prototype = new Parser();
TextParser.prototype.parse = function(url){
    var xhr = new XMLHttpRequest();
    xhr.open("get",url,true)
    xhr.send('');
    this.parseNode(xhr.responseText);
    return this.reuslt;
}
//parse text
TextParser.prototype.addParser(function(node){
    if(node.constructor == String){
        this.append.apply(this,this.parseText(node,false));
        return null;
    }
})




TextParser.prototype.parseText = function(text,xmlText,xmlAttr){
    if(!text){
        return [];
    }
    var buf = [];
    var pattern = new RegExp(/(\\*)\$([a-zA-Z!]{0,5}\{)/g)  //允许$for{} $if{} $end ...  see CT????
    //var pattern = /(\\*)\$\{/g
    var match ;
    //seach:
    while(match = pattern && pattern.exec(text)){
        var begin = match.index;
        var expressionBegin = begin + match[0].length;
        var expressionEnd = expressionBegin;
        var fn = match[2];
        
        begin && buf.push(text.substr(0,begin));
        
        if(match[1].length & 1){//转义后，打印转义结果，跳过
            buf.push(match[1].substr(0,parseInt(match[1].length / 2)) + '$')
            text = text.substr(expressionBegin+1);
        }else{
            fn = fn.substr(0,fn.length-1);
            //expression:
            while((expressionEnd = text.indexOf("}",expressionEnd+1))>0){
                try{
                    var expression = text.substring(expressionBegin ,expressionEnd );
                    expression = this.parseEL(expression);
                    if(xmlAttr){
                    	buf.push([XML_ATTRIBUTE_TYPE,expression]);
                    }else{
                    	buf.push([xmlText ? XML_TEXT_TYPE : EL_TYPE,expression]);
                    }
                    
                    text = text.substr(expressionEnd+1);
                    pattern = text && new RegExp(pattern);
                    //continue seach;
                    break;
                }catch(e){$log.debug("尝试表达式解析失败",expression,e)}
            }
        }
    }
    text && buf.push(text);
    //hack reuse begin as index
    if(xmlText||xmlAttr){
        var begin = buf.length;
        while(begin--){
            //hack match reuse match as item
            var match = buf[begin];
            if(match == ''){
            	buf.splice(begin,1);
            }
            if(match.constructor == String){
                buf[begin] = match.replace(xmlAttr?/[<>&'"]/g:/[<>&]/g,xmlReplacer);
            }
        }
    }
    return buf;
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

/**
 * 异常一定要抛出去，让parseText做回退处理
 */
TextParser.prototype.parseEL = function(expression){
    new Function(expression.replace(/for\s*\./g,"_."));
    try{
        if(this.nativeJS){
            return parseNativeEL(expression);
        }else{
            return new ExpressionTokenizer(expression).toTokens();
        }
    }catch(e){
        $log.debug("表达式解析失败",expression,e)
    }
}


function parseFor(el){
    //与CT相差太远
    try{
        new Function(el);
        el = '{'+el+'}'
    }catch(e){
        new Function(el = '['+el+']');
    }
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