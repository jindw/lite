/*
 * @author 金大为
 * @from JSON.org(http://www.json.org/)
 * @version $Id: event-util.js,v 1.5 2008/02/25 01:55:59 jindw Exp $
 */

/**
 * IE 好像容易出问题，可能是线程不安全导致。
 * @internal
 */
var stringRegexp = /["\\\x00-\x1f\x7f-\x9f]/g;
/**
 * 转义替换字符
 * @internal
 */
var charMap = {
    '\b': '\\b',
    '\t': '\\t',
    '\n': '\\n',
    '\f': '\\f',
    '\r': '\\r',
    '"' : '\\"',
    '\\': '\\\\'
};
function parseJSON(data){
    return this.eval("("+data+")")
}
/**
 * 转义替换函数
 * @internal
 */
function charReplacer(item) {
    var c = charMap[item];
    if (c) {
        return c;
    }
    c = item.charCodeAt().toString(16);
    return '\\u00' + (c.length>1?c:'0'+c);
}
/**
 * JSON 串行化实现
 * @internal
 */
function stringifyJSON(value,ident,depth) {
    switch (typeof value) {
        case 'string':
            stringRegexp.lastIndex = 0;
            return '"' + (stringRegexp.test(value) ?
                            value.replace(stringRegexp,charReplacer) :
                            value)
                       + '"';
        //case 'function':
        //    return value.toString();
        case 'object':
            if (!value) {
                return 'null';
            }
            depth  = (depth||0)+1;
            var buf = [];
            if (value instanceof Array) {
                var i = value.length;
                while (i--) {
                    buf[i] = stringifyJSON(value[i],ident,depth) || 'null';
                }
                return stringifyObject('[' , buf, ']',ident,depth);
            }else if(value instanceof RegExp){
            	//RegExp Source
            	//return value+'';
            	return '{"class":"RegExp","source":'+stringifyJSON(value+'',ident,depth)+"}";
            }
            for (var k in value) {
                var v = stringifyJSON(value[k],ident,depth);
                if (v) {
                    buf.push(stringifyJSON(k,ident,depth) + ':' + v);
                }
            }
            return stringifyObject('{',buf,'}',ident,depth);
        case 'undefined':
        	return 'null';
        case 'number':
            if(isNaN(value)){
            //	value = 'NaN'
            //}else if(!isFinite(value)){
                value = 'null';
            }
        default:
            return String(value);
    }
}
function stringifyObject(begin,buf,end,ident,depth){
	if(ident){
		var prefix = new Array(depth+1).join(ident);
        return begin+ '\n'+prefix + buf.join(',\n'+prefix) + '\n'+ prefix.substr(ident.length)+end;
    }else{
        return begin + buf.join(',') + end;
    }
}