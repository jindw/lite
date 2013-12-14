/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

/**
 * 
 * @param text
 * @param elQuteBegin
 *            {的位置
 * @return }的位置
 */
function findELEnd(text, elQuteBegin) {
	elQuteBegin = elQuteBegin||0;
	var length = text.length;
	var next = elQuteBegin + 1;
	if (next >= length) {
		return -1;
	}
	var stringChar = 0;
	var depth = 0;
	do {
		var c = text.charAt(next);
		switch (c) {
		case '\\':
			next++;
			break;
		case '\'':
		case '"':
			if (stringChar == c) {
				stringChar = 0;
			} else if (stringChar == 0) {
				stringChar = c;
			}
			break;
		case '{':
		case '[':
		case '(':
			if (stringChar == 0) {
				depth++;
			}
			break;
		case '}':
		case ']':
		case ')':
			if (stringChar == 0) {
				depth--;
				if (depth < 0) {
					return next;
				}
			}
			break;
		case '/':// 如果是正则，需要跳过正则
			if (stringChar == 0) {
				var regExp = isRegExp(text, elQuteBegin, next);
				if (regExp) {
					var end = findRegExpEnd(text, next);
					if(end >0){
						next = end;
					}else{
						console.error("无效状态");
					}
				}
			}
		}
	} while (++next < length);
	return -1;
}

function isRegExp(text, elQuteBegin,
		regExpStart) {
	for (var i = regExpStart-1; i > elQuteBegin; i--) {
		var pc = text.charAt(i);
		if (!/\s/.test(pc)) {
			if (/[\w\$]/.test(pc)) {
				return false;// 有效id后，不可能是正则
			} else {
				switch (pc) {
				case ']':// 伪有效id后，不可能是正则
				case ')':
				case '}':
					return false;
					// case '{'
					// case '[':
					// case '(':
					// 伪开头，不可能是除号，是正则
					// isRegExp = true;
					// break;
					// +-*/ 非后缀运算符后，一定是正则，非运算符
				default:
					return true;
				}
			}
		}
	}
	// 开头出现时，是正则
	return true;
}
function findRegExpEnd( text, regExpStart) {
	var length = text.length;
	var depth = 0;
	for (regExpStart++; regExpStart < length; regExpStart++) {
		var rc = text.charAt(regExpStart);
		if (rc == '[') {
			depth = 1;
		} else if (rc == ']') {
			depth = 0;
		} else if (rc == '\\') {
			regExpStart++;
		} else if (depth == 0 && rc == '/') {
			while (regExpStart < length) {
				rc = text.charAt(regExpStart++);
				switch (rc) {
				case 'g':
				case 'i':
				case 'm':
					break;
				default:
					return regExpStart - 1;
				}
			}

		}
	}
	return -1;
}

function findLiteParamMap(value){
	var result = {};
	while(value){
		var match = value.match(/^\s*([\w\$\_]+|'[^']*'|"[^"]*")\s*(?:[\:=]\s*([\s\S]+))\s*$/);
		if(!match){
			throw console.error("非法参数信息",value);
			return null;
		}
		value =match[2];
		var key = match[1].replace(/^['"]|['"]$/g,'');
		var p = findStatementEnd(value);
		var statment = value.substring(0,p);
		
		result[key] = statment;
		value = value.substring(p+1);
	}
	return result;
}
/**
 * @private
 */
function findStatementEnd(text){
	var end = 0;
	do{
		var end1 = text.indexOf(',',end + 1);
		var end2 = text.indexOf(';',end + 1);
		if(end2>0 && end1>0){
			end = Math.min(end1 , end2);
		}else{
			end = Math.max(end1,end2);
		}
		if(end<=0){
			break;
		}
		var code = text.substring(0,end);
		try{
			new Function(code);
			return end;
		}catch(e){
			end = end+1
		}
	}while(end>=0)
	return text.length;
}
if(typeof require == 'function'){
exports.findLiteParamMap=findLiteParamMap;
exports.findELEnd=findELEnd;
}