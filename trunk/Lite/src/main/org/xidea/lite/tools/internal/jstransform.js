var compressJS = $import('org.xidea.lite.util.compressJS');
function JSTransform(code){
	if(this instanceof JSTransform){
		this.code = compressJS(code);
	}else{
		return new JSTransform(code);
	}
}
/**
 * ^ 表达式开始
 * $ 表达式结束
 * :S 匹配表达式代码
 * :ID 匹配JavaScript ID
 * :/exp/i 匹配自定义正则表达式
 * :[] 可选代码
 * 
 * 测试实现只包含一个:S
 */
JSTransform.prototype.replace = function(pattern,callback){
	var left = pattern.replace(/[$^]/g,'').split(':S');
	var right = left[1];
	left=left[0];
	var code = this.code;
	var buf = [];
	var begin = -1;
	while((begin = code.indexOf(left,begin+1))>=0){
		var end = begin + left.length;
		var start = end;
		while((end = code.indexOf(right,end+1))>0){
			try{
				new Function('return ' +code.substring(start,end))	
			}catch(e){continue;}
				//hit
				buf.push(code.substring(0,begin));
				var end2 = end+right.length;
				var statment = code.substring(start,end)
				var all = code.substring(begin,end2);
				buf.push(callback(all,statment));
				code = code.substring(end2);
				begin = -1;
				break;
			
		}
	}
	buf.push(code);
	this.code = buf.join('')
	return this;
}
JSTransform.prototype.compress = function(){
	return compressJS(this.code);
}
exports.JSTransform = JSTransform;