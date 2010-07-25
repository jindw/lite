function parseXML(xml,begin,end,text,cdata,doctype,ins,comm,error) {
	var start = 0;
	var len = xml.length;
	while(start<len){
		var i = start;
		var c = xml.charAt(i++)
		if(c === '<'){
			c = xml.charAt(i++)
			if(c === '!'){
				c = xml.charAt(i++);
				if(c == '-'){//ignore comment start check
					var end = xml.indexOf('-->',i)+3;
					if(end>3){
						comm && comm(xml,start,start=end);
					}else{
						error("找不到注释结束符",getPosition(xml,start))
						text(xml,start,start+=2);
					}
				}else if(c == '['){//ignore cdata start check
					end = xml.indexOf(']]>',i)+3;
					if(end>3){
						cdata(xml,start,start=end);
					}else{
						error("找不到CDATA结束符",getPosition(xml,start))
						text(xml,start,start+=2);
					}
				}else{
					end = parseDoctype(doctype,xml,start);
					if(end<0){
						error("找不到DOCTYPE结束符",getPosition(xml,start))
						text(xml,start,start+=2);
					}
				}
			}else if(c === '?'){
				end = xml.indexOf('?>',i)+2;
				if(end>2){
					ins(xml,start,start=end);
				}else{
					error("找不到XML 处理器结束符",getPosition(xml,start))
					text(xml,start,start+=2);
				}
			}else if(c === '/'){
				end = xml.indexOf('>',i)+1;
				if(end>1){
					end(xml,start,end,xml.substring(start+2,end-1));
					start=end;
				}else{
					error("找不到变迁结束符",getPosition(xml,start))
					text(xml,start,start+=2);
				}
			}else{
				start = parseElement(begin,xml,i-2);
				continue;
			}
		}else{
			end = xml.indexOf('<',i);
			if(end<0){
				end = len;
			}
			text(xml,start,start = end)
		}
	}
}
function parseElement(begin,text,start,error){
	var len = text.length;
	var i = start+1;
	var buf = [];
	var ps = [];
	var tokenStart = 0;
	while(i<len){
		var c = text.charAt(i++);
		switch(c){
		case '"':
		case '\'':
			var j = text.indexOf(c,i);
			if(j>0){
				buf.push(text.substring(i-1,i=j+1));
				//tokenStart = 0;
				break;
			}else{
				error("元素解析异常",getPositon(text,start));
				return text.length;
			}
		case '<':
			i--;
		case '>':
			if(tokenStart){
				buf.push(text.substring(tokenStart,i));
			}
			var j = 1;
			var end = buf.length;
			var attrMap = {};
			while(j<end){
				var c = buf[j++];
				var attrMap = {};
				if(c == '='){
					var value = buf[j++];
					//TODO:check...
					attrMap[key] = value;
				}else{
					if(key){
						attrMap[key] = null;
					}
					key = c;
				}
				//error("非法属性",getPositon(text,start));
			}
			begin(text,start,i,buf[0],attrMap)
			//attrMap[attrName] = attrName;//html no-value attr
			return i;
		case '\r':
		case '\n':
		case '\t':
		case ' ':
		case '=':
			if(tokenStart){
				buf.push(text.substring(tokenStart,i-1));
				tokenStart = 0;
			}
			if(c == '='){
				buf.push(c);
			}
			break;
		default:
			if(!tokenStart){
				tokenStart = i;
			}
		}
	}
	error("非法属性,中止xml解析",getPositon(text,start));
	return text.length;
}
function parseDoctype(doctype,text,start,error){
	var exp = /<!DOCTYPE\s+(\w+)(?:\s+PUBLIC\s+("[^"]+"|'[^']+'))(?:\s+("[^"]+"|'[^']+'))\s*>/i
	var match = text.match(exp);
	if(match){
		var a = match[0];
		var end = start + a.length;
		if(a == text.substring(start,end)){
			doctype(text,start,end,match[1],match[2],match[3],null);
			return end;
		}
	}
	var len = text.length;
	var dep = 0;
	var i = start;
	while(i<len){
		var c = text.charAt(i++);
		switch(c){
		case '<':
			dep++;break;
		case '>':
			dep--;
			if(dep == 0){
				var content = text.substring(start,i);
				var n = content.match(/<!DOCTYPE\s+(\w+)/);
				doctype(text,start,end,n && n[1],null,null,content);
				return i;
			}
			break;
		case '"':
		case '\'':
			i = text.indexOf(c,i);
			if(i>0){
				start = i+1;
			}else{
				error("DTD 申明异常",getPositon(text,start));
				return text.length;
			}
		}
	}
	return text.length;
}
function getPosition(text,end){
	var line = 0;
	var col;
	while(end--){
		if(line === 0){
			col++;
		}
		var c = text.charAt(end);
		if(c === '\n'){
			if(text.charAt(end-1) === '\r'){
				end--;
			}
			line++;
		}else if(c === '\n'){
			line++;
		}
	}
	return [line,col]
}