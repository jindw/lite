function compressJS(source){
	if(source.search(/^(?:\s|\/[*\/])/m)<0){
		return source;
	}
	var ps = partitionJavaScript(source);
	var result = [];
	for(var i =0;i<ps.length;i++){
		var item = ps[i];
		switch(item.charAt()){
		case '\'':
		case '\"':
			result.push(item);//string
			break;
		case '/':
			//skip comment && reserve condition comment and regexp
			var stat = item.match(/^\/(?:(\*\s*@)|\/|\*)/);
			if(!stat || stat[1]){
				result.push(item);//regexp or condition comment
			}
			break;
			
		default:
			//result.push(item.replace(/^[ \t]+/gm,''));//被切开的语法块，前置换行，可能上上一个语法的结束语法，不能删除
			result.push(item.replace(/^[\t ]+|([\r\n])\s+/g,'$1'));
		}
	}
	return result.join('');
}
try{
	var parseLite = require('lite').parseLite;
}catch(e){}
/**
 * 如何token
 * 如何补全; 能不补全就不补全
 */
function partitionJavaScript(source){
	var regexp = /'(?:\\.|[^'])*'|"(?:\\.|[^"])*"|\/\/.*|\/\*([^*]+|\*[^\/])*\*\/|[\/<]/;
	var m,result = [],concatable=false;//not comment string regexp
	regexp.lastIndex = 0;
	while(m = regexp.exec(source)){
		if(m){
			var index = m.index;
			var m = m[0];
			var xml = m == '<'
			if(m == '/' || xml){
				var subsource = (xml ?findXML:findExp)(result,source.substring(index));
				if(subsource){
					m = subsource;
					if(xml){
						ex = 'new XML("'+subsource.replace(/["\r\n]/g,jsReplace)+'")'
					}
					concatable = false;
					//TODO: 内置 lite xml
					var pre = result[result.length-1];
					var tail = source.substring(m.length+index);
					var preReg =  /\bparseLite\s*\(\s*$/;
					var tailReg = /^\s*\)/;
					if(xml && parseLite &&pre.test(preReg) && tailReg.test(tail) ){
						result.pop();
						subsource = parseLite(subsource);
						source = tail.replace(tailReg,'');
						continue;
					}else{
						result.push(source.substring(0,index),subsource);
					}
				}else{
					if(concatable){
						result[result.length-1]+=source.substring(0,index+1)
					}else{
						result.push(source.substring(0,index+1))
					}
					concatable = true;
				}
			}else{
				result.push(source.substring(0,index),m);
				concatable  = false;
			}
			source = source.substring(m.length+index);
		}else{
			break;
		}
	}
	result.push(source);
	return result;
}
/**
 * 
运算符 ‘/’ 优先考虑
var i=0;		
if(i)alert(1)//...
/alert(2)/i
=> var i=0;if(i){alert(1)/alert(2)/i}
忽略 CDATA/textarea

 */
function findXML(result,source){
	var tag = source.match(/^<([a-zA-Z_][\w_\-\.]*(?:\:[\w_\-\.]+)?)(?:\s*[\/>]|\s+[\w_])/);
	if(tag){
		tag = tag[1];
		tag = tag.replace(/\.\-/g,'\\$&');
		var reg = new RegExp('<(/)?'+tag,'g');
		var depth = 0;
		reg.lastIndex = 0;
		while(tag = reg.exec(source)){
			if(tag[1]){
				if(--depth == 0){
					return source.substring(0,tag.index+tag[0].length+1)
				}else if(depth<0){
					return null;
				}
			}else{
				depth++;
			}
		}
	}else{
		return null;
	}
}
function jsReplace(c){
	switch(c){
		case '\r':
			return '\\r';
		case '\n':
			return '\\n';
		case '"':
			return '\\"';
		
	}
}
/**
 * 
 * ''/b/ // a
 * /xxx\///2; /**\/ //b=2; 
 *
 */
function findExp(result,source){
	var i = result.length;
	while(i--){
		var line = result[i];
		if(!/^\/[\/*]|^\s+$/.test(line)){//ignore common or space
			line = line.replace(/\s+$/,'');
			if(/^['"]|^\/.+\/$/.test(line)){
				break;
			}
			if(/^(?:\b(?:new|instanceof|typeof)|[^\w_$\]})])$/.test(line)){
				//a /b 1.1/b (1+2)/b a++ /2
				break;
			}else{
				// if(this.status != STATUS_EXPRESSION)
				// is op / start
				return findExpSource(source);
			}
		}
	}
}

function findExpSource(text){
	var depth=0,c,start = 1;
	while(c = text.charAt(start++)){
		if(c =='\n' || c == '\r'){
			return;
		}
	    if(c=='['){
	    	depth = 1;
	    }else if(c==']'){
	    	depth = 0;
	    }else if (c == '\\') {
	        start++;
	    }else if(depth == 0 && c == '/'){
	    	while(c = text.charAt(start++)){
	    		switch(c){
	    			case 'g':
	    			case 'i':
	    			case 'm':
	    			break;
	    			default:
	    			//console.error(text.substring(0,start-1)+'@',text)
	    			return text.substring(0,start-1);
	    		}
	    	}
	    	
	    }
	}
}
if(typeof require == 'function'){
exports.partitionJavaScript=partitionJavaScript;
exports.compressJS=compressJS;
}