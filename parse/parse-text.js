/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

var XA_TYPE=require('./template-token').XA_TYPE;
var EL_TYPE=require('./template-token').EL_TYPE;
var XT_TYPE=require('./template-token').XT_TYPE;
exports.parseText=parseText;
/**
 * for extension text parser
 */
function parseText(text,context,textParsers){
	switch(context.textType){
    case XA_TYPE :
        var qute = "'";
    case XT_TYPE :
        var encode = true;  
    case EL_TYPE:
        break;
    default:
    	console.error("未知编码模式："+context.textType+text)
    	return;
    }
	var len = text.length;
	var start = 0;
	do {
		var nip = null;
		var p$ = len + 1;
		{
			var pri = 0;
			var ti = textParsers.length;
			while (ti--) {
				var ip = textParsers[ti];
				var p$2 = ip.findStart(text, start, p$);
				var pri2 = ip.priority || 1;
				if (p$2 >= start ){
					if(p$2 < p$ || p$2 == p$ && pri2>pri){
						p$ = p$2;
						nip = ip;
						pri = pri2;
					}
				}
				
			}
		}
		if (nip != null) {
			var escapeCount = countEescape(text, p$);
			appendText(context,
					text.substring(start, p$ - ((escapeCount + 1) >>1)),
					encode,	qute);
			if ((escapeCount & 1) == 1) {// escapsed
				start = nextPosition(context, text, p$);
			} else {
				start = p$;
				var mark = context.mark();
				try {
					start = nip.parseText(text, start, context);
				} catch (e) {
					console.warn("尝试表达式解析失败:[source:"+text+",fileName:"+context.currentURI+"]",e);
				}
				if (start <= p$) {
					context.reset(mark);
					start = nextPosition(context, text, p$);
				}

			}
		} else {
			break;
		}
	} while (start < len);
	if (start < len) {
		appendText(context,text.substring(start), encode, qute);
	}
}
/**
 * 添加静态文本（不编码）
 * @param <String>text
 * @param <boolean>encode
 * @param <char>escapeQute
 */
function appendText(context,text, encode,  escapeQute){
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
	context.appendText(text);
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
function nextPosition(context, text, p$) {
	context.appendText(text.substring(p$, p$ + 1));
	return p$ + 1;
}

function countEescape(text, p$) {
	if (p$ > 0 && text.charAt(p$ - 1) == '\\') {
		var pre = p$ - 1;
		while (pre-- > 0 && text.charAt(pre) == '\\')
			;
		return p$ - pre - 1;
	}
	return 0;
}


